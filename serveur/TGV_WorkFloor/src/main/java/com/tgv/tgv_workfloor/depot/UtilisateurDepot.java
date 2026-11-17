package com.tgv.tgv_workfloor.depot;

import com.tgv.tgv_workfloor.modele.Utilisateur;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UtilisateurDepot extends DepotMemoire<Utilisateur> {

    public Optional<Utilisateur> findByIdentifiant(String identifiant) {
        return filtrer(u -> u.getIdentifiant().equalsIgnoreCase(identifiant)).stream().findFirst();
    }
}
