package com.tgv.tgv_workfloor.modele;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Tournée d'un camion pour une date : colis embarqués, boucle de livraison (arrêts),
 * ordre de chargement (du bas vers le haut du plateau) et bons de livraison.
 */
public class Tournee implements Identifiable {
    private Long id;
    private LocalDate date;
    private Atelier atelier;
    private Utilisateur chauffeur;
    private StatutTournee statut = StatutTournee.PREVUE;
    private final List<Colis> colis = new ArrayList<>();
    private final List<Arret> arrets = new ArrayList<>();
    /** Colis du bas (chargé en premier) vers le haut (déchargé en premier). */
    private final List<Colis> chargement = new ArrayList<>();
    private final List<BonLivraison> bons = new ArrayList<>();
    private double distanceRetourKm;
    private Algorithme algorithme;
    private boolean optimaliteProuvee;
    private long dureeCalculMs;
    private LocalDateTime dateCalcul;

    public Tournee() {
    }

    public Tournee(LocalDate date, Atelier atelier) {
        this.date = date;
        this.atelier = atelier;
    }

    /** Clients distincts concernés par les colis, dans l'ordre d'apparition. */
    public List<Client> getClients() {
        return new ArrayList<>(colis.stream().map(Colis::getClient)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    public List<Colis> colisDuClient(Client client) {
        return colis.stream().filter(c -> c.getClient().getId().equals(client.getId())).toList();
    }

    public double getPoidsTotalKg() {
        return colis.stream().mapToDouble(Colis::getPoidsKg).sum();
    }

    public double getDistanceTotaleKm() {
        if (arrets.isEmpty()) {
            return 0;
        }
        return arrets.stream().mapToDouble(Arret::getDistanceDepuisPrecedentKm).sum() + distanceRetourKm;
    }

    public boolean isBoucleCalculee() {
        return !arrets.isEmpty();
    }

    public Optional<Arret> arretDuClient(Client client) {
        return arrets.stream().filter(a -> a.getClient().getId().equals(client.getId())).findFirst();
    }

    public Optional<BonLivraison> bonDuClient(Client client) {
        return bons.stream().filter(b -> b.getClient().getId().equals(client.getId())).findFirst();
    }

    /** Nombre d'arrêts du groupe de masses : au-delà de 1, leur ordre est libre. */
    public long tailleGroupe(int groupe) {
        return arrets.stream().filter(a -> a.getGroupe() == groupe).count();
    }

    /** Oublie la boucle calculée (par exemple quand la liste des colis change). */
    public void effacerBoucle() {
        arrets.clear();
        chargement.clear();
        algorithme = null;
        distanceRetourKm = 0;
        optimaliteProuvee = false;
        dureeCalculMs = 0;
        dateCalcul = null;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Atelier getAtelier() {
        return atelier;
    }

    public void setAtelier(Atelier atelier) {
        this.atelier = atelier;
    }

    public Utilisateur getChauffeur() {
        return chauffeur;
    }

    public void setChauffeur(Utilisateur chauffeur) {
        this.chauffeur = chauffeur;
    }

    public StatutTournee getStatut() {
        return statut;
    }

    public void setStatut(StatutTournee statut) {
        this.statut = statut;
    }

    public List<Colis> getColis() {
        return colis;
    }

    public List<Arret> getArrets() {
        return arrets;
    }

    public List<Colis> getChargement() {
        return chargement;
    }

    public List<BonLivraison> getBons() {
        return bons;
    }

    public double getDistanceRetourKm() {
        return distanceRetourKm;
    }

    public void setDistanceRetourKm(double distanceRetourKm) {
        this.distanceRetourKm = distanceRetourKm;
    }

    public Algorithme getAlgorithme() {
        return algorithme;
    }

    public void setAlgorithme(Algorithme algorithme) {
        this.algorithme = algorithme;
    }

    public boolean isOptimaliteProuvee() {
        return optimaliteProuvee;
    }

    public void setOptimaliteProuvee(boolean optimaliteProuvee) {
        this.optimaliteProuvee = optimaliteProuvee;
    }

    public long getDureeCalculMs() {
        return dureeCalculMs;
    }

    public void setDureeCalculMs(long dureeCalculMs) {
        this.dureeCalculMs = dureeCalculMs;
    }

    public LocalDateTime getDateCalcul() {
        return dateCalcul;
    }

    public void setDateCalcul(LocalDateTime dateCalcul) {
        this.dateCalcul = dateCalcul;
    }
}
