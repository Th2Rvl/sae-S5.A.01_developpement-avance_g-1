package com.tgv.tgv_workfloor.web;

import com.tgv.tgv_workfloor.modele.Arret;
import com.tgv.tgv_workfloor.modele.Tournee;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Données d'affichage d'une tournée partagées par les pages Atelier et Chauffeur (carte Leaflet, chargement). */
public final class CarteTournee {

    private CarteTournee() {
    }

    /** Points affichés par Leaflet : dépôt, puis clients (numérotés si la boucle est calculée). */
    public static Map<String, Object> donnees(Tournee t) {
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

    /** Plan de chargement lu du haut du plateau (déchargé en premier) vers le bas : [position, colis]. */
    public static List<Object[]> chargementHautEnBas(Tournee t) {
        List<Object[]> lignes = new ArrayList<>();
        for (int i = t.getChargement().size() - 1; i >= 0; i--) {
            lignes.add(new Object[]{i + 1, t.getChargement().get(i)});
        }
        return lignes;
    }
}
