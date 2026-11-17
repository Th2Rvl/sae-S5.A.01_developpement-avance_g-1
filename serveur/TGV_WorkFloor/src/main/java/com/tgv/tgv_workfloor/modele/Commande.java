package com.tgv.tgv_workfloor.modele;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Commande d'un client : ensemble de pièces à fabriquer puis à livrer à une date prévue. */
public class Commande implements Identifiable {
    private Long id;
    private String reference;
    private Client client;
    private LocalDate datePrevue;
    private LocalDate dateCreation;
    private final List<Piece> pieces = new ArrayList<>();
    private final List<Colis> colis = new ArrayList<>();

    public Commande() {
    }

    public Commande(String reference, Client client, LocalDate datePrevue, LocalDate dateCreation) {
        this.reference = reference;
        this.client = client;
        this.datePrevue = datePrevue;
        this.dateCreation = dateCreation;
    }

    public double getPoidsTotalKg() {
        return pieces.stream().mapToDouble(Piece::getPoidsKg).sum();
    }

    /** État le moins avancé parmi les pièces (A_FAIRE si la commande est vide). */
    public EtatPiece getEtat() {
        return pieces.stream().map(Piece::getEtat).min(Comparator.naturalOrder()).orElse(EtatPiece.A_FAIRE);
    }

    /** Pièces fabriquées qui ne sont pas encore emballées dans un colis. */
    public List<Piece> getPiecesAEmballer() {
        return pieces.stream().filter(p -> p.getEtat() == EtatPiece.FABRIQUEE && p.getColis() == null).toList();
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

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public LocalDate getDatePrevue() {
        return datePrevue;
    }

    public void setDatePrevue(LocalDate datePrevue) {
        this.datePrevue = datePrevue;
    }

    public LocalDate getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDate dateCreation) {
        this.dateCreation = dateCreation;
    }

    public List<Piece> getPieces() {
        return pieces;
    }

    public List<Colis> getColis() {
        return colis;
    }
}
