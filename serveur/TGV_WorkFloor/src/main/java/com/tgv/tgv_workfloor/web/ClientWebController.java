package com.tgv.tgv_workfloor.web;

import com.tgv.tgv_workfloor.exception.RegleMetierException;
import com.tgv.tgv_workfloor.modele.Client;
import com.tgv.tgv_workfloor.service.ClientService;
import com.tgv.tgv_workfloor.web.formulaire.ClientFormulaire;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

/** Ajout, modification et suppression des clients. */
@Controller
@RequestMapping("/atelier/clients")
public class ClientWebController {

    private final ClientService clients;

    public ClientWebController(ClientService clients) {
        this.clients = clients;
    }

    @GetMapping
    public String liste(Model model) {
        Map<Client, Integer> commandesParClient = new LinkedHashMap<>();
        clients.lister().forEach(c -> commandesParClient.put(c, clients.nombreDeCommandes(c)));
        model.addAttribute("clients", commandesParClient);
        return "atelier/clients/liste";
    }

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("formulaire", new ClientFormulaire());
        model.addAttribute("clientId", null);
        return "atelier/clients/formulaire";
    }

    @PostMapping("/nouveau")
    public String creer(@Valid @ModelAttribute("formulaire") ClientFormulaire formulaire, BindingResult erreurs,
                        Model model, RedirectAttributes redirection) {
        if (erreurs.hasErrors()) {
            model.addAttribute("clientId", null);
            return "atelier/clients/formulaire";
        }
        Client client = clients.creer(formulaire.versClient());
        redirection.addFlashAttribute("succes", "Client « " + client.getNom() + " » ajouté.");
        return "redirect:/atelier/clients";
    }

    @GetMapping("/{id}")
    public String modification(@PathVariable Long id, Model model) {
        model.addAttribute("formulaire", ClientFormulaire.depuis(clients.trouver(id)));
        model.addAttribute("clientId", id);
        return "atelier/clients/formulaire";
    }

    @PostMapping("/{id}")
    public String modifier(@PathVariable Long id, @Valid @ModelAttribute("formulaire") ClientFormulaire formulaire,
                           BindingResult erreurs, Model model, RedirectAttributes redirection) {
        if (erreurs.hasErrors()) {
            model.addAttribute("clientId", id);
            return "atelier/clients/formulaire";
        }
        Client client = clients.modifier(id, formulaire.versClient());
        redirection.addFlashAttribute("succes", "Client « " + client.getNom() + " » modifié.");
        return "redirect:/atelier/clients";
    }

    @PostMapping("/{id}/suppression")
    public String supprimer(@PathVariable Long id, RedirectAttributes redirection) {
        try {
            String nom = clients.trouver(id).getNom();
            clients.supprimer(id);
            redirection.addFlashAttribute("succes", "Client « " + nom + " » supprimé.");
        } catch (RegleMetierException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/atelier/clients";
    }
}
