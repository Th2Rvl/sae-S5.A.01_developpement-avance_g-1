package com.tgv.tgv_workfloor.web;

import com.tgv.tgv_workfloor.exception.RessourceIntrouvableException;
import com.tgv.tgv_workfloor.modele.Commande;
import com.tgv.tgv_workfloor.modele.EtatPiece;
import com.tgv.tgv_workfloor.service.ClientService;
import com.tgv.tgv_workfloor.service.CommandeService;
import com.tgv.tgv_workfloor.service.TourneeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;

/** Tableau de bord de l'atelier. */
@Controller
public class AccueilController {

    private final ClientService clients;
    private final CommandeService commandes;
    private final TourneeService tournees;

    public AccueilController(ClientService clients, CommandeService commandes, TourneeService tournees) {
        this.clients = clients;
        this.commandes = commandes;
        this.tournees = tournees;
    }

    @GetMapping("/atelier")
    public String accueil(Model model) {
        List<Commande> toutes = commandes.lister(null);
        List<Commande> enCours = toutes.stream().filter(c -> c.getEtat() != EtatPiece.LIVREE).toList();
        model.addAttribute("nbClients", clients.lister().size());
        model.addAttribute("nbCommandesEnCours", enCours.size());
        model.addAttribute("nbPiecesAFabriquer", toutes.stream().flatMap(c -> c.getPieces().stream())
                .filter(p -> p.getEtat() == EtatPiece.A_FAIRE || p.getEtat() == EtatPiece.EN_FABRICATION).count());
        model.addAttribute("nbPiecesAEmballer", toutes.stream().mapToLong(c -> c.getPiecesAEmballer().size()).sum());
        model.addAttribute("nbColisEnAttente", tournees.colisDisponibles(LocalDate.now().plusYears(10)).size());
        model.addAttribute("commandesEnCours", enCours.stream().limit(8).toList());
        try {
            model.addAttribute("tournee", tournees.courante());
        } catch (RessourceIntrouvableException aucune) {
            model.addAttribute("tournee", null);
        }
        model.addAttribute("derniersBons", tournees.listerBons().stream().limit(5).toList());
        return "atelier/accueil";
    }
}
