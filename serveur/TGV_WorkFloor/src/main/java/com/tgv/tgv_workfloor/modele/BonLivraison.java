package com.tgv.tgv_workfloor.modele;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Bon signé chez un client : colis remis et état de la remise. */
public class BonLivraison implements Identifiable {
    private Long id;
    private String numero;
    private Tournee tournee;
    private Client client;
    private Utilisateur chauffeur;
    private LocalDateTime dateHeure;
    private EtatBon etat;
    private String commentaire;
    private final List<Colis> colis = new ArrayList<>();

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Tournee getTournee() {
        return tournee;
    }

    public void setTournee(Tournee tournee) {
        this.tournee = tournee;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Utilisateur getChauffeur() {
        return chauffeur;
    }

    public void setChauffeur(Utilisateur chauffeur) {
        this.chauffeur = chauffeur;
    }

    public LocalDateTime getDateHeure() {
        return dateHeure;
    }

    public void setDateHeure(LocalDateTime dateHeure) {
        this.dateHeure = dateHeure;
    }

    public EtatBon getEtat() {
        return etat;
    }

    public void setEtat(EtatBon etat) {
        this.etat = etat;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public List<Colis> getColis() {
        return colis;
    }
}
