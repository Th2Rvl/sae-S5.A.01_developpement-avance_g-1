package com.tgv.tgv_workfloor.web;

import com.tgv.tgv_workfloor.exception.RegleMetierException;
import com.tgv.tgv_workfloor.exception.RessourceIntrouvableException;
import com.tgv.tgv_workfloor.modele.Algorithme;
import com.tgv.tgv_workfloor.modele.Arret;
import com.tgv.tgv_workfloor.modele.Client;
import com.tgv.tgv_workfloor.modele.Colis;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Espace chauffeur du site, calqué sur les maquettes « 1 · Préparer la tournée » :
 * tournée du jour (onglets Colis, Carte, Boucle), détail d'un colis, calcul de la boucle, ordre manuel
 * et validation de la boucle. Mêmes règles métier que l'API de l'application Android.
 */
@Controller
@RequestMapping("/chauffeur")
public class ChauffeurWebController {

    private final TourneeService tournees;
    private final UtilisateurService utilisateurs;

    public ChauffeurWebController(TourneeService tournees, UtilisateurService utilisateurs) {
        this.tournees = tournees;
        this.utilisateurs = utilisateurs;
    }

    /** Maquette 02 · onglet Colis (et page d'arrivée, y compris quand aucune tournée n'est à effectuer). */
    @GetMapping
    public String colis(@RequestParam(defaultValue = "client") String tri, Model model) {
        Optional<Tournee> courante = courante();
        model.addAttribute("tournee", courante.orElse(null));
        if (courante.isEmpty()) {
            return "chauffeur/colis";
        }
        Tournee tournee = courante.get();
        boolean parOrdre = "ordre".equals(tri) && tournee.isBoucleCalculee();
        List<Client> clients = parOrdre ? tournee.getArrets().stream().map(Arret::getClient).toList()
                : tournee.getClients();
        List<Colis> colis = new ArrayList<>();
        clients.forEach(c -> colis.addAll(tournee.colisDuClient(c)));
        model.addAttribute("colis", colis);
        model.addAttribute("tri", parOrdre ? "ordre" : "client");
        return "chauffeur/colis";
    }

    /** Maquette 03 · détail d'un colis de la tournée. */
    @GetMapping("/colis/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes redirection) {
        Optional<Tournee> courante = courante();
        Optional<Colis> colis = courante.flatMap(t -> t.getColis().stream().filter(c -> c.getId().equals(id)).findFirst());
        if (colis.isEmpty()) {
            redirection.addFlashAttribute("erreur", "Ce colis ne fait pas partie de la tournée du jour.");
            return "redirect:/chauffeur";
        }
        model.addAttribute("colis", colis.get());
        model.addAttribute("arret", courante.get().arretDuClient(colis.get().getClient()).orElse(null));
        return "chauffeur/detail";
    }

    /** Maquette 04 · onglet Carte ; {@code client} centre la carte sur ce client (« Voir sur la carte »). */
    @GetMapping("/carte")
    public String carte(@RequestParam(required = false) Long client, Model model) {
        Optional<Tournee> courante = courante();
        if (courante.isEmpty()) {
            return "redirect:/chauffeur";
        }
        model.addAttribute("tournee", courante.get());
        model.addAttribute("carte", CarteTournee.donnees(courante.get(), client));
        return "chauffeur/carte";
    }

    /** Maquettes 05 et 06 · onglet Boucle et dialogue « Calculer la boucle ». */
    @GetMapping("/boucle")
    public String boucle(Model model) {
        Optional<Tournee> courante = courante();
        if (courante.isEmpty()) {
            return "redirect:/chauffeur";
        }
        Tournee tournee = courante.get();
        boolean aDesColis = !tournee.getColis().isEmpty();
        model.addAttribute("tournee", tournee);
        model.addAttribute("estimations", aDesColis ? tournees.estimer(tournee.getId()) : List.of());
        model.addAttribute("conflits", aDesColis ? tournees.conflitsDeMasses(tournee.getId()) : List.of());
        return "chauffeur/boucle";
    }

    /** Maquette 07 · ordre manuel, à partir de la boucle calculée (ou des clients si elle ne l'est pas encore). */
    @GetMapping("/ordre")
    public String ordre(Model model) {
        Optional<Tournee> courante = courante();
        if (courante.isEmpty()) {
            return "redirect:/chauffeur";
        }
        Tournee tournee = courante.get();
        if (tournee.getStatut().estFigee()) {
            return "redirect:/chauffeur/boucle";
        }
        model.addAttribute("tournee", tournee);
        model.addAttribute("clients", tournee.isBoucleCalculee()
                ? tournee.getArrets().stream().map(Arret::getClient).toList() : tournee.getClients());
        return "chauffeur/ordre";
    }

    @PostMapping("/tournees/{id}/calcul")
    public String calculer(@PathVariable Long id, @RequestParam Algorithme algorithme, RedirectAttributes redirection) {
        return executer(redirection, "redirect:/chauffeur/boucle", "redirect:/chauffeur/boucle", () -> {
            Tournee t = tournees.calculer(id, algorithme);
            return String.format(Locale.FRANCE, "Boucle calculée : %.1f km.", t.getDistanceTotaleKm());
        });
    }

    @PostMapping("/tournees/{id}/ordre")
    public String enregistrerOrdre(@PathVariable Long id, @RequestParam List<Long> clientIds,
                                   RedirectAttributes redirection) {
        return executer(redirection, "redirect:/chauffeur/boucle", "redirect:/chauffeur/ordre", () -> {
            tournees.ordreManuel(id, clientIds);
            return "Ordre de passage enregistré.";
        });
    }

    @PostMapping("/tournees/{id}/validation")
    public String valider(@PathVariable Long id, Principal principal, RedirectAttributes redirection) {
        return executer(redirection, "redirect:/chauffeur/boucle", "redirect:/chauffeur/boucle", () -> {
            Utilisateur chauffeur = utilisateurs.parIdentifiant(principal.getName())
                    .orElseThrow(() -> new RegleMetierException("Utilisateur connecté inconnu."));
            tournees.valider(id, chauffeur);
            return "Boucle validée.";
        });
    }

    private Optional<Tournee> courante() {
        try {
            return Optional.of(tournees.courante());
        } catch (RessourceIntrouvableException aucune) {
            return Optional.empty();
        }
    }

    private static String executer(RedirectAttributes redirection, String siSucces, String siErreur,
                                   Supplier<String> action) {
        try {
            redirection.addFlashAttribute("succes", action.get());
            return siSucces;
        } catch (RegleMetierException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
            return siErreur;
        }
    }
}
