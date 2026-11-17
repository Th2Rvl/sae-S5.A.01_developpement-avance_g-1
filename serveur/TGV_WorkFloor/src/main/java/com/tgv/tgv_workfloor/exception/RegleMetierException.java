package com.tgv.tgv_workfloor.exception;

/** Action refusée par une règle métier (message affichable tel quel à l'utilisateur). */
public class RegleMetierException extends RuntimeException {
    public RegleMetierException(String message) {
        super(message);
    }
}
