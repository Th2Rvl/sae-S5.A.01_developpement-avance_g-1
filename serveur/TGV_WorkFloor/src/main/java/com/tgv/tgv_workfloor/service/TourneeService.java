package com.tgv.tgv_workfloor.service;

import com.tgv.tgv_workfloor.depot.AtelierDepot;
import com.tgv.tgv_workfloor.depot.BonLivraisonDepot;
import com.tgv.tgv_workfloor.depot.ColisDepot;
import com.tgv.tgv_workfloor.depot.TourneeDepot;
import com.tgv.tgv_workfloor.exception.RegleMetierException;
import com.tgv.tgv_workfloor.exception.RessourceIntrouvableException;
import com.tgv.tgv_workfloor.modele.Algorithme;
import com.tgv.tgv_workfloor.modele.Arret;
import com.tgv.tgv_workfloor.modele.Atelier;
import com.tgv.tgv_workfloor.modele.BonLivraison;
import com.tgv.tgv_workfloor.modele.Client;
import com.tgv.tgv_workfloor.modele.Colis;
import com.tgv.tgv_workfloor.modele.EtatBon;
import com.tgv.tgv_workfloor.modele.EtatPiece;
import com.tgv.tgv_workfloor.modele.StatutColis;
import com.tgv.tgv_workfloor.modele.StatutTournee;
import com.tgv.tgv_workfloor.modele.Tournee;
import com.tgv.tgv_workfloor.modele.Utilisateur;
import com.tgv.tgv_workfloor.optimisation.EstimationDifficulte;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/** Cycle de vie d'une tournée : préparation, calcul de la boucle, validation, livraisons, fin. */
@Service
public class TourneeService {

    private static final DateTimeFormatter FORMAT_BON = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TourneeDepot tournees;
    private final ColisDepot colis;
    private final AtelierDepot ateliers;
    private final BonLivraisonDepot bons;
    private final OptimisationService optimisation;

    public TourneeService(TourneeDepot tournees, ColisDepot colis, AtelierDepot ateliers, BonLivraisonDepot bons,
                          OptimisationService optimisation) {
        this.tournees = tournees;
        this.colis = colis;
        this.ateliers = ateliers;
        this.bons = bons;
        this.optimisation = optimisation;
    }

    public List<Tournee> lister() {
        return tournees.findAllByOrderByDateDesc();
    }

    public Tournee trouver(Long id) {
        return tournees.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Tournée", id));
    }

    /** Tournée que le chauffeur doit effectuer : la plus ancienne qui n'est pas terminée. */
    public Tournee courante() {
        return tournees.findAll().stream()
                .filter(t -> t.getStatut() != StatutTournee.TERMINEE)
                .min(Comparator.comparing(Tournee::getDate))
                .orElseThrow(() -> new RessourceIntrouvableException("Tournée à effectuer", "aucune"));
    }

    /**
     * Colis prêts (à livrer, pas encore affectés) qui peuvent partir dans la tournée de ce jour-là selon la règle
     * de planification : colis neufs dès le lendemain ouvré de leur emballage, colis reportés le jeudi.
     */
    public List<Colis> colisDisponibles(LocalDate date) {
        return colisEnAttente().stream()
                .filter(c -> PlanificationLivraisons.peutPartirLe(c, date))
                .toList();
    }

    /** Colis à livrer qui ne sont dans aucune tournée, du plus urgent au moins urgent. */
    public List<Colis> colisEnAttente() {
        return colis.findByStatut(StatutColis.A_LIVRER).stream()
                .filter(c -> c.getTournee() == null)
                .sorted(Comparator.comparing(PlanificationLivraisons::dateDue).thenComparing(Colis::getReference))
                .toList();
    }

    /** Crée la tournée du jour (ou la complète si elle est encore prévue) avec les colis disponibles. */
    public synchronized Tournee preparer(LocalDate date) {
        if (!PlanificationLivraisons.estOuvre(date)) {
            throw new RegleMetierException("Pas de tournée le week-end : choisissez un jour du lundi au vendredi.");
        }
        Tournee tournee = tournees.findByDate(date).orElse(null);
        if (tournee != null && tournee.getStatut().estFigee()) {
            throw new RegleMetierException("La tournée du " + date + " est déjà "
                    + tournee.getStatut().getLibelle().toLowerCase() + " : ses colis ne peuvent plus changer.");
        }
        List<Colis> disponibles = colisDisponibles(date);
        if (tournee == null && disponibles.isEmpty()) {
            throw new RegleMetierException("Aucun colis ne doit partir le " + date + ".");
        }
        if (tournee == null) {
            tournee = tournees.save(new Tournee(date, atelierPrincipal()));
        }
        if (!disponibles.isEmpty()) {
            for (Colis c : disponibles) {
                c.setTournee(tournee);
                tournee.getColis().add(c);
            }
            tournee.effacerBoucle();
        }
        return tournee;
    }

    /**
     * Place automatiquement un colis à livrer dans la tournée de sa date de livraison (créée si besoin). Si cette
     * tournée est déjà validée ou partie, le colis prend la suivante possible (lendemain ouvré, ou jeudi suivant
     * pour un colis reporté).
     */
    public synchronized Tournee planifier(Colis c) {
        if (c.getStatut() != StatutColis.A_LIVRER || c.getTournee() != null) {
            return c.getTournee();
        }
        LocalDate date = PlanificationLivraisons.premiereDate(c, LocalDate.now());
        Tournee tournee = tournees.findByDate(date).orElse(null);
        while (tournee != null && tournee.getStatut().estFigee()) {
            date = PlanificationLivraisons.dateSuivante(c, date);
            tournee = tournees.findByDate(date).orElse(null);
        }
        if (tournee == null) {
            tournee = tournees.save(new Tournee(date, atelierPrincipal()));
        }
        c.setDateLivraisonPrevue(date);
        c.setTournee(tournee);
        tournee.getColis().add(c);
        tournee.effacerBoucle();
        return tournee;
    }

    /** Sort un colis de sa tournée prévue (colis défait) ; une tournée prévue devenue vide est supprimée. */
    public synchronized void retirerDuPlanning(Colis c) {
        Tournee tournee = c.getTournee();
        if (tournee == null) {
            return;
        }
        tournee.getColis().remove(c);
        c.setTournee(null);
        tournee.effacerBoucle();
        if (tournee.getColis().isEmpty() && tournee.getStatut() == StatutTournee.PREVUE) {
            tournees.delete(tournee);
        }
    }

    public synchronized Tournee retirerColis(Long tourneeId, Long colisId) {
        Tournee tournee = trouver(tourneeId);
        verifierModifiable(tournee);
        Colis c = tournee.getColis().stream().filter(x -> x.getId().equals(colisId)).findFirst()
                .orElseThrow(() -> new RessourceIntrouvableException("Colis de la tournée", colisId));
        tournee.getColis().remove(c);
        c.setTournee(null);
        tournee.effacerBoucle();
        return tournee;
    }

    public synchronized void supprimer(Long id) {
        Tournee tournee = trouver(id);
        if (tournee.getStatut() != StatutTournee.PREVUE) {
            throw new RegleMetierException("Seule une tournée prévue (non validée) peut être supprimée.");
        }
        tournee.getColis().forEach(c -> c.setTournee(null));
        tournees.delete(tournee);
    }

    public List<EstimationDifficulte> estimer(Long id) {
        return optimisation.estimer(trouver(id));
    }

    public double nombreOrdresPossibles(Long id) {
        return optimisation.nombreOrdresPossibles(trouver(id));
    }

    public List<String> conflitsDeMasses(Long id) {
        return optimisation.conflitsDeMasses(trouver(id));
    }

    public synchronized Tournee calculer(Long id, Algorithme algorithme) {
        Tournee tournee = trouver(id);
        verifierRecalculable(tournee);
        if (algorithme == null || !algorithme.estCalcule()) {
            throw new RegleMetierException("Choisissez un algorithme de calcul.");
        }
        optimisation.calculer(tournee, algorithme);
        tournee.setStatut(StatutTournee.PREVUE);
        return tournee;
    }

    public synchronized Tournee ordreManuel(Long id, List<Long> clientIds) {
        Tournee tournee = trouver(id);
        verifierRecalculable(tournee);
        optimisation.appliquerOrdreManuel(tournee, clientIds);
        tournee.setStatut(StatutTournee.PREVUE);
        return tournee;
    }

    /** Le chauffeur valide la boucle et le chargement : les colis de la tournée sont figés. */
    public synchronized Tournee valider(Long id, Utilisateur chauffeur) {
        Tournee tournee = trouver(id);
        if (tournee.getStatut() != StatutTournee.PREVUE) {
            throw new RegleMetierException("La tournée est déjà " + tournee.getStatut().getLibelle().toLowerCase() + ".");
        }
        if (!tournee.isBoucleCalculee()) {
            throw new RegleMetierException("Calculez ou saisissez la boucle avant de la valider.");
        }
        tournee.setChauffeur(chauffeur);
        tournee.setStatut(StatutTournee.VALIDEE);
        return tournee;
    }

    /**
     * Établit le bon de livraison d'un arrêt. Les colis listés sont remis (livrés), sauf si le client
     * refuse la livraison ; les autres colis du client sont marqués refusés.
     */
    public synchronized BonLivraison enregistrerBon(Long tourneeId, Long clientId, List<Long> colisRemis, EtatBon etat,
                                                   String commentaire, Utilisateur chauffeur) {
        Tournee tournee = trouver(tourneeId);
        if (tournee.getStatut() != StatutTournee.VALIDEE && tournee.getStatut() != StatutTournee.EN_COURS) {
            throw new RegleMetierException("La tournée doit être validée pour établir des bons de livraison.");
        }
        if (etat == null) {
            throw new RegleMetierException("Indiquez l'état de la livraison.");
        }
        Arret arret = tournee.getArrets().stream().filter(a -> a.getClient().getId().equals(clientId)).findFirst()
                .orElseThrow(() -> new RessourceIntrouvableException("Arrêt de la tournée pour le client", clientId));
        Client client = arret.getClient();
        if (tournee.bonDuClient(client).isPresent()) {
            throw new RegleMetierException("Le bon de livraison de " + client.getNom() + " a déjà été établi.");
        }
        if (etat == EtatBon.RESERVES && (commentaire == null || commentaire.isBlank())) {
            throw new RegleMetierException("Décrivez les réserves émises par le client.");
        }
        List<Colis> colisClient = tournee.colisDuClient(client);
        List<Long> remis = colisRemis == null || colisRemis.isEmpty()
                ? colisClient.stream().map(Colis::getId).toList() : colisRemis;
        for (Long id : remis) {
            if (colisClient.stream().noneMatch(c -> c.getId().equals(id))) {
                throw new RegleMetierException("Le colis " + id + " n'est pas destiné à " + client.getNom() + ".");
            }
        }

        BonLivraison bon = new BonLivraison();
        bon.setNumero("BL-" + tournee.getDate().format(FORMAT_BON) + "-" + String.format("%02d", arret.getOrdre()));
        bon.setTournee(tournee);
        bon.setClient(client);
        bon.setChauffeur(chauffeur);
        bon.setDateHeure(LocalDateTime.now());
        bon.setEtat(etat);
        bon.setCommentaire(commentaire == null || commentaire.isBlank() ? null : commentaire.trim());
        for (Colis c : colisClient) {
            boolean livre = etat != EtatBon.REFUSE && remis.contains(c.getId());
            c.setStatut(livre ? StatutColis.LIVRE : StatutColis.REFUSE);
            c.setBon(bon);
            bon.getColis().add(c);
            if (livre) {
                c.getPieces().forEach(p -> p.setEtat(EtatPiece.LIVREE));
            }
        }
        bons.save(bon);
        tournee.getBons().add(bon);
        tournee.setStatut(tournee.getBons().size() == tournee.getArrets().size()
                ? StatutTournee.TERMINEE : StatutTournee.EN_COURS);
        return bon;
    }

    /**
     * Clôt la tournée. Les colis sans bon n'ont pas été livrés : ils sont reportés au jeudi suivant et placés
     * dans la tournée de ce jeudi.
     */
    public synchronized Tournee terminer(Long id) {
        Tournee tournee = trouver(id);
        if (tournee.getStatut() != StatutTournee.VALIDEE && tournee.getStatut() != StatutTournee.EN_COURS) {
            throw new RegleMetierException("Seule une tournée validée ou en cours peut être terminée.");
        }
        tournee.setStatut(StatutTournee.TERMINEE);
        LocalDate aujourdhui = LocalDate.now();
        LocalDate manquee = tournee.getDate().isAfter(aujourdhui) ? tournee.getDate() : aujourdhui;
        for (Colis c : List.copyOf(tournee.getColis())) {
            if (c.getBon() == null) {
                // le colis reste dans l'historique de cette tournée mais part dans celle du jeudi suivant
                c.setStatut(StatutColis.A_LIVRER);
                c.setTournee(null);
                c.setReporte(true);
                c.setDateLivraisonPrevue(PlanificationLivraisons.livraisonColisReporte(manquee));
                planifier(c);
            }
        }
        return tournee;
    }


    public List<BonLivraison> listerBons() {
        return bons.findAllByOrderByDateHeureDesc();
    }

    private Atelier atelierPrincipal() {
        return ateliers.findAll().stream().findFirst()
                .orElseThrow(() -> new RegleMetierException("Aucun atelier n'est configuré."));
    }

    private static void verifierModifiable(Tournee tournee) {
        if (tournee.getStatut().estFigee()) {
            throw new RegleMetierException("La tournée est " + tournee.getStatut().getLibelle().toLowerCase()
                    + " : ses colis ne peuvent plus changer.");
        }
    }

    private static void verifierRecalculable(Tournee tournee) {
        if (tournee.getStatut() == StatutTournee.EN_COURS || tournee.getStatut() == StatutTournee.TERMINEE) {
            throw new RegleMetierException("La tournée est " + tournee.getStatut().getLibelle().toLowerCase()
                    + " : la boucle ne peut plus être modifiée.");
        }
    }
}
