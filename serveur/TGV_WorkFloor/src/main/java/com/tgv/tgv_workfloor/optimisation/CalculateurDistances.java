package com.tgv.tgv_workfloor.optimisation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Distance entre deux points GPS : formule de haversine (vol d'oiseau) multipliée par un
 * coefficient routier pour approcher la distance réelle sur route.
 * Pourra être remplacé par un service d'itinéraire (OSRM sur les données OpenStreetMap).
 */
@Component
public class CalculateurDistances {

    private static final double RAYON_TERRE_KM = 6371.0;

    private final double coefficientRoutier;

    public CalculateurDistances(@Value("${tgv.distances.coefficient-routier:1.3}") double coefficientRoutier) {
        this.coefficientRoutier = coefficientRoutier;
    }

    public double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * RAYON_TERRE_KM * Math.asin(Math.min(1, Math.sqrt(a))) * coefficientRoutier;
    }

    /** Matrice des distances entre points {latitude, longitude} ; le premier point est le dépôt. */
    public double[][] matrice(List<double[]> points) {
        int taille = points.size();
        double[][] matrice = new double[taille][taille];
        for (int i = 0; i < taille; i++) {
            for (int j = i + 1; j < taille; j++) {
                double d = distanceKm(points.get(i)[0], points.get(i)[1], points.get(j)[0], points.get(j)[1]);
                matrice[i][j] = d;
                matrice[j][i] = d;
            }
        }
        return matrice;
    }
}
