package com.tgv.tgv_workfloor.depot;

import com.tgv.tgv_workfloor.modele.Tournee;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
public class TourneeDepot extends DepotMemoire<Tournee> {

    public Optional<Tournee> findByDate(LocalDate date) {
        return filtrer(t -> t.getDate().equals(date)).stream().findFirst();
    }

    /** Tournées de la plus récente à la plus ancienne. */
    public List<Tournee> findAllByOrderByDateDesc() {
        return findAll().stream().sorted(Comparator.comparing(Tournee::getDate).reversed()).toList();
    }
}
