package com.tgv.tgv_workfloor.depot;

import com.tgv.tgv_workfloor.modele.Client;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

@Repository
public class ClientDepot extends DepotMemoire<Client> {

    public List<Client> findAllByOrderByNom() {
        return findAll().stream().sorted(Comparator.comparing(Client::getNom, String.CASE_INSENSITIVE_ORDER)).toList();
    }
}
