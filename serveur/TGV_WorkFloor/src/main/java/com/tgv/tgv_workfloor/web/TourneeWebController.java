package com.tgv.tgv_workfloor.web;

import com.tgv.tgv_workfloor.exception.RegleMetierException;
import com.tgv.tgv_workfloor.modele.Algorithme;
import com.tgv.tgv_workfloor.modele.StatutTournee;
import com.tgv.tgv_workfloor.modele.Tournee;
import com.tgv.tgv_workfloor.service.PlanificationLivraisons;
import com.tgv.tgv_workfloor.service.TourneeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

/** Consultation des boucles de livraison (trajet, kilomètres) et préparation des tournées. */
@Controller
@RequestMapping("/atelier/tournees")
public class TourneeWebController {

    private final TourneeService tournees;

    public TourneeWebController(TourneeService tournees) {
        this.tournees = tournees;
    }

    @GetMapping
    public String liste(Model model) {
        LocalDate prochaineTournee = PlanificationLivraisons.lendemainOuvre(LocalDate.now());
        model.addAttribute("tournees", tournees.lister());
        model.addAttribute("datePreparation", prochaineTournee);
        model.addAttribute("nbColisDisponibles", tournees.colisDisponibles(prochaineTournee).size());
        model.addAttribute("colisEnAttente", tournees.colisEnAttente());
        model.addAttribute("kmTotal", tournees.lister().stream()
                .filter(t -> t.getStatut() == StatutTournee.TERMINEE).mapToDouble(Tournee::getDistanceTotaleKm).sum());
        return "atelier/tournees/liste";
    }

    @PostMapping("/preparation")
    public String preparer(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           RedirectAttributes redirection) {
        try {
            Tournee t = tournees.preparer(date);
            redirection.addFlashAttribute("succes", "Tournée du " + date + " prête : " + t.getColis().size()
                    + " colis. Calculez la boucle.");
            return "redirect:/atelier/tournees/" + t.getId();
        } catch (RegleMetierException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/atelier/tournees";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Tournee tournee = tournees.trouver(id);
        model.addAttribute("tournee", tournee);
        model.addAttribute("estimations", tournee.getColis().isEmpty() ? List.of() : tournees.estimer(id));
        model.addAttribute("nbOrdresPossibles", tournee.getColis().isEmpty() ? 0 : tournees.nombreOrdresPossibles(id));
        model.addAttribute("conflits", tournee.getColis().isEmpty() ? List.of() : tournees.conflitsDeMasses(id));
        model.addAttribute("carte", CarteTournee.donnees(tournee));
        model.addAttribute("chargementHautEnBas", CarteTournee.chargementHautEnBas(tournee));
        return "atelier/tournees/detail";
    }

    @PostMapping("/{id}/calcul")
    public String calculer(@PathVariable Long id, @RequestParam Algorithme algorithme,
                           RedirectAttributes redirection) {
        try {
            Tournee t = tournees.calculer(id, algorithme);
            redirection.addFlashAttribute("succes", String.format(java.util.Locale.FRANCE,
                    "Boucle calculée (%s) : %.1f km en %d ms.", algorithme.getLibelle(), t.getDistanceTotaleKm(),
                    t.getDureeCalculMs()));
        } catch (RegleMetierException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/atelier/tournees/" + id;
    }

    @PostMapping("/{id}/colis/{colisId}/retrait")
    public String retirerColis(@PathVariable Long id, @PathVariable Long colisId, RedirectAttributes redirection) {
        try {
            tournees.retirerColis(id, colisId);
            redirection.addFlashAttribute("succes", "Colis retiré de la tournée : recalculez la boucle.");
        } catch (RegleMetierException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/atelier/tournees/" + id;
    }

    @PostMapping("/{id}/fin")
    public String terminer(@PathVariable Long id, RedirectAttributes redirection) {
        try {
            tournees.terminer(id);
            redirection.addFlashAttribute("succes", "Tournée terminée.");
        } catch (RegleMetierException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/atelier/tournees/" + id;
    }

    @PostMapping("/{id}/suppression")
    public String supprimer(@PathVariable Long id, RedirectAttributes redirection) {
        try {
            tournees.supprimer(id);
            redirection.addFlashAttribute("succes", "Tournée supprimée : ses colis sont de nouveau disponibles.");
            return "redirect:/atelier/tournees";
        } catch (RegleMetierException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/atelier/tournees/" + id;
        }
    }
}
