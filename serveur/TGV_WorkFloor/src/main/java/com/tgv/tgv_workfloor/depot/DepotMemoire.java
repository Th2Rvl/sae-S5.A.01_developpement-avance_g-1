package com.tgv.tgv_workfloor.depot;

import com.tgv.tgv_workfloor.modele.Identifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * Stockage statique en mémoire, en attendant la base MySQL.
 * Les noms de méthodes reprennent ceux de Spring Data (save, findById, findAll, deleteById, count)
 * pour que le passage à des interfaces JpaRepository ne change pas les services.
 * Les données sont perdues à chaque redémarrage.
 */
public abstract class DepotMemoire<T extends Identifiable> {

    private final Map<Long, T> donnees = new ConcurrentSkipListMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public T save(T entite) {
        if (entite.getId() == null) {
            entite.setId(sequence.incrementAndGet());
        }
        donnees.put(entite.getId(), entite);
        return entite;
    }

    public Optional<T> findById(Long id) {
        return id == null ? Optional.empty() : Optional.ofNullable(donnees.get(id));
    }

    /** Toutes les entités, triées par identifiant. */
    public List<T> findAll() {
        return new ArrayList<>(donnees.values());
    }

    public void deleteById(Long id) {
        donnees.remove(id);
    }

    public void delete(T entite) {
        deleteById(entite.getId());
    }

    public long count() {
        return donnees.size();
    }

    public void deleteAll() {
        donnees.clear();
        sequence.set(0);
    }

    protected List<T> filtrer(Predicate<T> critere) {
        return donnees.values().stream().filter(critere).toList();
    }
}
