package com.tgv.tgv_workfloor.optimisation;

import java.util.Random;

/** Instances de test et solveur de référence par force brute (toutes les permutations). */
final class Instances {

    private Instances() {
    }

    /** Points aléatoires dans un carré de 50 km ; le point 0 est le dépôt. */
    static double[][] aleatoire(int nbClients, long graine) {
        Random hasard = new Random(graine);
        double[][] points = new double[nbClients + 1][2];
        for (double[] p : points) {
            p[0] = hasard.nextDouble() * 50;
            p[1] = hasard.nextDouble() * 50;
        }
        double[][] d = new double[nbClients + 1][nbClients + 1];
        for (int i = 0; i <= nbClients; i++) {
            for (int j = 0; j <= nbClients; j++) {
                d[i][j] = Math.hypot(points[i][0] - points[j][0], points[i][1] - points[j][1]);
            }
        }
        return d;
    }

    /** Précédences aléatoires sans cycle : a précède b seulement si a < b (probabilité p). */
    static boolean[][] precedencesAleatoires(int n, double p, long graine) {
        Random hasard = new Random(graine);
        boolean[][] prec = new boolean[n][n];
        for (int a = 0; a < n; a++) {
            for (int b = a + 1; b < n; b++) {
                prec[a][b] = hasard.nextDouble() < p;
            }
        }
        return prec;
    }

    /** Longueur optimale par énumération de toutes les permutations valides (n ≤ 8). */
    static double optimumForceBrute(ProblemeBoucle probleme) {
        int n = probleme.nbClients();
        int[] ordre = new int[n];
        for (int i = 0; i < n; i++) {
            ordre[i] = i;
        }
        double[] meilleur = {Double.POSITIVE_INFINITY};
        permuter(probleme, ordre, 0, meilleur);
        return meilleur[0];
    }

    private static void permuter(ProblemeBoucle probleme, int[] ordre, int k, double[] meilleur) {
        if (k == ordre.length) {
            if (probleme.estValide(ordre)) {
                meilleur[0] = Math.min(meilleur[0], probleme.longueur(ordre));
            }
            return;
        }
        for (int i = k; i < ordre.length; i++) {
            echanger(ordre, k, i);
            permuter(probleme, ordre, k + 1, meilleur);
            echanger(ordre, k, i);
        }
    }

    private static void echanger(int[] t, int i, int j) {
        int x = t[i];
        t[i] = t[j];
        t[j] = x;
    }
}
