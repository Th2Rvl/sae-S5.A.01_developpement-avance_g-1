package com.tgv.tgv_workfloor.optimisation;

import com.tgv.tgv_workfloor.modele.Algorithme;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.variables.IntVar;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Résolution exacte reposant sur une bibliothèque maintenue : le solveur de contraintes Choco-solver.
 * <p>
 * Modèle : position[k] = client livré en k-ième ; rang[c] = position du client c (canal inverse,
 * qui impose aussi que tous les clients soient différents). Le coût de chaque tronçon est lié aux
 * positions par une table (tronçons entre clients) ou un « element » (départ et retour au dépôt).
 * Les précédences dues aux masses deviennent rang[a] &lt; rang[b]. Choco minimise la somme par
 * séparation-évaluation ; si la limite de temps est atteinte, on garde la meilleure boucle trouvée
 * sans preuve d'optimalité. Les distances sont manipulées en mètres (entiers).
 */
@Component
public class ChocoSolveur implements SolveurBoucle {

    public static final int TAILLE_MAX = 100;
    private static final long PLAFOND_DENOMBREMENT = 2_000_000L;

    private final long limiteMs;

    public ChocoSolveur(@Value("${tgv.optimisation.choco.limite-ms:20000}") long limiteMs) {
        this.limiteMs = limiteMs;
    }

    @Override
    public Algorithme algorithme() {
        return Algorithme.EXACT_BIBLIOTHEQUE;
    }

    @Override
    public ResultatBoucle resoudre(ProblemeBoucle probleme) {
        int n = probleme.nbClients();
        if (n == 0) {
            return new ResultatBoucle(new int[0], 0, true);
        }
        if (n > TAILLE_MAX) {
            throw new BoucleImpossibleException("La résolution par bibliothèque est limitée à " + TAILLE_MAX
                    + " clients (" + n + " dans cette tournée) : utilisez l'algorithme approché.");
        }

        int[][] metres = new int[n + 1][n + 1];
        int maxTroncon = 0;
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= n; j++) {
                metres[i][j] = (int) Math.round(probleme.distance(i, j) * 1000);
                maxTroncon = Math.max(maxTroncon, metres[i][j]);
            }
        }

        Model modele = new Model("boucle de livraison");
        IntVar[] position = modele.intVarArray("position", n, 0, n - 1);
        IntVar[] rang = modele.intVarArray("rang", n, 0, n - 1);
        modele.inverseChanneling(position, rang).post();

        IntVar[] troncons = new IntVar[n + 1];
        int[] depart = new int[n];
        int[] retour = new int[n];
        for (int c = 0; c < n; c++) {
            depart[c] = metres[0][c + 1];
            retour[c] = metres[c + 1][0];
        }
        troncons[0] = modele.intVar("depart", 0, maxTroncon, true);
        modele.element(troncons[0], depart, position[0]).post();
        if (n > 1) {
            Tuples tuples = new Tuples(true);
            for (int a = 0; a < n; a++) {
                for (int b = 0; b < n; b++) {
                    if (a != b) {
                        tuples.add(a, b, metres[a + 1][b + 1]);
                    }
                }
            }
            for (int k = 0; k + 1 < n; k++) {
                troncons[k + 1] = modele.intVar("troncon" + k, 0, maxTroncon, false);
                modele.table(new IntVar[]{position[k], position[k + 1], troncons[k + 1]}, tuples).post();
            }
        }
        troncons[n] = modele.intVar("retour", 0, maxTroncon, true);
        modele.element(troncons[n], retour, position[n - 1]).post();

        for (int a = 0; a < n; a++) {
            for (int b = 0; b < n; b++) {
                if (probleme.doitPreceder(a, b)) {
                    modele.arithm(rang[a], "<", rang[b]).post();
                }
            }
        }

        // Borne supérieure initiale : la boucle approchée (si elle existe) élague la séparation-évaluation.
        int borne = maxTroncon * (n + 1);
        try {
            int[] approchee = new PlusProcheVoisinSolveur().resoudre(probleme).ordre();
            borne = longueurMetres(metres, approchee);
        } catch (BoucleImpossibleException aucune) {
            // pas de borne : Choco prouvera lui-même l'absence de solution
        }
        IntVar total = modele.intVar("total", 0, borne, true);
        modele.sum(troncons, "=", total).post();
        modele.setObjective(Model.MINIMIZE, total);

        Solver solveur = modele.getSolver();
        solveur.limitTime(limiteMs);
        int[] meilleur = null;
        while (solveur.solve()) {
            meilleur = new int[n];
            for (int k = 0; k < n; k++) {
                meilleur[k] = position[k].getValue();
            }
        }
        boolean interrompu = solveur.isStopCriterionMet();
        if (meilleur == null) {
            throw new BoucleImpossibleException(interrompu
                    ? "La bibliothèque n'a trouvé aucune boucle dans le temps imparti."
                    : "Aucune boucle ne respecte l'ordre imposé par les masses des colis.");
        }
        return new ResultatBoucle(meilleur, probleme.longueur(meilleur), !interrompu);
    }

    /**
     * Au pire, la séparation-évaluation énumère tous les ordres valides : n! sans contrainte,
     * produit des k! des groupes de clients aux masses identiques avec la contrainte.
     */
    @Override
    public EstimationDifficulte estimer(ProblemeBoucle probleme) {
        int n = probleme.nbClients();
        String complexite = "O(n·P), P = ordres valides (produit des k! des groupes de masses identiques)";
        if (n > TAILLE_MAX) {
            return new EstimationDifficulte(algorithme(), Difficulte.HORS_DE_PORTEE, factorielle(n), complexite);
        }
        ProblemeBoucle.EspaceRecherche espace = probleme.espaceRecherche(PLAFOND_DENOMBREMENT);
        if (!espace.depasse() && espace.ordres() == 0) {
            return new EstimationDifficulte(algorithme(), Difficulte.IMPOSSIBLE, 0, complexite);
        }
        double operations = n * (espace.depasse() ? factorielle(n) : espace.ordres());
        return new EstimationDifficulte(algorithme(), Difficulte.selonOperations(operations), operations, complexite);
    }

    private static int longueurMetres(int[][] metres, int[] ordre) {
        int total = metres[0][ordre[0] + 1];
        for (int i = 0; i + 1 < ordre.length; i++) {
            total += metres[ordre[i] + 1][ordre[i + 1] + 1];
        }
        return total + metres[ordre[ordre.length - 1] + 1][0];
    }

    private static double factorielle(int n) {
        double resultat = 1;
        for (int i = 2; i <= n; i++) {
            resultat *= i;
        }
        return resultat;
    }
}
