package com.tgv.tgv_workfloor.optimisation;

import com.tgv.tgv_workfloor.modele.Algorithme;
import org.springframework.stereotype.Component;

/**
 * Résolution approchée « maison » en deux temps :
 * <ol>
 *   <li>construction gloutonne du plus proche voisin, limitée aux clients « disponibles »
 *       (ceux dont tous les prédécesseurs imposés par les masses sont déjà livrés) ;</li>
 *   <li>amélioration locale 2-opt : on inverse un segment de la boucle tant que cela la raccourcit
 *       et que l'inversion ne viole aucune précédence.</li>
 * </ol>
 * Complexité : O(n²) pour la construction, O(n²) candidats par passe de 2-opt
 * (chaque inversion acceptée raccourcit strictement la boucle, donc le nombre de passes est fini).
 */
@Component
public class PlusProcheVoisinSolveur implements SolveurBoucle {

    private static final double EPSILON = 1e-9;

    @Override
    public Algorithme algorithme() {
        return Algorithme.APPROCHE_MAISON;
    }

    @Override
    public ResultatBoucle resoudre(ProblemeBoucle probleme) {
        int n = probleme.nbClients();
        if (n == 0) {
            return new ResultatBoucle(new int[0], 0, true);
        }
        int[] ordre = plusProcheVoisin(probleme);
        deuxOpt(probleme, ordre);
        return new ResultatBoucle(ordre, probleme.longueur(ordre), n <= 2);
    }

    int[] plusProcheVoisin(ProblemeBoucle probleme) {
        int n = probleme.nbClients();
        int[] predecesseursRestants = new int[n];
        for (int a = 0; a < n; a++) {
            for (int b = 0; b < n; b++) {
                if (probleme.doitPreceder(a, b)) {
                    predecesseursRestants[b]++;
                }
            }
        }
        boolean[] livre = new boolean[n];
        int[] ordre = new int[n];
        int courant = -1;
        for (int etape = 0; etape < n; etape++) {
            int choisi = -1;
            double plusCourte = Double.POSITIVE_INFINITY;
            for (int k = 0; k < n; k++) {
                if (livre[k] || predecesseursRestants[k] > 0) {
                    continue;
                }
                double d = courant < 0 ? probleme.depuisDepot(k) : probleme.distanceClients(courant, k);
                if (d < plusCourte) {
                    plusCourte = d;
                    choisi = k;
                }
            }
            if (choisi < 0) {
                throw new BoucleImpossibleException("Aucune boucle ne respecte l'ordre imposé par les masses des colis.");
            }
            livre[choisi] = true;
            ordre[etape] = choisi;
            for (int j = 0; j < n; j++) {
                if (probleme.doitPreceder(choisi, j)) {
                    predecesseursRestants[j]--;
                }
            }
            courant = choisi;
        }
        return ordre;
    }

    void deuxOpt(ProblemeBoucle probleme, int[] ordre) {
        int n = ordre.length;
        double longueur = probleme.longueur(ordre);
        boolean ameliore = true;
        while (ameliore) {
            ameliore = false;
            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (!inversionPermise(probleme, ordre, i, j)) {
                        continue;
                    }
                    inverser(ordre, i, j);
                    double nouvelle = probleme.longueur(ordre);
                    if (nouvelle < longueur - EPSILON) {
                        longueur = nouvelle;
                        ameliore = true;
                    } else {
                        inverser(ordre, i, j);
                    }
                }
            }
        }
    }

    /** Inverser ordre[i..j] place b avant a pour tout couple (a avant b) du segment. */
    private static boolean inversionPermise(ProblemeBoucle probleme, int[] ordre, int i, int j) {
        for (int a = i; a < j; a++) {
            for (int b = a + 1; b <= j; b++) {
                if (probleme.doitPreceder(ordre[a], ordre[b])) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void inverser(int[] ordre, int i, int j) {
        for (int g = i, d = j; g < d; g++, d--) {
            int tmp = ordre[g];
            ordre[g] = ordre[d];
            ordre[d] = tmp;
        }
    }

    @Override
    public EstimationDifficulte estimer(ProblemeBoucle probleme) {
        int n = probleme.nbClients();
        String complexite = "O(n²) + O(n²) par passe de 2-opt";
        ProblemeBoucle.EspaceRecherche espace = probleme.espaceRecherche(1_000_000L);
        if (!espace.depasse() && espace.ordres() == 0) {
            return new EstimationDifficulte(algorithme(), Difficulte.IMPOSSIBLE, 0, complexite);
        }
        double operations = Math.pow(n, 3);
        return new EstimationDifficulte(algorithme(), Difficulte.selonOperations(operations), operations, complexite);
    }
}
