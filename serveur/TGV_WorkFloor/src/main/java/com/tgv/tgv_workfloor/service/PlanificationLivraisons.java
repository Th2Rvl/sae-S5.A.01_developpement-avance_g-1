package com.tgv.tgv_workfloor.service;

import com.tgv.tgv_workfloor.modele.Colis;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Règle de planification des livraisons :
 * <ul>
 *   <li>un colis neuf est livré dès qu'il est disponible : emballé dans la journée, il part le lendemain
 *       (jour ouvré : emballé le vendredi ou le week-end, il part le lundi) ;</li>
 *   <li>un colis qui n'a pas pu être livré (tournée terminée sans bon pour son client) est reporté au jeudi
 *       suivant, où tous les reports sont regroupés.</li>
 * </ul>
 * Aucune tournée n'a lieu le samedi ni le dimanche.
 */
public final class PlanificationLivraisons {

    public static final DayOfWeek JOUR_DES_REPORTS = DayOfWeek.THURSDAY;

    private PlanificationLivraisons() {
    }

    public static boolean estOuvre(LocalDate jour) {
        return jour.getDayOfWeek() != DayOfWeek.SATURDAY && jour.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    /** Premier jour ouvré strictement après {@code jour}. */
    public static LocalDate lendemainOuvre(LocalDate jour) {
        LocalDate suivant = jour.plusDays(1);
        while (!estOuvre(suivant)) {
            suivant = suivant.plusDays(1);
        }
        return suivant;
    }

    /** Premier jour ouvré à partir de {@code jour} inclus. */
    public static LocalDate premierJourOuvre(LocalDate jour) {
        return estOuvre(jour) ? jour : lendemainOuvre(jour);
    }

    /** Premier jeudi strictement après {@code jour}. */
    public static LocalDate jeudiSuivant(LocalDate jour) {
        return jour.with(TemporalAdjusters.next(JOUR_DES_REPORTS));
    }

    /** Date de livraison d'un colis neuf emballé le jour {@code disponibilite}. */
    public static LocalDate livraisonColisNeuf(LocalDate disponibilite) {
        return lendemainOuvre(disponibilite);
    }

    /** Date de livraison d'un colis non livré lors de la tournée du jour {@code tourneeManquee}. */
    public static LocalDate livraisonColisReporte(LocalDate tourneeManquee) {
        return jeudiSuivant(tourneeManquee);
    }

    /** Date à partir de laquelle le colis peut partir (sa date prévue si elle est connue). */
    public static LocalDate dateDue(Colis colis) {
        if (colis.getDateLivraisonPrevue() != null) {
            return colis.getDateLivraisonPrevue();
        }
        return colis.getDateDisponibilite() == null ? LocalDate.MIN : livraisonColisNeuf(colis.getDateDisponibilite());
    }

    /**
     * Le colis peut-il partir dans la tournée du jour {@code date} ? Un colis neuf part dès sa date due (ou plus tard
     * s'il est en retard) ; un colis reporté ne part qu'un jeudi.
     */
    public static boolean peutPartirLe(Colis colis, LocalDate date) {
        if (!estOuvre(date) || dateDue(colis).isAfter(date)) {
            return false;
        }
        return !colis.isReporte() || date.getDayOfWeek() == JOUR_DES_REPORTS;
    }

    /** Prochaine date de tournée possible pour le colis après {@code date} (tournée de ce jour-là déjà partie). */
    public static LocalDate dateSuivante(Colis colis, LocalDate date) {
        return colis.isReporte() ? jeudiSuivant(date) : lendemainOuvre(date);
    }

    /** Première date de tournée possible pour le colis à partir de {@code aujourdhui}. */
    public static LocalDate premiereDate(Colis colis, LocalDate aujourdhui) {
        LocalDate due = dateDue(colis);
        LocalDate date = due.isBefore(aujourdhui) ? aujourdhui : due;
        if (colis.isReporte()) {
            return date.getDayOfWeek() == JOUR_DES_REPORTS ? date : jeudiSuivant(date);
        }
        return premierJourOuvre(date);
    }
}
