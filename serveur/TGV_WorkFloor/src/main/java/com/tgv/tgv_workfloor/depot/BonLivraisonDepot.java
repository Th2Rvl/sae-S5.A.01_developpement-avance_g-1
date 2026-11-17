package com.tgv.tgv_workfloor.depot;

import com.tgv.tgv_workfloor.modele.BonLivraison;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

@Repository
public class BonLivraisonDepot extends DepotMemoire<BonLivraison> {

    /** Bons du plus récent au plus ancien. */
    public List<BonLivraison> findAllByOrderByDateHeureDesc() {
        return findAll().stream().sorted(Comparator.comparing(BonLivraison::getDateHeure).reversed()).toList();
    }
}
