package com.tgv.tgv_workfloor.service;

import com.tgv.tgv_workfloor.depot.UtilisateurDepot;
import com.tgv.tgv_workfloor.exception.RegleMetierException;
import com.tgv.tgv_workfloor.exception.RessourceIntrouvableException;
import com.tgv.tgv_workfloor.modele.Role;
import com.tgv.tgv_workfloor.modele.Utilisateur;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Comptes atelier et chauffeurs ; sert aussi de source d'authentification à Spring Security. */
@Service
public class UtilisateurService implements UserDetailsService {

    private final UtilisateurDepot utilisateurs;
    private final PasswordEncoder encodeur;

    public UtilisateurService(UtilisateurDepot utilisateurs, PasswordEncoder encodeur) {
        this.utilisateurs = utilisateurs;
        this.encodeur = encodeur;
    }

    @Override
    public UserDetails loadUserByUsername(String identifiant) {
        Utilisateur u = utilisateurs.findByIdentifiant(identifiant)
                .orElseThrow(() -> new UsernameNotFoundException(identifiant));
        return User.withUsername(u.getIdentifiant()).password(u.getMotDePasseHash()).roles(u.getRole().name()).build();
    }

    public Optional<Utilisateur> parIdentifiant(String identifiant) {
        return utilisateurs.findByIdentifiant(identifiant);
    }

    /** Vérifie un couple identifiant / mot de passe (connexion de l'application mobile). */
    public Optional<Utilisateur> authentifier(String identifiant, String motDePasse) {
        if (identifiant == null || motDePasse == null) {
            return Optional.empty();
        }
        return utilisateurs.findByIdentifiant(identifiant.trim())
                .filter(u -> encodeur.matches(motDePasse, u.getMotDePasseHash()));
    }

    public List<Utilisateur> lister() {
        return utilisateurs.findAll().stream()
                .sorted(Comparator.comparing(Utilisateur::getRole).thenComparing(Utilisateur::getIdentifiant))
                .toList();
    }

    public synchronized Utilisateur creer(String identifiant, String nom, String motDePasse, Role role) {
        if (identifiant == null || identifiant.isBlank() || motDePasse == null || motDePasse.length() < 4) {
            throw new RegleMetierException("Identifiant obligatoire et mot de passe d'au moins 4 caractères.");
        }
        if (utilisateurs.findByIdentifiant(identifiant.trim()).isPresent()) {
            throw new RegleMetierException("L'identifiant « " + identifiant.trim() + " » est déjà utilisé.");
        }
        return utilisateurs.save(new Utilisateur(identifiant.trim(), encodeur.encode(motDePasse),
                nom == null || nom.isBlank() ? identifiant.trim() : nom.trim(), role));
    }

    public synchronized void supprimer(Long id, String identifiantConnecte) {
        Utilisateur u = utilisateurs.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Utilisateur", id));
        if (u.getIdentifiant().equalsIgnoreCase(identifiantConnecte)) {
            throw new RegleMetierException("Vous ne pouvez pas supprimer votre propre compte.");
        }
        utilisateurs.delete(u);
    }
}
