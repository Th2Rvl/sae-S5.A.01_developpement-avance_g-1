package com.tgv.tgv_workfloor.service;

import com.tgv.tgv_workfloor.modele.Colis;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Règle : colis neuf livré le lendemain ouvré de son emballage, colis non livré reporté au jeudi suivant. */
class PlanificationLivraisonsTest {

    private static final LocalDate LUNDI = LocalDate.of(2026, 11, 16);
    private static final LocalDate MERCREDI = LUNDI.plusDays(2);
    private static final LocalDate JEUDI = LUNDI.plusDays(3);
    private static final LocalDate VENDREDI = LUNDI.plusDays(4);
    private static final LocalDate SAMEDI = LUNDI.plusDays(5);

    @Test
    void unColisNeufPartLeLendemainOuvre() {
        assertThat(PlanificationLivraisons.livraisonColisNeuf(LUNDI)).isEqualTo(LUNDI.plusDays(1));
        assertThat(PlanificationLivraisons.livraisonColisNeuf(MERCREDI)).isEqualTo(JEUDI);
        assertThat(PlanificationLivraisons.livraisonColisNeuf(VENDREDI)).isEqualTo(LUNDI.plusWeeks(1));
        assertThat(PlanificationLivraisons.livraisonColisNeuf(SAMEDI)).isEqualTo(LUNDI.plusWeeks(1));
    }

    @Test
    void unColisNonLivreEstReporteAuJeudiSuivant() {
        assertThat(PlanificationLivraisons.livraisonColisReporte(LUNDI)).isEqualTo(JEUDI);
        assertThat(PlanificationLivraisons.livraisonColisReporte(JEUDI)).isEqualTo(JEUDI.plusWeeks(1));
        assertThat(PlanificationLivraisons.livraisonColisReporte(VENDREDI)).isEqualTo(JEUDI.plusWeeks(1));
    }

    @Test
    void unColisNeufPeutPartirDesSaDateEtRattraperSonRetard() {
        Colis c = colis(LUNDI, false);
        assertThat(PlanificationLivraisons.peutPartirLe(c, LUNDI)).isFalse();
        assertThat(PlanificationLivraisons.peutPartirLe(c, LUNDI.plusDays(1))).isTrue();
        assertThat(PlanificationLivraisons.peutPartirLe(c, VENDREDI)).isTrue();
        assertThat(PlanificationLivraisons.peutPartirLe(c, SAMEDI)).isFalse();
        assertThat(PlanificationLivraisons.premiereDate(c, LUNDI)).isEqualTo(LUNDI.plusDays(1));
        assertThat(PlanificationLivraisons.premiereDate(c, SAMEDI)).isEqualTo(LUNDI.plusWeeks(1));
    }

    @Test
    void unColisReporteNePartQueLeJeudi() {
        Colis c = colis(LUNDI, true);
        c.setDateLivraisonPrevue(JEUDI);
        assertThat(PlanificationLivraisons.peutPartirLe(c, MERCREDI)).isFalse();
        assertThat(PlanificationLivraisons.peutPartirLe(c, JEUDI)).isTrue();
        assertThat(PlanificationLivraisons.peutPartirLe(c, VENDREDI)).isFalse();
        assertThat(PlanificationLivraisons.peutPartirLe(c, JEUDI.plusWeeks(1))).isTrue();
        assertThat(PlanificationLivraisons.premiereDate(c, VENDREDI)).isEqualTo(JEUDI.plusWeeks(1));
        assertThat(PlanificationLivraisons.dateSuivante(c, JEUDI)).isEqualTo(JEUDI.plusWeeks(1));
    }

    private static Colis colis(LocalDate disponibilite, boolean reporte) {
        Colis c = new Colis();
        c.setDateDisponibilite(disponibilite);
        c.setReporte(reporte);
        return c;
    }
}
