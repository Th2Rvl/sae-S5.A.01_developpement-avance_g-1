package com.tgv.tgv_workfloor.optimisation;

import com.tgv.tgv_workfloor.modele.Algorithme;

/**
 * Estimation, avant calcul, du coût d'un algorithme pour un problème donné.
 *
 * @param operationsEstimees ordre de grandeur du nombre d'opérations élémentaires
 * @param complexite         formule de complexité affichable (ex. « O(n²·2ⁿ) »)
 */
public record EstimationDifficulte(Algorithme algorithme, Difficulte difficulte, double operationsEstimees,
                                   String complexite) {

    public boolean realisable() {
        return difficulte != Difficulte.HORS_DE_PORTEE && difficulte != Difficulte.IMPOSSIBLE;
    }
}
