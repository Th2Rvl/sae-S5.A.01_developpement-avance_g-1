package com.tgv.tgv_workfloor.modele;

/**
 * Étape de la boucle : un client, son rang de passage, la distance depuis le point précédent
 * et son groupe de masses. Les arrêts d'un même groupe (colis de masses identiques) peuvent être
 * permutés entre eux ; l'ordre des groupes est imposé par les masses.
 */
public class Arret {
    private final int ordre;
    private final Client client;
    private final double distanceDepuisPrecedentKm;
    private final int groupe;

    public Arret(int ordre, Client client, double distanceDepuisPrecedentKm, int groupe) {
        this.ordre = ordre;
        this.client = client;
        this.distanceDepuisPrecedentKm = distanceDepuisPrecedentKm;
        this.groupe = groupe;
    }

    public int getOrdre() {
        return ordre;
    }

    public Client getClient() {
        return client;
    }

    public double getDistanceDepuisPrecedentKm() {
        return distanceDepuisPrecedentKm;
    }

    public int getGroupe() {
        return groupe;
    }
}
