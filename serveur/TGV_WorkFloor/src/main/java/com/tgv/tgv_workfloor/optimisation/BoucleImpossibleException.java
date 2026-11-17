package com.tgv.tgv_workfloor.optimisation;

import com.tgv.tgv_workfloor.exception.RegleMetierException;

/** Aucune boucle ne respecte les contraintes, ou le problème dépasse la capacité de l'algorithme. */
public class BoucleImpossibleException extends RegleMetierException {
    public BoucleImpossibleException(String message) {
        super(message);
    }
}
