package com.tgv.tgv_workfloor.exception;

/** Élément demandé absent (renvoyé en 404 par l'API). */
public class RessourceIntrouvableException extends RuntimeException {
    public RessourceIntrouvableException(String quoi, Object id) {
        super(quoi + " introuvable : " + id);
    }
}
