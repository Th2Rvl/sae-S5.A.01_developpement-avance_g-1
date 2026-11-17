package com.tgv.tgv_workfloor.optimisation;

import com.tgv.tgv_workfloor.modele.Algorithme;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Résolution exacte « maison » : programmation dynamique de Held-Karp, restreinte aux états
 * compatibles avec l'ordre des masses.
 * <p>
 * cout[E][j] = longueur minimale d'un chemin qui part du dépôt, livre exactement l'ensemble E de
 * clients (masque binaire) et se termine au client j ∈ E. On n'ajoute un client k à E que si tous les
 * clients aux colis plus légers que les siens sont déjà dans E. Seuls les ensembles atteignables sont
 * créés (tables de hachage, couche par couche) : avec les masses, ce sont les groupes de clients aux
 * masses identiques qui font la taille du problème, pas le nombre total de clients.
 * <p>
 * Complexité : O(n²·E) en temps et O(n·E) en mémoire, E = nombre d'ensembles atteignables
 * (2ⁿ sans contrainte, Σ 2^kᵢ environ avec des groupes de kᵢ clients interchangeables).
 */
@Component
public class HeldKarpSolveur implements SolveurBoucle {

    /** Plafond de E × n (≈ 5·10⁶ cases, quelques dizaines de Mo). */
    public static final long LIMITE_CASES = 5_000_000L;
    /** Les ensembles sont codés sur un long. */
    public static final int TAILLE_MAX = 63;

    private static final class Etat {
        final double[] cout;
        final byte[] precedent;

        Etat(int n) {
            cout = new double[n];
            precedent = new byte[n];
            Arrays.fill(cout, Double.POSITIVE_INFINITY);
        }
    }

    @Override
    public Algorithme algorithme() {
        return Algorithme.EXACT_MAISON;
    }

    @Override
    public ResultatBoucle resoudre(ProblemeBoucle probleme) {
        int n = probleme.nbClients();
        if (n == 0) {
            return new ResultatBoucle(new int[0], 0, true);
        }
        if (n > TAILLE_MAX || probleme.espaceRecherche(LIMITE_CASES / n).depasse()) {
            throw new BoucleImpossibleException("Trop de clients aux masses identiques pour l'algorithme exact "
                    + "maison : utilisez la bibliothèque ou l'algorithme approché.");
        }
        long[] predecesseurs = probleme.masquesPredecesseurs();

        List<Map<Long, Etat>> couches = new ArrayList<>(n);
        Map<Long, Etat> couche = new HashMap<>();
        for (int j = 0; j < n; j++) {
            if (predecesseurs[j] == 0) {
                Etat e = couche.computeIfAbsent(1L << j, m -> new Etat(n));
                e.cout[j] = probleme.depuisDepot(j);
                e.precedent[j] = -1;
            }
        }
        couches.add(couche);

        for (int taille = 1; taille < n; taille++) {
            Map<Long, Etat> suivante = new HashMap<>();
            for (Map.Entry<Long, Etat> entree : couche.entrySet()) {
                long ensemble = entree.getKey();
                Etat etat = entree.getValue();
                for (int j = 0; j < n; j++) {
                    double c = etat.cout[j];
                    if (c == Double.POSITIVE_INFINITY) {
                        continue;
                    }
                    for (int k = 0; k < n; k++) {
                        long bit = 1L << k;
                        if ((ensemble & bit) != 0 || (predecesseurs[k] & ~ensemble) != 0) {
                            continue;
                        }
                        Etat cible = suivante.computeIfAbsent(ensemble | bit, m -> new Etat(n));
                        double candidat = c + probleme.distanceClients(j, k);
                        if (candidat < cible.cout[k]) {
                            cible.cout[k] = candidat;
                            cible.precedent[k] = (byte) j;
                        }
                    }
                }
            }
            if (suivante.isEmpty()) {
                break;
            }
            couches.add(suivante);
            couche = suivante;
        }

        long complet = (1L << n) - 1;
        Etat fin = couches.size() == n ? couches.get(n - 1).get(complet) : null;
        if (fin == null) {
            throw new BoucleImpossibleException("Aucune boucle ne respecte l'ordre imposé par les masses des colis.");
        }
        double meilleur = Double.POSITIVE_INFINITY;
        int dernier = -1;
        for (int j = 0; j < n; j++) {
            double total = fin.cout[j] + probleme.versDepot(j);
            if (total < meilleur) {
                meilleur = total;
                dernier = j;
            }
        }

        int[] ordre = new int[n];
        long ensemble = complet;
        int j = dernier;
        for (int position = n - 1; position >= 0; position--) {
            ordre[position] = j;
            int avant = couches.get(position).get(ensemble).precedent[j];
            ensemble &= ~(1L << j);
            j = avant;
        }
        return new ResultatBoucle(ordre, meilleur, true);
    }

    @Override
    public EstimationDifficulte estimer(ProblemeBoucle probleme) {
        int n = probleme.nbClients();
        String complexite = "O(n²·E), E = ensembles de clients livrables (≤ 2ⁿ)";
        if (n == 0) {
            return new EstimationDifficulte(algorithme(), Difficulte.FAIBLE, 0, complexite);
        }
        ProblemeBoucle.EspaceRecherche espace = probleme.espaceRecherche(LIMITE_CASES / n);
        if (n > TAILLE_MAX || espace.depasse()) {
            return new EstimationDifficulte(algorithme(), Difficulte.HORS_DE_PORTEE,
                    (double) n * n * Math.pow(2, n), complexite);
        }
        if (espace.ordres() == 0) {
            return new EstimationDifficulte(algorithme(), Difficulte.IMPOSSIBLE, 0, complexite);
        }
        double operations = (double) n * n * espace.etats();
        return new EstimationDifficulte(algorithme(), Difficulte.selonOperations(operations), operations, complexite);
    }
}
