package com.tgv.tgv_workfloor.api;

import com.tgv.tgv_workfloor.exception.RessourceIntrouvableException;
import com.tgv.tgv_workfloor.service.UtilisateurService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Connexion de l'application mobile. Après une connexion réussie, le mobile envoie l'en-tête
 * « Authorization: Basic base64(identifiant:motDePasse) » à chaque requête.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final UtilisateurService utilisateurs;

    public AuthApiController(UtilisateurService utilisateurs) {
        this.utilisateurs = utilisateurs;
    }

    @PostMapping("/connexion")
    public ResponseEntity<Object> connexion(@Valid @RequestBody Dto.ConnexionDemande demande) {
        return utilisateurs.authentifier(demande.identifiant(), demande.motDePasse())
                .<ResponseEntity<Object>>map(u -> ResponseEntity.ok(ApiMapper.utilisateur(u)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new Dto.Erreur(401, "Identifiant ou mot de passe incorrect")));
    }

    @GetMapping("/moi")
    public Dto.UtilisateurDto moi(@AuthenticationPrincipal UserDetails connecte) {
        return utilisateurs.parIdentifiant(connecte.getUsername()).map(ApiMapper::utilisateur)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur", connecte.getUsername()));
    }
}
