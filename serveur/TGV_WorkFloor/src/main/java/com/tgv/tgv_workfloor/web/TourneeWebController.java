package com.tgv.tgv_workfloor.web;

import com.tgv.tgv_workfloor.exception.RegleMetierException;
import com.tgv.tgv_workfloor.modele.Algorithme;
import com.tgv.tgv_workfloor.modele.Arret;
import com.tgv.tgv_workfloor.modele.StatutTournee;
import com.tgv.tgv_workfloor.modele.Tournee;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        LocalDate prochainJeudi = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY));
        model.addAttribute("tournees", tournees.lister());
        model.addAttribute("datePreparation", prochainJeudi);
        model.addAttribute("nbColisDisponibles", tournees.colisDisponibles(prochainJeudi).size());
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
        model.addAttribute("carte", donneesCarte(tournee));
        model.addAttribute("chargementHautEnBas", inverse(tournee));
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

    /** Points affichés par Leaflet : dépôt, puis clients (numérotés si la boucle est calculée). */
    private static Map<String, Object> donneesCarte(Tournee t) {
        Map<String, Object> carte = new LinkedHashMap<>();
        carte.put("depot", Map.of("nom", t.getAtelier().getNom(), "lat", t.getAtelier().getLatitude(),
                "lon", t.getAtelier().getLongitude()));
        List<Map<String, Object>> points = new ArrayList<>();
        if (t.isBoucleCalculee()) {
            for (Arret a : t.getArrets()) {
                points.add(point(a.getOrdre(), a.getClient().getNom(), a.getClient().getVille(),
                        a.getClient().getLatitude(), a.getClient().getLongitude(), t.bonDuClient(a.getClient()).isPresent()));
            }
        } else {
            t.getClients().forEach(c -> points.add(point(0, c.getNom(), c.getVille(), c.getLatitude(),
                    c.getLongitude(), false)));
        }
        carte.put("arrets", points);
        carte.put("boucle", t.isBoucleCalculee());
        return carte;
    }

    private static Map<String, Object> point(int ordre, String nom, String ville, double lat, double lon,
                                             boolean livre) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("ordre", ordre);
        p.put("nom", nom);
        p.put("ville", ville);
        p.put("lat", lat);
        p.put("lon", lon);
        p.put("livre", livre);
        return p;
    }

    private static List<Object[]> inverse(Tournee t) {
        List<Object[]> lignes = new ArrayList<>();
        for (int i = t.getChargement().size() - 1; i >= 0; i--) {
            lignes.add(new Object[]{i + 1, t.getChargement().get(i)});
        }
        return lignes;
    }
}
