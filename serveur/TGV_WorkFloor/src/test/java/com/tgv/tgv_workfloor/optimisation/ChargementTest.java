package com.tgv.tgv_workfloor.optimisation;

import com.tgv.tgv_workfloor.modele.Client;
import com.tgv.tgv_workfloor.modele.Colis;
import com.tgv.tgv_workfloor.modele.Commande;
import com.tgv.tgv_workfloor.modele.EtatPiece;
import com.tgv.tgv_workfloor.modele.Piece;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ChargementTest {

    @Test
    void unClientAuxColisPlusLegersDoitEtreLivreAvant() {
        boolean[][] p = ContraintesMasses.precedences(List.of(new double[]{45}, new double[]{62, 70}, new double[]{140}));
        assertThat(p[0][1]).isTrue();
        assertThat(p[1][0]).isFalse();
        assertThat(p[1][2]).isTrue();
        assertThat(p[0][2]).isTrue();
        assertThat(ContraintesMasses.conflits(p)).isEmpty();
    }

    @Test
    void desMassesEgalesLaissentLOrdreLibre() {
        boolean[][] p = ContraintesMasses.precedences(List.of(new double[]{50}, new double[]{50}));
        assertThat(p[0][1]).isFalse();
        assertThat(p[1][0]).isFalse();
    }

    @Test
    void desMassesEntrecroiseesSontEnConflit() {
        boolean[][] p = ContraintesMasses.precedences(List.of(new double[]{10, 40}, new double[]{20}));
        assertThat(ContraintesMasses.conflits(p)).hasSize(1);
    }

    @Test
    void lePlanChargeLeDernierClientEnBasEtLePlusLourdDessous() {
        Colis a1 = colis("C-1", 45);
        Colis b1 = colis("C-2", 62);
        Colis b2 = colis("C-3", 70);
        List<Colis> basVersHaut = PlanChargement.calculer(List.of(List.of(a1), List.of(b1, b2)));
        assertThat(basVersHaut).extracting(Colis::getReference).containsExactly("C-3", "C-2", "C-1");
        assertThat(PlanChargement.respecteOrdreDesMasses(basVersHaut)).isTrue();
    }

    @Test
    void unColisLourdPoseSurUnLegerEstDetecte() {
        List<Colis> basVersHaut = PlanChargement.calculer(List.of(List.of(colis("C-1", 200)), List.of(colis("C-2", 30))));
        assertThat(PlanChargement.respecteOrdreDesMasses(basVersHaut)).isFalse();
    }

    @Test
    void seulsLesClientsAuxMassesIdentiquesSontPermutables() {
        // groupes : {45, 45 45} puis {110, 110, 110 110} puis {260 260, 260} → 2! × 3! × 2! ordres
        List<double[]> masses = List.of(new double[]{45}, new double[]{45, 45}, new double[]{110},
                new double[]{110}, new double[]{110, 110}, new double[]{260, 260}, new double[]{260});
        ProblemeBoucle p = new ProblemeBoucle(Instances.aleatoire(7, 1), ContraintesMasses.precedences(masses));
        ProblemeBoucle.EspaceRecherche espace = p.espaceRecherche(1_000_000);
        assertThat(espace.depasse()).isFalse();
        assertThat(espace.ordres()).isEqualTo(24.0);
        assertThat(espace.etats()).isLessThan(128); // bien moins que les 2⁷ ensembles sans contrainte

        ProblemeBoucle toutesDistinctes = new ProblemeBoucle(Instances.aleatoire(3, 1),
                ContraintesMasses.precedences(List.of(new double[]{10}, new double[]{20}, new double[]{30})));
        assertThat(toutesDistinctes.espaceRecherche(1000).ordres()).isEqualTo(1.0);

        ProblemeBoucle enConflit = new ProblemeBoucle(Instances.aleatoire(2, 1),
                ContraintesMasses.precedences(List.of(new double[]{10, 40}, new double[]{20})));
        assertThat(enConflit.espaceRecherche(1000).ordres()).isZero();
        assertThat(new HeldKarpSolveur().estimer(enConflit).difficulte()).isEqualTo(Difficulte.IMPOSSIBLE);
        assertThat(new ChocoSolveur(1000).estimer(enConflit).realisable()).isFalse();
        assertThat(new PlusProcheVoisinSolveur().estimer(enConflit).difficulte()).isEqualTo(Difficulte.IMPOSSIBLE);
    }

    @Test
    void laDistanceEntreRodezEtOnetEstPlausible() {
        CalculateurDistances volOiseau = new CalculateurDistances(1.0);
        double d = volOiseau.distanceKm(44.3506, 2.5750, 44.3665, 2.5930);
        assertThat(d).isCloseTo(2.2, within(0.3));
        double[][] m = new CalculateurDistances(1.3).matrice(List.of(new double[]{44.35, 2.57}, new double[]{44.36, 2.59}));
        assertThat(m[0][1]).isEqualTo(m[1][0]).isGreaterThan(0);
        assertThat(m[0][0]).isZero();
    }

    private static Colis colis(String reference, double poids) {
        Client client = new Client("Client " + reference, "1 rue", "12000", "Rodez", null, 44.35, 2.57);
        Commande commande = new Commande("CMD-" + reference, client, LocalDate.now(), LocalDate.now());
        Colis c = new Colis(reference, commande, 10, 10, 10);
        c.getPieces().add(new Piece("P-" + reference, "Pièce", poids, EtatPiece.FABRIQUEE, commande));
        return c;
    }
}
