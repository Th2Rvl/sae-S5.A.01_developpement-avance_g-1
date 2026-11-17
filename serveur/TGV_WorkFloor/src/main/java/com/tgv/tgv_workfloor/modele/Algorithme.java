package com.tgv.tgv_workfloor.modele;

/** Façon dont la boucle de livraison a été obtenue. */
public enum Algorithme {
    EXACT_MAISON("Exacte — algorithme maison"),
    EXACT_BIBLIOTHEQUE("Exacte — bibliothèque"),
    APPROCHE_MAISON("Approchée — algorithme maison"),
    MANUEL("Ordre manuel");

    private final String libelle;

    Algorithme(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public boolean estCalcule() {
        return this != MANUEL;
    }
}
