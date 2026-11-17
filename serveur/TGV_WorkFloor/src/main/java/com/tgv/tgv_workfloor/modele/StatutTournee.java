package com.tgv.tgv_workfloor.modele;

public enum StatutTournee {
    PREVUE("Prévue"),
    VALIDEE("Validée"),
    EN_COURS("En cours"),
    TERMINEE("Terminée");

    private final String libelle;

    StatutTournee(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    /** Une tournée validée ou commencée a un ensemble de colis figé. */
    public boolean estFigee() {
        return this != PREVUE;
    }
}
