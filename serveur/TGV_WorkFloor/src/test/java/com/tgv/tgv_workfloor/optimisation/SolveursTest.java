package com.tgv.tgv_workfloor.optimisation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class SolveursTest {

    private final HeldKarpSolveur heldKarp = new HeldKarpSolveur();
    private final ChocoSolveur choco = new ChocoSolveur(10_000);
    private final PlusProcheVoisinSolveur approche = new PlusProcheVoisinSolveur();

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 5, 7, 8})
    void lesAlgorithmesExactsTrouventLOptimumSansContrainte(int n) {
        for (long graine = 1; graine <= 5; graine++) {
            ProblemeBoucle p = ProblemeBoucle.sansContrainte(Instances.aleatoire(n, graine));
            double optimum = Instances.optimumForceBrute(p);

            ResultatBoucle hk = heldKarp.resoudre(p);
            assertThat(p.estValide(hk.ordre())).isTrue();
            assertThat(hk.distanceKm()).isCloseTo(optimum, within(1e-6));
            assertThat(hk.optimaliteProuvee()).isTrue();

            ResultatBoucle bib = choco.resoudre(p);
            assertThat(p.estValide(bib.ordre())).isTrue();
            // Choco travaille en mètres entiers : écart d'arrondi d'au plus 0,5 m par tronçon
            assertThat(bib.distanceKm()).isCloseTo(optimum, within(0.0005 * (n + 1)));
            assertThat(bib.optimaliteProuvee()).isTrue();
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 6, 8})
    void lesAlgorithmesExactsRespectentLesPrecedences(int n) {
        for (long graine = 1; graine <= 5; graine++) {
            ProblemeBoucle p = new ProblemeBoucle(Instances.aleatoire(n, graine),
                    Instances.precedencesAleatoires(n, 0.3, graine));
            double optimum = Instances.optimumForceBrute(p);

            ResultatBoucle hk = heldKarp.resoudre(p);
            assertThat(p.estValide(hk.ordre())).isTrue();
            assertThat(hk.distanceKm()).isCloseTo(optimum, within(1e-6));

            ResultatBoucle bib = choco.resoudre(p);
            assertThat(p.estValide(bib.ordre())).isTrue();
            assertThat(bib.distanceKm()).isCloseTo(optimum, within(0.0005 * (n + 1)));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 8, 30, 80})
    void lApprocheDonneUneBoucleValideJamaisMeilleureQueLOptimum(int n) {
        for (long graine = 1; graine <= 5; graine++) {
            ProblemeBoucle p = new ProblemeBoucle(Instances.aleatoire(n, graine),
                    Instances.precedencesAleatoires(n, 0.05, graine));
            ResultatBoucle r = approche.resoudre(p);
            assertThat(p.estValide(r.ordre())).isTrue();
            assertThat(r.distanceKm()).isCloseTo(p.longueur(r.ordre()), within(1e-9));
            if (n <= 8) {
                assertThat(r.distanceKm()).isGreaterThanOrEqualTo(Instances.optimumForceBrute(p) - 1e-9);
            }
        }
    }

    @Test
    void leDeuxOptDecroiseUneBoucle() {
        // Carré : dépôt (0,0), clients (0,10), (10,10), (10,0). Ordre croisé 0-2-1 puis 2-opt.
        double[][] pts = {{0, 0}, {0, 10}, {10, 10}, {10, 0}};
        double[][] d = new double[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                d[i][j] = Math.hypot(pts[i][0] - pts[j][0], pts[i][1] - pts[j][1]);
            }
        }
        ProblemeBoucle p = ProblemeBoucle.sansContrainte(d);
        int[] croise = {0, 2, 1};
        approche.deuxOpt(p, croise);
        assertThat(p.longueur(croise)).isCloseTo(40, within(1e-9));
    }

    @Test
    void uneBoucleVideNeCouteRien() {
        ProblemeBoucle p = ProblemeBoucle.sansContrainte(new double[][]{{0}});
        assertThat(heldKarp.resoudre(p).ordre()).isEmpty();
        assertThat(choco.resoudre(p).ordre()).isEmpty();
        assertThat(approche.resoudre(p).ordre()).isEmpty();
    }

    @Test
    void desPrecedencesCycliquesRendentLeProblemeImpossible() {
        boolean[][] cycle = {{false, true}, {true, false}};
        ProblemeBoucle p = new ProblemeBoucle(Instances.aleatoire(2, 1), cycle);
        assertThatThrownBy(() -> heldKarp.resoudre(p)).isInstanceOf(BoucleImpossibleException.class);
        assertThatThrownBy(() -> choco.resoudre(p)).isInstanceOf(BoucleImpossibleException.class);
        assertThatThrownBy(() -> approche.resoudre(p)).isInstanceOf(BoucleImpossibleException.class);
    }

    @Test
    void heldKarpRefuseLesEspacesDeRechercheTropGrands() {
        ProblemeBoucle p = ProblemeBoucle.sansContrainte(Instances.aleatoire(19, 1));
        assertThatThrownBy(() -> heldKarp.resoudre(p)).isInstanceOf(BoucleImpossibleException.class);
        assertThat(heldKarp.estimer(p).difficulte()).isEqualTo(Difficulte.HORS_DE_PORTEE);
        assertThat(heldKarp.estimer(p).realisable()).isFalse();
    }

    @Test
    void heldKarpTraiteBeaucoupDeClientsQuandLesMassesImposentPresqueTout() {
        // 40 clients par paires de masses identiques : 2²⁰ ordres valides mais seulement ~60 états
        int n = 40;
        boolean[][] paires = new boolean[n][n];
        for (int a = 0; a < n; a++) {
            for (int b = 0; b < n; b++) {
                paires[a][b] = a / 2 < b / 2;
            }
        }
        ProblemeBoucle p = new ProblemeBoucle(Instances.aleatoire(n, 3), paires);
        assertThat(heldKarp.estimer(p).difficulte()).isEqualTo(Difficulte.FAIBLE);
        ResultatBoucle exact = heldKarp.resoudre(p);
        assertThat(p.estValide(exact.ordre())).isTrue();
        assertThat(exact.distanceKm()).isLessThanOrEqualTo(approche.resoudre(p).distanceKm() + 1e-9);
    }

    @Test
    void lEstimationDeLaDifficulteCroitAvecLaTaille() {
        ProblemeBoucle petit = ProblemeBoucle.sansContrainte(Instances.aleatoire(5, 1));
        ProblemeBoucle grand = ProblemeBoucle.sansContrainte(Instances.aleatoire(16, 1));
        assertThat(heldKarp.estimer(petit).difficulte()).isEqualTo(Difficulte.FAIBLE);
        assertThat(heldKarp.estimer(grand).difficulte()).isEqualTo(Difficulte.ELEVEE);
        assertThat(choco.estimer(grand).difficulte()).isEqualTo(Difficulte.ELEVEE);
        assertThat(approche.estimer(grand).difficulte()).isEqualTo(Difficulte.FAIBLE);
        assertThat(heldKarp.estimer(grand).operationsEstimees())
                .isGreaterThan(heldKarp.estimer(petit).operationsEstimees());
    }

    @Test
    void leProblemeVerifieSesDonnees() {
        assertThatThrownBy(() -> new ProblemeBoucle(new double[0][0], null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProblemeBoucle(new double[][]{{0, 1}}, null))
                .isInstanceOf(IllegalArgumentException.class);
        ProblemeBoucle p = ProblemeBoucle.sansContrainte(Instances.aleatoire(3, 1));
        assertThat(p.estValide(new int[]{0, 0, 1})).isFalse();
        assertThat(p.estValide(new int[]{0, 1})).isFalse();
        assertThat(p.aDesPrecedences()).isFalse();
    }
}
