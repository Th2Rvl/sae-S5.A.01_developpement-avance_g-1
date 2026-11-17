package com.tgv.tgv_workfloor.web.formulaire;

import com.tgv.tgv_workfloor.modele.Client;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Champs saisis dans le formulaire client. */
public class ClientFormulaire {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100)
    private String nom;

    @NotBlank(message = "L'adresse est obligatoire")
    @Size(max = 255)
    private String adresse;

    @NotBlank(message = "Le code postal est obligatoire")
    @Pattern(regexp = "\\d{5}", message = "5 chiffres attendus")
    private String codePostal;

    @NotBlank(message = "La ville est obligatoire")
    private String ville;

    @Pattern(regexp = "^$|^[0-9 +.]{8,20}$", message = "Numéro de téléphone invalide")
    private String telephone;

    @NotNull(message = "Placez le client sur la carte ou saisissez sa latitude")
    @DecimalMin("-90") @DecimalMax("90")
    private Double latitude;

    @NotNull(message = "Placez le client sur la carte ou saisissez sa longitude")
    @DecimalMin("-180") @DecimalMax("180")
    private Double longitude;

    public static ClientFormulaire depuis(Client c) {
        ClientFormulaire f = new ClientFormulaire();
        f.nom = c.getNom();
        f.adresse = c.getAdresse();
        f.codePostal = c.getCodePostal();
        f.ville = c.getVille();
        f.telephone = c.getTelephone();
        f.latitude = c.getLatitude();
        f.longitude = c.getLongitude();
        return f;
    }

    public Client versClient() {
        return new Client(nom.trim(), adresse.trim(), codePostal.trim(), ville.trim(),
                telephone == null || telephone.isBlank() ? null : telephone.trim(), latitude, longitude);
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

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
