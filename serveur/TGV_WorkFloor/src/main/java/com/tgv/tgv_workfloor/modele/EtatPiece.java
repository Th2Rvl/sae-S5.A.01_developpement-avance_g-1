package com.tgv.tgv_workfloor.modele;

/** Avancement d'une pièce, dans l'ordre du cycle de vie. */
public enum EtatPiece {
    A_FAIRE("À faire"),
    EN_FABRICATION("En cours de fabrication"),
    FABRIQUEE("Fabriquée"),
    LIVREE("Livrée");

    private final String libelle;

    EtatPiece(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
