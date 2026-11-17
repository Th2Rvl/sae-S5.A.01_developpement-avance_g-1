package com.tgv.tgv_workfloor.web;

import com.tgv.tgv_workfloor.exception.RegleMetierException;
import com.tgv.tgv_workfloor.exception.RessourceIntrouvableException;
import com.tgv.tgv_workfloor.modele.Algorithme;
import com.tgv.tgv_workfloor.modele.Arret;
import com.tgv.tgv_workfloor.modele.BonLivraison;
import com.tgv.tgv_workfloor.modele.EtatBon;
import com.tgv.tgv_workfloor.modele.Tournee;
import com.tgv.tgv_workfloor.modele.Utilisateur;
import com.tgv.tgv_workfloor.service.TourneeService;
import com.tgv.tgv_workfloor.service.UtilisateurService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Espace chauffeur du site, pensé pour le téléphone : tournée à effectuer, calcul ou ajustement de la boucle,
 * validation du chargement, bons de livraison à chaque arrêt et fin de tournée (mêmes règles que l'API mobile).
 */
@Controller
@RequestMapping("/chauffeur")
public class ChauffeurWebController {

    private static final String RETOUR = "redirect:/chauffeur";

    private final TourneeService tournees;
    private final UtilisateurService utilisateurs;

    public ChauffeurWebController(TourneeService tournees, UtilisateurService utilisateurs) {
        this.tournees = tournees;
        this.utilisateurs = utilisateurs;
    }

    @GetMapping
    public String tourneeCourante(Model model) {
        Tournee tournee;
        try {
            tournee = tournees.courante();
        } catch (RessourceIntrouvableException aucune) {
            model.addAttribute("tournee", null);
            return "chauffeur/tournee";
        }
        boolean aDesColis = !tournee.getColis().isEmpty();
        model.addAttribute("tournee", tournee);
        model.addAttribute("estimations", aDesColis ? tournees.estimer(tournee.getId()) : List.of());
        model.addAttribute("nbOrdresPossibles", aDesColis ? tournees.nombreOrdresPossibles(tournee.getId()) : 0);
        model.addAttribute("conflits", aDesColis ? tournees.conflitsDeMasses(tournee.getId()) : List.of());
        model.addAttribute("carte", CarteTournee.donnees(tournee));
        model.addAttribute("chargementHautEnBas", CarteTournee.chargementHautEnBas(tournee));
        model.addAttribute("etatsBon", EtatBon.values());
        return "chauffeur/tournee";
    }

    @PostMapping("/tournees/{id}/calcul")
    public String calculer(@PathVariable Long id, @RequestParam Algorithme algorithme, RedirectAttributes redirection) {
        return executer(redirection, () -> {
            Tournee t = tournees.calculer(id, algorithme);
            return String.format(Locale.FRANCE, "Boucle calculée : %.1f km.", t.getDistanceTotaleKm());
        });
    }

    /** Échange un arrêt avec son voisin (sens -1 : plus tôt, +1 : plus tard) ; seuls les arrêts de masses identiques le peuvent. */
    @PostMapping("/tournees/{id}/arrets/{ordre}/deplacement")
    public String deplacer(@PathVariable Long id, @PathVariable int ordre, @RequestParam int sens,
                           RedirectAttributes redirection) {
        return executer(redirection, () -> {
            Tournee tournee = tournees.trouver(id);
            List<Long> clients = new ArrayList<>(tournee.getArrets().stream().map(a -> a.getClient().getId()).toList());
            int i = ordre - 1;
            int j = i + Integer.signum(sens);
            if (i < 0 || j < 0 || i >= clients.size() || j >= clients.size()) {
                throw new RegleMetierException("Cet arrêt ne peut pas être déplacé dans ce sens.");
            }
            Collections.swap(clients, i, j);
            tournees.ordreManuel(id, clients);
            return "Ordre de passage modifié.";
        });
    }

    @PostMapping("/tournees/{id}/validation")
    public String valider(@PathVariable Long id, Principal principal, RedirectAttributes redirection) {
        return executer(redirection, () -> {
            tournees.valider(id, connecte(principal));
            return "Boucle et chargement validés : bonne route !";
        });
    }

    @PostMapping("/tournees/{id}/bons")
    public String enregistrerBon(@PathVariable Long id, @RequestParam Long clientId,
                                 @RequestParam(required = false) List<Long> colisIds, @RequestParam EtatBon etat,
                                 @RequestParam(required = false) String commentaire, Principal principal,
                                 RedirectAttributes redirection) {
        return executer(redirection, () -> {
            if (etat != EtatBon.REFUSE && (colisIds == null || colisIds.isEmpty())) {
                throw new RegleMetierException("Cochez les colis remis au client (ou choisissez « Refusé »).");
            }
            BonLivraison bon = tournees.enregistrerBon(id, clientId, colisIds, etat, commentaire, connecte(principal));
            return "Bon " + bon.getNumero() + " enregistré (" + bon.getEtat().getLibelle().toLowerCase() + ").";
        });
    }

    @PostMapping("/tournees/{id}/fin")
    public String terminer(@PathVariable Long id, RedirectAttributes redirection) {
        return executer(redirection, () -> {
            Tournee t = tournees.terminer(id);
            long nonLivres = t.getArrets().stream().map(Arret::getClient).filter(c -> t.bonDuClient(c).isEmpty()).count();
            return nonLivres == 0 ? "Tournée terminée."
                    : "Tournée terminée : les colis de " + nonLivres + " client(s) non livré(s) sont reportés au jeudi suivant.";
        });
    }

    private Utilisateur connecte(Principal principal) {
        return utilisateurs.parIdentifiant(principal.getName())
                .orElseThrow(() -> new RegleMetierException("Utilisateur connecté inconnu."));
    }

    private static String executer(RedirectAttributes redirection, Action action) {
        try {
            redirection.addFlashAttribute("succes", action.executer());
        } catch (RegleMetierException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
        }
        return RETOUR;
    }

    @FunctionalInterface
    private interface Action {
        String executer();
    }
}
