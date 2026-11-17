package com.tgv.tgv_workfloor.modele;

/** Point de chargement des camions (un seul aujourd'hui, plusieurs possibles demain). */
public class Atelier implements Identifiable {
    private Long id;
    private String nom;
    private String adresse;
    private double latitude;
    private double longitude;

    public Atelier() {
    }

    public Atelier(String nom, String adresse, double latitude, double longitude) {
        this.nom = nom;
        this.adresse = adresse;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
