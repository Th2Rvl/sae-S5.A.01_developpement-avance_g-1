package com.tgv.tgv_workfloor.optimisation;

/**
 * Données d'entrée des algorithmes, indépendantes du modèle métier.
 * <ul>
 *   <li>{@code distances} : matrice (n+1)×(n+1) en km ; l'indice 0 est le dépôt (atelier),
 *       l'indice c+1 est le client c (c de 0 à n-1).</li>
 *   <li>{@code precedences[a][b]} vrai si le client a doit être livré avant le client b
 *       (déduit de l'ordre des masses : voir {@link ContraintesMasses}).</li>
 * </ul>
 * Une solution est une permutation des clients 0..n-1 ; la boucle part du dépôt et y revient.
 */
public final class ProblemeBoucle {

    private final double[][] distances;
    private final boolean[][] precedences;
    private final int nbClients;

    public ProblemeBoucle(double[][] distances, boolean[][] precedences) {
        if (distances == null || distances.length == 0) {
            throw new IllegalArgumentException("La matrice doit contenir au moins le dépôt");
        }
        for (double[] ligne : distances) {
            if (ligne.length != distances.length) {
                throw new IllegalArgumentException("La matrice des distances doit être carrée");
            }
        }
        this.distances = distances;
        this.nbClients = distances.length - 1;
        this.precedences = precedences != null ? precedences : new boolean[nbClients][nbClients];
        if (this.precedences.length != nbClients) {
            throw new IllegalArgumentException("La matrice des précédences doit être n×n");
        }
    }

    public static ProblemeBoucle sansContrainte(double[][] distances) {
        return new ProblemeBoucle(distances, null);
    }

    public int nbClients() {
        return nbClients;
    }

    /** Distance entre deux points de la matrice (0 = dépôt). */
    public double distance(int de, int vers) {
        return distances[de][vers];
    }

    /** Distance entre deux clients (indices clients 0..n-1). */
    public double distanceClients(int a, int b) {
        return distances[a + 1][b + 1];
    }

    public double depuisDepot(int client) {
        return distances[0][client + 1];
    }

    public double versDepot(int client) {
        return distances[client + 1][0];
    }

    public boolean doitPreceder(int a, int b) {
        return precedences[a][b];
    }

    public boolean aDesPrecedences() {
        for (boolean[] ligne : precedences) {
            for (boolean p : ligne) {
                if (p) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Longueur de la boucle dépôt → ordre[0] → … → ordre[n-1] → dépôt. */
    public double longueur(int[] ordre) {
        if (ordre.length == 0) {
            return 0;
        }
        double total = depuisDepot(ordre[0]);
        for (int i = 0; i + 1 < ordre.length; i++) {
            total += distanceClients(ordre[i], ordre[i + 1]);
        }
        return total + versDepot(ordre[ordre.length - 1]);
    }

    /**
     * Taille réelle de l'espace de recherche compte tenu des précédences.
     *
     * @param etats  nombre d'ensembles de clients « livrables en premier » (idéaux de l'ordre partiel),
     *               c'est-à-dire d'états explorés par Held-Karp ; vaut 2ⁿ sans contrainte
     * @param ordres nombre d'ordres de livraison valides ; avec les masses, c'est le produit des k!
     *               pour chaque groupe de k clients aux colis de masses identiques (0 si impossible)
     * @param depasse vrai si le dénombrement a été interrompu (plafond atteint ou n &gt; 63)
     */
    public record EspaceRecherche(long etats, double ordres, boolean depasse) {
    }

    /** Dénombre couche par couche les ensembles atteignables, en s'arrêtant au-delà du plafond. */
    public EspaceRecherche espaceRecherche(long plafondEtats) {
        int n = nbClients;
        if (n == 0) {
            return new EspaceRecherche(1, 1, false);
        }
        if (n > 63) {
            return new EspaceRecherche(Long.MAX_VALUE, Double.POSITIVE_INFINITY, true);
        }
        long[] predecesseurs = masquesPredecesseurs();
        java.util.Map<Long, Double> couche = java.util.Map.of(0L, 1.0);
        long etats = 1;
        for (int taille = 0; taille < n; taille++) {
            java.util.Map<Long, Double> suivante = new java.util.HashMap<>();
            for (var entree : couche.entrySet()) {
                long ensemble = entree.getKey();
                for (int k = 0; k < n; k++) {
                    long bit = 1L << k;
                    if ((ensemble & bit) == 0 && (predecesseurs[k] & ~ensemble) == 0) {
                        suivante.merge(ensemble | bit, entree.getValue(), Double::sum);
                    }
                }
            }
            etats += suivante.size();
            if (etats > plafondEtats) {
                return new EspaceRecherche(etats, Double.POSITIVE_INFINITY, true);
            }
            couche = suivante;
        }
        return new EspaceRecherche(etats, couche.getOrDefault((1L << n) - 1, 0.0), false);
    }

    /** predecesseurs[b] : masque des clients qui doivent être livrés avant b (n ≤ 63). */
    long[] masquesPredecesseurs() {
        long[] predecesseurs = new long[nbClients];
        for (int a = 0; a < nbClients; a++) {
            for (int b = 0; b < nbClients; b++) {
                if (precedences[a][b]) {
                    predecesseurs[b] |= 1L << a;
                }
            }
        }
        return predecesseurs;
    }

    /** Vrai si l'ordre est une permutation des clients respectant toutes les précédences. */
    public boolean estValide(int[] ordre) {
        if (ordre.length != nbClients) {
            return false;
        }
        int[] rang = new int[nbClients];
        java.util.Arrays.fill(rang, -1);
        for (int i = 0; i < ordre.length; i++) {
            if (ordre[i] < 0 || ordre[i] >= nbClients || rang[ordre[i]] != -1) {
                return false;
            }
            rang[ordre[i]] = i;
        }
        for (int a = 0; a < nbClients; a++) {
            for (int b = 0; b < nbClients; b++) {
                if (precedences[a][b] && rang[a] > rang[b]) {
                    return false;
                }
            }
        }
        return true;
    }
}
