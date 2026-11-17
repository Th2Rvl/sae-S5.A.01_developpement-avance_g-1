package com.tgv.tgv_workfloor.optimisation;

import com.tgv.tgv_workfloor.modele.Colis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ordre de chargement déduit de la boucle : le dernier client livré est chargé en premier (en bas),
 * le premier client livré en dernier (en haut). Pour un même client, le colis le plus lourd va dessous.
 * Si la boucle respecte les précédences de {@link ContraintesMasses}, la pile obtenue est rangée par
 * masses décroissantes du bas vers le haut.
 */
public final class PlanChargement {

    private PlanChargement() {
    }

    /**
     * @param colisParArret colis de chaque arrêt, dans l'ordre de livraison
     * @return colis du bas vers le haut du plateau
     */
    public static List<Colis> calculer(List<List<Colis>> colisParArret) {
        List<Colis> basVersHaut = new ArrayList<>();
        for (int i = colisParArret.size() - 1; i >= 0; i--) {
            colisParArret.get(i).stream()
                    .sorted(Comparator.comparingDouble(Colis::getPoidsKg).reversed())
                    .forEach(basVersHaut::add);
        }
        return basVersHaut;
    }

    /** Vrai si aucun colis n'est posé sur un colis plus léger. */
    public static boolean respecteOrdreDesMasses(List<Colis> basVersHaut) {
        for (int i = 1; i < basVersHaut.size(); i++) {
            if (basVersHaut.get(i).getPoidsKg() > basVersHaut.get(i - 1).getPoidsKg() + 1e-9) {
                return false;
            }
        }
        return true;
    }
}
