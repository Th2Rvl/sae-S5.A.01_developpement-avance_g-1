package com.tgv.tgv_workfloor.optimisation;

public enum Difficulte {
    FAIBLE("faible"),
    MOYENNE("moyenne"),
    ELEVEE("élevée"),
    HORS_DE_PORTEE("hors de portée"),
    /** Aucun ordre de livraison ne respecte les masses : rien à calculer. */
    IMPOSSIBLE("impossible");

    private final String libelle;

    Difficulte(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    /**
     * Classement d'un nombre d'opérations élémentaires estimé
     * (≈ 10⁵ : instantané ; ≈ 10⁷ : quelques dixièmes de seconde ; au-delà : plusieurs secondes).
     */
    public static Difficulte selonOperations(double operations) {
        if (operations <= 1e5) {
            return FAIBLE;
        }
        if (operations <= 1e7) {
            return MOYENNE;
        }
        return ELEVEE;
    }
}
