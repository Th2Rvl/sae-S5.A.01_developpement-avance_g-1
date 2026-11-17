package com.tgv.tgv_workfloor.depot;

import com.tgv.tgv_workfloor.modele.Colis;
import com.tgv.tgv_workfloor.modele.StatutColis;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ColisDepot extends DepotMemoire<Colis> {

    public List<Colis> findByStatut(StatutColis statut) {
        return filtrer(c -> c.getStatut() == statut);
    }
}
