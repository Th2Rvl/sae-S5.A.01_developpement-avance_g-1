package com.tgv.tgv_workfloor.optimisation;

import com.tgv.tgv_workfloor.modele.Algorithme;

/** Algorithme de calcul de la boucle de livraison (patron Stratégie). */
public interface SolveurBoucle {

    Algorithme algorithme();

    /**
     * Calcule une boucle respectant les précédences du problème.
     *
     * @throws BoucleImpossibleException si aucune boucle ne respecte les précédences,
     *                                   ou si le problème est trop grand pour cet algorithme
     */
    ResultatBoucle resoudre(ProblemeBoucle probleme);

    EstimationDifficulte estimer(ProblemeBoucle probleme);
}
