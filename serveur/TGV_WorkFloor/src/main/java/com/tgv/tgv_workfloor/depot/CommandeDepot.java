package com.tgv.tgv_workfloor.depot;

import com.tgv.tgv_workfloor.modele.Client;
import com.tgv.tgv_workfloor.modele.Commande;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CommandeDepot extends DepotMemoire<Commande> {

    public List<Commande> findByClient(Client client) {
        return filtrer(c -> c.getClient().getId().equals(client.getId()));
    }
}
