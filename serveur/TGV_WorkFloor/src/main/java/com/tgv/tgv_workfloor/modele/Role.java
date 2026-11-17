package com.tgv.tgv_workfloor.modele;

public enum Role {
    ATELIER("Atelier"),
    CHAUFFEUR("Chauffeur");

    private final String libelle;

    Role(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
