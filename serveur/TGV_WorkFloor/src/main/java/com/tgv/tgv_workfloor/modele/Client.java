package com.tgv.tgv_workfloor.modele;

/** Client de T.G.V, avec son unique adresse de livraison. */
public class Client implements Identifiable {
    private Long id;
    private String nom;
    private String adresse;
    private String codePostal;
    private String ville;
    private String telephone;
    private double latitude;
    private double longitude;

    public Client() {
    }

    public Client(String nom, String adresse, String codePostal, String ville, String telephone,
                  double latitude, double longitude) {
        this.nom = nom;
        this.adresse = adresse;
        this.codePostal = codePostal;
        this.ville = ville;
        this.telephone = telephone;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /** Adresse sur une ligne : « 12 rue des Forges, 12850 Onet-le-Château ». */
    public String getAdresseComplete() {
        return adresse + ", " + codePostal + " " + ville;
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

    public String getCodePostal() {
        return codePostal;
    }

    public void setCodePostal(String codePostal) {
        this.codePostal = codePostal;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
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
