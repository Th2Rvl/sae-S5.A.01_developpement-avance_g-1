package com.tgv.tgv_workfloor.optimisation;

import java.util.ArrayList;
import java.util.List;

/**
 * Traduit la règle de chargement en contraintes d'ordre de livraison.
 * <p>
 * Les colis sont empilés par masse décroissante (les plus lourds en bas) et déchargés par le dessus.
 * Si un colis du client a est plus léger qu'un colis du client b, il est posé au-dessus : a doit donc
 * être livré avant b. Avec min/max les masses extrêmes des colis d'un client :
 * a précède b ⟺ min(a) &lt; max(b).
 * Si a précède b et b précède a (intervalles de masses qui se chevauchent), aucun chargement
 * ne convient : les deux clients sont « en conflit ».
 */
public final class ContraintesMasses {

    private ContraintesMasses() {
    }

    /**
     * @param massesParClient pour chaque client (indice 0..n-1), les masses de ses colis
     */
    public static boolean[][] precedences(List<double[]> massesParClient) {
        int n = massesParClient.size();
        double[] min = new double[n];
        double[] max = new double[n];
        for (int c = 0; c < n; c++) {
            min[c] = Double.POSITIVE_INFINITY;
            max[c] = Double.NEGATIVE_INFINITY;
            for (double m : massesParClient.get(c)) {
                min[c] = Math.min(min[c], m);
                max[c] = Math.max(max[c], m);
            }
        }
        boolean[][] precede = new boolean[n][n];
        for (int a = 0; a < n; a++) {
            for (int b = 0; b < n; b++) {
                precede[a][b] = a != b && min[a] < max[b];
            }
        }
        return precede;
    }

    /** Couples {a, b} (a &lt; b) dont les masses s'entrecroisent. */
    public static List<int[]> conflits(boolean[][] precedences) {
        List<int[]> conflits = new ArrayList<>();
        for (int a = 0; a < precedences.length; a++) {
            for (int b = a + 1; b < precedences.length; b++) {
                if (precedences[a][b] && precedences[b][a]) {
                    conflits.add(new int[]{a, b});
                }
            }
        }
        return conflits;
    }
}
