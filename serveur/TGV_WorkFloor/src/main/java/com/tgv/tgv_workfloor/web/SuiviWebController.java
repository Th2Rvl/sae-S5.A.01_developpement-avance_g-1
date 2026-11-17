package com.tgv.tgv_workfloor.web;

import com.tgv.tgv_workfloor.exception.RegleMetierException;
import com.tgv.tgv_workfloor.modele.Role;
import com.tgv.tgv_workfloor.service.TourneeService;
import com.tgv.tgv_workfloor.service.UtilisateurService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/** Suivi des bons de livraison et gestion des comptes (atelier, chauffeurs). */
@Controller
public class SuiviWebController {

    private final TourneeService tournees;
    private final UtilisateurService utilisateurs;

    public SuiviWebController(TourneeService tournees, UtilisateurService utilisateurs) {
        this.tournees = tournees;
        this.utilisateurs = utilisateurs;
    }

    @GetMapping("/atelier/bons")
    public String bons(Model model) {
        model.addAttribute("bons", tournees.listerBons());
        return "atelier/bons";
    }

    @GetMapping("/atelier/utilisateurs")
    public String utilisateurs(Model model) {
        model.addAttribute("utilisateurs", utilisateurs.lister());
        model.addAttribute("roles", Role.values());
        return "atelier/utilisateurs";
    }

    @PostMapping("/atelier/utilisateurs")
    public String creer(@RequestParam String identifiant, @RequestParam(required = false) String nom,
                        @RequestParam String motDePasse, @RequestParam Role role, RedirectAttributes redirection) {
        try {
            utilisateurs.creer(identifiant, nom, motDePasse, role);
            redirection.addFlashAttribute("succes", "Compte « " + identifiant.trim() + " » créé.");
        } catch (RegleMetierException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/atelier/utilisateurs";
    }

    @PostMapping("/atelier/utilisateurs/{id}/suppression")
    public String supprimer(@PathVariable Long id, Principal principal, RedirectAttributes redirection) {
        try {
            utilisateurs.supprimer(id, principal.getName());
            redirection.addFlashAttribute("succes", "Compte supprimé.");
        } catch (RegleMetierException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/atelier/utilisateurs";
    }
}
