package com.tgv.tgv_workfloor.modele;

/** Objet métier repéré par un identifiant technique (attribué par le dépôt). */
public interface Identifiable {
    Long getId();

    void setId(Long id);
}
