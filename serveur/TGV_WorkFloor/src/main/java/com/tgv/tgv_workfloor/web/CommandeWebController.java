package com.tgv.tgv_workfloor.web;

import com.tgv.tgv_workfloor.exception.RegleMetierException;
import com.tgv.tgv_workfloor.modele.Commande;
import com.tgv.tgv_workfloor.modele.EtatPiece;
import com.tgv.tgv_workfloor.modele.Piece;
import com.tgv.tgv_workfloor.service.ClientService;
import com.tgv.tgv_workfloor.service.CommandeService;
import com.tgv.tgv_workfloor.web.formulaire.ColisFormulaire;
import com.tgv.tgv_workfloor.web.formulaire.CommandeFormulaire;
import com.tgv.tgv_workfloor.web.formulaire.PieceFormulaire;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** Saisie et suivi des commandes : pièces, états de fabrication, emballage en colis. */
@Controller
@RequestMapping("/atelier/commandes")
public class CommandeWebController {

    private final CommandeService commandes;
    private final ClientService clients;

    public CommandeWebController(CommandeService commandes, ClientService clients) {
        this.commandes = commandes;
        this.clients = clients;
    }

    @ModelAttribute("etats")
    EtatPiece[] etats() {
        return EtatPiece.values();
    }

    @GetMapping
    public String liste(@RequestParam(required = false) EtatPiece etat, Model model) {
        model.addAttribute("commandes", commandes.lister(etat));
        model.addAttribute("etatFiltre", etat);
        return "atelier/commandes/liste";
    }

    // ------------------------------------------------------------------ commande

    @GetMapping("/nouvelle")
    public String nouvelle(@RequestParam(required = false) Long clientId, Model model) {
        CommandeFormulaire f = new CommandeFormulaire();
        f.setClientId(clientId);
        f.setDatePrevue(LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.THURSDAY)));
        return formulaire(model, f, null);
    }

    @PostMapping("/nouvelle")
    public String creer(@Valid @ModelAttribute("formulaire") CommandeFormulaire f, BindingResult erreurs, Model model,
                        RedirectAttributes redirection) {
        if (erreurs.hasErrors()) {
            return formulaire(model, f, null);
        }
        Commande commande = commandes.creer(f.getClientId(), f.getDatePrevue());
        redirection.addFlashAttribute("succes", "Commande " + commande.getReference() + " créée : ajoutez ses pièces.");
        return "redirect:/atelier/commandes/" + commande.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("commande", commandes.trouver(id));
        if (!model.containsAttribute("piece")) {
            model.addAttribute("piece", new PieceFormulaire());
        }
        model.addAttribute("colisFormulaire", new ColisFormulaire());
        return "atelier/commandes/detail";
    }

    @GetMapping("/{id}/modification")
    public String modification(@PathVariable Long id, Model model) {
        Commande commande = commandes.trouver(id);
        CommandeFormulaire f = new CommandeFormulaire();
        f.setClientId(commande.getClient().getId());
        f.setDatePrevue(commande.getDatePrevue());
        return formulaire(model, f, commande);
    }

    @PostMapping("/{id}/modification")
    public String modifier(@PathVariable Long id, @Valid @ModelAttribute("formulaire") CommandeFormulaire f,
                           BindingResult erreurs, Model model, RedirectAttributes redirection) {
        if (erreurs.hasErrors()) {
            return formulaire(model, f, commandes.trouver(id));
        }
        try {
            commandes.modifier(id, f.getClientId(), f.getDatePrevue());
            redirection.addFlashAttribute("succes", "Commande modifiée.");
        } catch (RegleMetierException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/atelier/commandes/" + id;
    }

    @PostMapping("/{id}/suppression")
    public String supprimer(@PathVariable Long id, RedirectAttributes redirection) {
        try {
            String reference = commandes.trouver(id).getReference();
            commandes.supprimer(id);
            redirection.addFlashAttribute("succes", "Commande " + reference + " supprimée.");
            return "redirect:/atelier/commandes";
        } catch (RegleMetierException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/atelier/commandes/" + id;
        }
    }

    // ------------------------------------------------------------------ pièces

    @PostMapping("/{id}/pieces")
    public String ajouterPiece(@PathVariable Long id, @Valid @ModelAttribute("piece") PieceFormulaire f,
                               BindingResult erreurs, Model model, RedirectAttributes redirection) {
        if (erreurs.hasErrors()) {
            model.addAttribute("commande", commandes.trouver(id));
            model.addAttribute("colisFormulaire", new ColisFormulaire());
            return "atelier/commandes/detail";
        }
        Piece p = commandes.ajouterPiece(id, f.getDesignation().trim(), f.getPoidsKg(), f.getEtat());
        redirection.addFlashAttribute("succes", "Pièce " + p.getReference() + " ajoutée.");
        return "redirect:/atelier/commandes/" + id;
    }

    @PostMapping("/pieces/{pieceId}/etat")
    public String changerEtat(@PathVariable Long pieceId, @RequestParam EtatPiece etat,
                              RedirectAttributes redirection) {
        Piece piece = commandes.trouverPiece(pieceId);
        try {
            commandes.changerEtat(pieceId, etat);
            redirection.addFlashAttribute("succes", "Pièce " + piece.getReference() + " : " + etat.getLibelle() + ".");
        } catch (RegleMetierException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/atelier/commandes/" + piece.getCommande().getId();
    }

    @GetMapping("/pieces/{pieceId}")
    public String modificationPiece(@PathVariable Long pieceId, Model model) {
        Piece piece = commandes.trouverPiece(pieceId);
        PieceFormulaire f = new PieceFormulaire();
        f.setDesignation(piece.getDesignation());
        f.setPoidsKg(piece.getPoidsKg());
        f.setEtat(piece.getEtat());
        model.addAttribute("pieceModifiee", piece);
        model.addAttribute("piece", f);
        return "atelier/commandes/piece";
    }

    @PostMapping("/pieces/{pieceId}")
    public String modifierPiece(@PathVariable Long pieceId, @Valid @ModelAttribute("piece") PieceFormulaire f,
                                BindingResult erreurs, Model model, RedirectAttributes redirection) {
        Piece piece = commandes.trouverPiece(pieceId);
        if (erreurs.hasErrors()) {
            model.addAttribute("pieceModifiee", piece);
            return "atelier/commandes/piece";
        }
        try {
            commandes.modifierPiece(pieceId, f.getDesignation().trim(), f.getPoidsKg());
            redirection.addFlashAttribute("succes", "Pièce " + piece.getReference() + " modifiée.");
        } catch (RegleMetierException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/atelier/commandes/" + piece.getCommande().getId();
    }

    @PostMapping("/pieces/{pieceId}/suppression")
    public String supprimerPiece(@PathVariable Long pieceId, RedirectAttributes redirection) {
        Piece piece = commandes.trouverPiece(pieceId);
        try {
            commandes.supprimerPiece(pieceId);
            redirection.addFlashAttribute("succes", "Pièce " + piece.getReference() + " supprimée.");
        } catch (RegleMetierException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/atelier/commandes/" + piece.getCommande().getId();
    }

    // ------------------------------------------------------------------ colis

    @PostMapping("/{id}/colis")
    public String emballer(@PathVariable Long id, @ModelAttribute ColisFormulaire f, RedirectAttributes redirection) {
        try {
            var colis = commandes.emballer(id, f.getPieceIds(), f.getLongueurCm(), f.getLargeurCm(), f.getHauteurCm());
            redirection.addFlashAttribute("succes", "Colis " + colis.getReference() + " créé ("
                    + colis.getPieces().size() + " pièce(s)).");
        } catch (RegleMetierException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/atelier/commandes/" + id;
    }

    @PostMapping("/colis/{colisId}/suppression")
    public String supprimerColis(@PathVariable Long colisId, RedirectAttributes redirection) {
        var colis = commandes.trouverColis(colisId);
        Long commandeId = colis.getCommande().getId();
        try {
            commandes.supprimerColis(colisId);
            redirection.addFlashAttribute("succes", "Colis " + colis.getReference() + " défait : ses pièces sont "
                    + "de nouveau à emballer.");
        } catch (RegleMetierException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/atelier/commandes/" + commandeId;
    }

    private String formulaire(Model model, CommandeFormulaire f, Commande commande) {
        model.addAttribute("formulaire", f);
        model.addAttribute("commande", commande);
        model.addAttribute("clients", clients.lister());
        return "atelier/commandes/formulaire";
    }
}
