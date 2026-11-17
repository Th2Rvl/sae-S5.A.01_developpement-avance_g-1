package com.tgv.tgv_workfloor.api;

import com.tgv.tgv_workfloor.modele.Tournee;
import com.tgv.tgv_workfloor.modele.Utilisateur;
import com.tgv.tgv_workfloor.service.TourneeService;
import com.tgv.tgv_workfloor.service.UtilisateurService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de l'interface Livraison (application mobile du chauffeur).
 * Le calcul de la boucle est fait ici, sur le serveur : le mobile ne reçoit que le résultat.
 */
@RestController
@RequestMapping("/api/tournees")
public class TourneeApiController {

    private final TourneeService tournees;
    private final UtilisateurService utilisateurs;

    public TourneeApiController(TourneeService tournees, UtilisateurService utilisateurs) {
        this.tournees = tournees;
        this.utilisateurs = utilisateurs;
    }

    /** Tournée à effectuer (onglets Colis, Carte et Boucle). */
    @GetMapping("/courante")
    public Dto.TourneeDto courante() {
        return ApiMapper.tournee(tournees.courante());
    }

    @GetMapping("/{id}")
    public Dto.TourneeDto detail(@PathVariable Long id) {
        return ApiMapper.tournee(tournees.trouver(id));
    }

    /** Difficulté estimée de chaque algorithme (boîte de dialogue « Calculer la boucle »). */
    @GetMapping("/{id}/difficultes")
    public Dto.DifficultesDto difficultes(@PathVariable Long id) {
        Tournee t = tournees.trouver(id);
        return new Dto.DifficultesDto(t.getClients().size(), t.getColis().size(), tournees.nombreOrdresPossibles(id),
                tournees.conflitsDeMasses(id), tournees.estimer(id).stream().map(ApiMapper::estimation).toList());
    }

    /** La boucle respecte toujours l'ordre des masses ; l'algorithme optimise l'ordre des masses identiques. */
    @PostMapping("/{id}/calcul")
    public Dto.TourneeDto calculer(@PathVariable Long id, @Valid @RequestBody Dto.CalculDemande demande) {
        return ApiMapper.tournee(tournees.calculer(id, demande.algorithme()));
    }

    /** Ordre de passage choisi par le chauffeur (écran « Ordre manuel »). */
    @PutMapping("/{id}/ordre")
    public Dto.TourneeDto ordreManuel(@PathVariable Long id, @Valid @RequestBody Dto.OrdreManuelDemande demande) {
        return ApiMapper.tournee(tournees.ordreManuel(id, demande.clientIds()));
    }

    /** Ordre de chargement, du bas (position 1) vers le haut du plateau. */
    @GetMapping("/{id}/chargement")
    public List<Dto.ChargementDto> chargement(@PathVariable Long id) {
        return ApiMapper.chargement(tournees.trouver(id));
    }

    @PostMapping("/{id}/validation")
    public Dto.TourneeDto valider(@PathVariable Long id, @AuthenticationPrincipal UserDetails connecte) {
        return ApiMapper.tournee(tournees.valider(id, utilisateur(connecte)));
    }

    @PostMapping("/{id}/bons")
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.BonDto bon(@PathVariable Long id, @Valid @RequestBody Dto.BonDemande demande,
                          @AuthenticationPrincipal UserDetails connecte) {
        return ApiMapper.bon(tournees.enregistrerBon(id, demande.clientId(), demande.colisIds(), demande.etat(),
                demande.commentaire(), utilisateur(connecte)));
    }

    @PostMapping("/{id}/fin")
    public Dto.TourneeDto terminer(@PathVariable Long id) {
        return ApiMapper.tournee(tournees.terminer(id));
    }

    private Utilisateur utilisateur(UserDetails connecte) {
        return utilisateurs.parIdentifiant(connecte.getUsername()).orElse(null);
    }
}
