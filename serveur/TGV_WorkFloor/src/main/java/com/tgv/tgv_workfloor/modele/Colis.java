package com.tgv.tgv_workfloor.modele;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Emballage d'une ou plusieurs pièces d'une même commande (donc d'un seul client). */
public class Colis implements Identifiable {
    private Long id;
    private String reference;
    private Commande commande;
    private final List<Piece> pieces = new ArrayList<>();
    private int longueurCm;
    private int largeurCm;
    private int hauteurCm;
    private StatutColis statut = StatutColis.A_LIVRER;
    private Tournee tournee;
    private BonLivraison bon;
    /** Jour où le colis a été emballé, donc prêt à partir. */
    private LocalDate dateDisponibilite;
    /** Jour de livraison imposé par la règle de planification (voir PlanificationLivraisons). */
    private LocalDate dateLivraisonPrevue;
    /** Vrai si le colis est revenu d'une tournée sans être livré : il part alors le jeudi. */
    private boolean reporte;

    public Colis() {
    }

    public Colis(String reference, Commande commande, int longueurCm, int largeurCm, int hauteurCm) {
        this.reference = reference;
        this.commande = commande;
        this.longueurCm = longueurCm;
        this.largeurCm = largeurCm;
        this.hauteurCm = hauteurCm;
    }

    public double getPoidsKg() {
        return pieces.stream().mapToDouble(Piece::getPoidsKg).sum();
    }

    public Client getClient() {
        return commande.getClient();
    }

    public String getDimensions() {
        return longueurCm + " × " + largeurCm + " × " + hauteurCm + " cm";
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

    public Commande getCommande() {
        return commande;
    }

    public void setCommande(Commande commande) {
        this.commande = commande;
    }

    public List<Piece> getPieces() {
        return pieces;
    }

    public int getLongueurCm() {
        return longueurCm;
    }

    public void setLongueurCm(int longueurCm) {
        this.longueurCm = longueurCm;
    }

    public int getLargeurCm() {
        return largeurCm;
    }

    public void setLargeurCm(int largeurCm) {
        this.largeurCm = largeurCm;
    }

    public int getHauteurCm() {
        return hauteurCm;
    }

    public void setHauteurCm(int hauteurCm) {
        this.hauteurCm = hauteurCm;
    }

    public StatutColis getStatut() {
        return statut;
    }

    public void setStatut(StatutColis statut) {
        this.statut = statut;
    }

    public Tournee getTournee() {
        return tournee;
    }

    public void setTournee(Tournee tournee) {
        this.tournee = tournee;
    }

    public BonLivraison getBon() {
        return bon;
    }

    public void setBon(BonLivraison bon) {
        this.bon = bon;
    }

    public LocalDate getDateDisponibilite() {
        return dateDisponibilite;
    }

    public void setDateDisponibilite(LocalDate dateDisponibilite) {
        this.dateDisponibilite = dateDisponibilite;
    }

    public LocalDate getDateLivraisonPrevue() {
        return dateLivraisonPrevue;
    }

    public void setDateLivraisonPrevue(LocalDate dateLivraisonPrevue) {
        this.dateLivraisonPrevue = dateLivraisonPrevue;
    }

    public boolean isReporte() {
        return reporte;
    }

    public void setReporte(boolean reporte) {
        this.reporte = reporte;
    }
}
