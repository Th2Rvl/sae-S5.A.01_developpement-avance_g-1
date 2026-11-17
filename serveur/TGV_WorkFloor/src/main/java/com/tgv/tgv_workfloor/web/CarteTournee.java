package com.tgv.tgv_workfloor.web;

import com.tgv.tgv_workfloor.modele.Arret;
import com.tgv.tgv_workfloor.modele.Client;
import com.tgv.tgv_workfloor.modele.Tournee;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Données de la carte Leaflet de l'onglet Carte (maquette 04). */
public final class CarteTournee {

    private CarteTournee() {
    }

    /**
     * Points affichés : l'atelier (départ et retour), puis les clients, numérotés si la boucle est calculée.
     *
     * @param focus identifiant du client sur lequel centrer la carte, ou {@code null}
     */
    public static Map<String, Object> donnees(Tournee t, Long focus) {
        Map<String, Object> carte = new LinkedHashMap<>();
        carte.put("depot", Map.of("nom", t.getAtelier().getNom(), "lat", t.getAtelier().getLatitude(),
                "lon", t.getAtelier().getLongitude()));
        List<Map<String, Object>> points = new ArrayList<>();
        if (t.isBoucleCalculee()) {
            for (Arret a : t.getArrets()) {
                points.add(point(a.getOrdre(), a.getClient()));
            }
        } else {
            t.getClients().forEach(c -> points.add(point(0, c)));
        }
        carte.put("arrets", points);
        carte.put("boucle", t.isBoucleCalculee());
        carte.put("focus", focus);
        return carte;
    }

    private static Map<String, Object> point(int ordre, Client client) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("ordre", ordre);
        p.put("client", client.getId());
        p.put("nom", client.getNom());
        p.put("ville", client.getVille());
        p.put("lat", client.getLatitude());
        p.put("lon", client.getLongitude());
        return p;
    }
}
