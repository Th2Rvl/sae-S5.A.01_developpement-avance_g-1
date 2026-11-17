package com.tgv.tgv_workfloor.modele;

/** Compte de connexion : responsable atelier (web) ou chauffeur (mobile). */
public class Utilisateur implements Identifiable {
    private Long id;
    private String identifiant;
    private String motDePasseHash;
    private String nom;
    private Role role;

    public Utilisateur() {
    }

    public Utilisateur(String identifiant, String motDePasseHash, String nom, Role role) {
        this.identifiant = identifiant;
        this.motDePasseHash = motDePasseHash;
        this.nom = nom;
        this.role = role;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getIdentifiant() {
        return identifiant;
    }

    public void setIdentifiant(String identifiant) {
        this.identifiant = identifiant;
    }

    public String getMotDePasseHash() {
        return motDePasseHash;
    }

    public void setMotDePasseHash(String motDePasseHash) {
        this.motDePasseHash = motDePasseHash;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
