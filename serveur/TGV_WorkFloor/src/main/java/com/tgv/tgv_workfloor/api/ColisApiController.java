package com.tgv.tgv_workfloor.api;

import com.tgv.tgv_workfloor.service.CommandeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Détail d'un colis : pièces emballées et client (écran « Colis C-0412 »). */
@RestController
@RequestMapping("/api/colis")
public class ColisApiController {

    private final CommandeService commandes;

    public ColisApiController(CommandeService commandes) {
        this.commandes = commandes;
    }

    @GetMapping("/{id}")
    public Dto.ColisDetail detail(@PathVariable Long id) {
        return ApiMapper.colisDetail(commandes.trouverColis(id));
    }
}
