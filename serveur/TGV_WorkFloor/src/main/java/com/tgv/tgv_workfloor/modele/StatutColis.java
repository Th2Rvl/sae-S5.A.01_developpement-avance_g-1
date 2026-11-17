package com.tgv.tgv_workfloor.modele;

/** Statut d'un colis (repris de la charte : à livrer, en cours, livré, refusé). */
public enum StatutColis {
    A_LIVRER("À livrer"),
    EN_COURS("En cours"),
    LIVRE("Livré"),
    REFUSE("Refusé");

    private final String libelle;

    StatutColis(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
