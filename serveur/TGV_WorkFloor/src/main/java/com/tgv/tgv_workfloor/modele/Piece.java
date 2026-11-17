package com.tgv.tgv_workfloor.modele;

/** Pièce fabriquée pour un client, repérée par une référence et un poids. */
public class Piece implements Identifiable {
    private Long id;
    private String reference;
    private String designation;
    private double poidsKg;
    private EtatPiece etat = EtatPiece.A_FAIRE;
    private Commande commande;
    private Colis colis;

    public Piece() {
    }

    public Piece(String reference, String designation, double poidsKg, EtatPiece etat, Commande commande) {
        this.reference = reference;
        this.designation = designation;
        this.poidsKg = poidsKg;
        this.etat = etat;
        this.commande = commande;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public double getPoidsKg() {
        return poidsKg;
    }

    public void setPoidsKg(double poidsKg) {
        this.poidsKg = poidsKg;
    }

    public EtatPiece getEtat() {
        return etat;
    }

    public void setEtat(EtatPiece etat) {
        this.etat = etat;
    }

    public Commande getCommande() {
        return commande;
    }

    public void setCommande(Commande commande) {
        this.commande = commande;
    }

    public Colis getColis() {
        return colis;
    }

    public void setColis(Colis colis) {
        this.colis = colis;
    }
}
