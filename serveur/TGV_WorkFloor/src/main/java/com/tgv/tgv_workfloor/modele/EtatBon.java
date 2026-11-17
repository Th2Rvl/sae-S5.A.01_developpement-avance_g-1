package com.tgv.tgv_workfloor.modele;

/** Résultat de la remise des colis chez un client. */
public enum EtatBon {
    CONFORME("Conforme"),
    RESERVES("Livré avec réserves"),
    REFUSE("Refusé");

    private final String libelle;

    EtatBon(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
