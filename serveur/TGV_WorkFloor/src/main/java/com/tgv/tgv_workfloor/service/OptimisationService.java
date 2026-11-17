package com.tgv.tgv_workfloor.service;

import com.tgv.tgv_workfloor.exception.RegleMetierException;
import com.tgv.tgv_workfloor.modele.Algorithme;
import com.tgv.tgv_workfloor.modele.Arret;
import com.tgv.tgv_workfloor.modele.Client;
import com.tgv.tgv_workfloor.modele.Colis;
import com.tgv.tgv_workfloor.modele.Tournee;
import com.tgv.tgv_workfloor.optimisation.BoucleImpossibleException;
import com.tgv.tgv_workfloor.optimisation.CalculateurDistances;
import com.tgv.tgv_workfloor.optimisation.ContraintesMasses;
import com.tgv.tgv_workfloor.optimisation.EstimationDifficulte;
import com.tgv.tgv_workfloor.optimisation.PlanChargement;
import com.tgv.tgv_workfloor.optimisation.ProblemeBoucle;
import com.tgv.tgv_workfloor.optimisation.ResultatBoucle;
import com.tgv.tgv_workfloor.optimisation.SolveurBoucle;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Fait le lien entre une tournée (modèle métier) et les algorithmes d'optimisation.
 * <p>
 * Règle toujours appliquée : les colis sont chargés par masses décroissantes et déchargés par le dessus,
 * donc un client aux colis plus légers est livré avant un client aux colis plus lourds. Le seul choix
 * à optimiser est l'ordre des clients dont les colis ont des masses identiques.
 */
@Service
public class OptimisationService {

    private final Map<Algorithme, SolveurBoucle> solveurs = new EnumMap<>(Algorithme.class);
    private final CalculateurDistances distances;

    public OptimisationService(List<SolveurBoucle> solveurs, CalculateurDistances distances) {
        solveurs.forEach(s -> this.solveurs.put(s.algorithme(), s));
        this.distances = distances;
    }

    public List<EstimationDifficulte> estimer(Tournee tournee) {
        ProblemeBoucle probleme = probleme(tournee, tournee.getClients());
        return solveurs.values().stream().map(s -> s.estimer(probleme)).toList();
    }

    /** Nombre d'ordres de livraison compatibles avec les masses (infini si non dénombré). */
    public double nombreOrdresPossibles(Tournee tournee) {
        return probleme(tournee, tournee.getClients()).espaceRecherche(2_000_000L).ordres();
    }

    /** Clients dont les masses de colis s'entrecroisent (aucun chargement par masses possible). */
    public List<String> conflitsDeMasses(Tournee tournee) {
        List<Client> clients = tournee.getClients();
        boolean[][] precedences = ContraintesMasses.precedences(masses(tournee, clients));
        List<String> messages = new ArrayList<>();
        for (int[] conflit : ContraintesMasses.conflits(precedences)) {
            Client a = clients.get(conflit[0]);
            Client b = clients.get(conflit[1]);
            messages.add(a.getNom() + " (" + intervalle(tournee, a) + ") et " + b.getNom() + " ("
                    + intervalle(tournee, b) + ")");
        }
        return messages;
    }

    /** Calcule et enregistre dans la tournée la boucle et le plan de chargement. */
    public void calculer(Tournee tournee, Algorithme algorithme) {
        SolveurBoucle solveur = solveurs.get(algorithme);
        if (solveur == null) {
            throw new RegleMetierException("Algorithme non disponible : " + algorithme);
        }
        List<Client> clients = tournee.getClients();
        if (clients.isEmpty()) {
            throw new RegleMetierException("La tournée ne contient aucun colis.");
        }
        verifierAbsenceDeConflit(tournee);
        ProblemeBoucle probleme = probleme(tournee, clients);
        long debut = System.nanoTime();
        ResultatBoucle resultat = solveur.resoudre(probleme);
        long dureeMs = (System.nanoTime() - debut) / 1_000_000;

        appliquer(tournee, clients, resultat.ordre(), probleme);
        tournee.setAlgorithme(algorithme);
        tournee.setOptimaliteProuvee(resultat.optimaliteProuvee());
        tournee.setDureeCalculMs(dureeMs);
    }

    /** Enregistre l'ordre choisi par le chauffeur, s'il respecte l'ordre des masses. */
    public void appliquerOrdreManuel(Tournee tournee, List<Long> clientIds) {
        List<Client> clients = tournee.getClients();
        Set<Long> attendus = new HashSet<>(clients.stream().map(Client::getId).toList());
        if (clientIds == null || clientIds.size() != attendus.size() || !attendus.equals(new HashSet<>(clientIds))) {
            throw new RegleMetierException("L'ordre manuel doit contenir chaque client de la tournée exactement une fois.");
        }
        verifierAbsenceDeConflit(tournee);
        int[] ordre = new int[clientIds.size()];
        for (int i = 0; i < ordre.length; i++) {
            Long id = clientIds.get(i);
            ordre[i] = clients.indexOf(clients.stream().filter(c -> c.getId().equals(id)).findFirst().orElseThrow());
        }
        ProblemeBoucle probleme = probleme(tournee, clients);
        for (int i = 0; i < ordre.length; i++) {
            for (int j = i + 1; j < ordre.length; j++) {
                if (probleme.doitPreceder(ordre[j], ordre[i])) {
                    Client avant = clients.get(ordre[j]);
                    Client apres = clients.get(ordre[i]);
                    throw new RegleMetierException("Ordre impossible : " + avant.getNom() + " (" + intervalle(tournee, avant)
                            + ") doit être livré avant " + apres.getNom() + " (" + intervalle(tournee, apres)
                            + "), car ses colis plus légers sont chargés au-dessus. Seuls les clients aux colis de "
                            + "masses identiques peuvent être permutés.");
                }
            }
        }
        appliquer(tournee, clients, ordre, probleme);
        tournee.setAlgorithme(Algorithme.MANUEL);
        tournee.setOptimaliteProuvee(false);
        tournee.setDureeCalculMs(0);
    }

    private void appliquer(Tournee tournee, List<Client> clients, int[] ordre, ProblemeBoucle probleme) {
        tournee.effacerBoucle();
        double latPrec = tournee.getAtelier().getLatitude();
        double lonPrec = tournee.getAtelier().getLongitude();
        List<List<Colis>> colisParArret = new ArrayList<>();
        int groupe = 0;
        for (int rang = 0; rang < ordre.length; rang++) {
            Client client = clients.get(ordre[rang]);
            // nouveau groupe dès que le client doit être livré après le précédent (masses différentes)
            if (rang == 0 || probleme.doitPreceder(ordre[rang - 1], ordre[rang])) {
                groupe++;
            }
            double d = distances.distanceKm(latPrec, lonPrec, client.getLatitude(), client.getLongitude());
            tournee.getArrets().add(new Arret(rang + 1, client, d, groupe));
            colisParArret.add(tournee.colisDuClient(client));
            latPrec = client.getLatitude();
            lonPrec = client.getLongitude();
        }
        tournee.setDistanceRetourKm(distances.distanceKm(latPrec, lonPrec,
                tournee.getAtelier().getLatitude(), tournee.getAtelier().getLongitude()));
        tournee.getChargement().addAll(PlanChargement.calculer(colisParArret));
        if (!PlanChargement.respecteOrdreDesMasses(tournee.getChargement())) {
            throw new IllegalStateException("Plan de chargement incohérent avec l'ordre des masses");
        }
        tournee.setDateCalcul(LocalDateTime.now());
    }

    private void verifierAbsenceDeConflit(Tournee tournee) {
        List<String> conflits = conflitsDeMasses(tournee);
        if (!conflits.isEmpty()) {
            throw new BoucleImpossibleException("Chargement impossible par masses décroissantes : les colis de "
                    + String.join(", de ", conflits) + " s'entrecroisent. Chaque client étant livré en une seule fois, "
                    + "revoyez l'emballage de ces commandes.");
        }
    }

    ProblemeBoucle probleme(Tournee tournee, List<Client> clients) {
        List<double[]> points = new ArrayList<>();
        points.add(new double[]{tournee.getAtelier().getLatitude(), tournee.getAtelier().getLongitude()});
        clients.forEach(c -> points.add(new double[]{c.getLatitude(), c.getLongitude()}));
        return new ProblemeBoucle(distances.matrice(points), ContraintesMasses.precedences(masses(tournee, clients)));
    }

    private static List<double[]> masses(Tournee tournee, List<Client> clients) {
        return clients.stream()
                .map(c -> tournee.colisDuClient(c).stream().mapToDouble(Colis::getPoidsKg).toArray())
                .toList();
    }

    private static String intervalle(Tournee tournee, Client client) {
        double[] m = tournee.colisDuClient(client).stream().mapToDouble(Colis::getPoidsKg).sorted().toArray();
        return m[0] == m[m.length - 1] ? String.format(Locale.FRANCE, "%.0f kg", m[0])
                : String.format(Locale.FRANCE, "%.0f à %.0f kg", m[0], m[m.length - 1]);
    }
}
