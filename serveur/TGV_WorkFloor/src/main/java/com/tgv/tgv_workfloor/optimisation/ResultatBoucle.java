package com.tgv.tgv_workfloor.optimisation;

/**
 * Boucle trouvée : ordre de passage des clients (indices 0..n-1), longueur en km,
 * et indication que l'optimalité est prouvée (faux pour l'approché ou un calcul interrompu).
 */
public record ResultatBoucle(int[] ordre, double distanceKm, boolean optimaliteProuvee) {
}
