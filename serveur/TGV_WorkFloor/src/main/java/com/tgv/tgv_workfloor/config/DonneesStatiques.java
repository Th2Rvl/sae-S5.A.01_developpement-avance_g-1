package com.tgv.tgv_workfloor.config;

import com.tgv.tgv_workfloor.depot.AtelierDepot;
import com.tgv.tgv_workfloor.depot.ClientDepot;
import com.tgv.tgv_workfloor.depot.ColisDepot;
import com.tgv.tgv_workfloor.depot.CommandeDepot;
import com.tgv.tgv_workfloor.depot.PieceDepot;
import com.tgv.tgv_workfloor.modele.Algorithme;
import com.tgv.tgv_workfloor.modele.Atelier;
import com.tgv.tgv_workfloor.modele.BonLivraison;
import com.tgv.tgv_workfloor.modele.Client;
import com.tgv.tgv_workfloor.modele.Colis;
import com.tgv.tgv_workfloor.modele.Commande;
import com.tgv.tgv_workfloor.modele.EtatBon;
import com.tgv.tgv_workfloor.modele.EtatPiece;
import com.tgv.tgv_workfloor.modele.Piece;
import com.tgv.tgv_workfloor.modele.Role;
import com.tgv.tgv_workfloor.modele.Tournee;
import com.tgv.tgv_workfloor.modele.Utilisateur;
import com.tgv.tgv_workfloor.service.PlanificationLivraisons;
import com.tgv.tgv_workfloor.service.TourneeService;
import com.tgv.tgv_workfloor.service.UtilisateurService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Jeu de données statique chargé en mémoire au démarrage (en attendant la base MySQL).
 * Il reprend les maquettes : 12 colis chez 7 clients autour de Rodez pour le prochain jeudi,
 * une tournée terminée le jeudi précédent et quelques commandes en fabrication.
 * Désactivable avec tgv.donnees-statiques=false.
 */
@Component
@ConditionalOnProperty(name = "tgv.donnees-statiques", havingValue = "true", matchIfMissing = true)
public class DonneesStatiques implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DonneesStatiques.class);

    private final AtelierDepot ateliers;
    private final ClientDepot clients;
    private final CommandeDepot commandes;
    private final PieceDepot pieces;
    private final ColisDepot colis;
    private final UtilisateurService utilisateurs;
    private final TourneeService tournees;

    public DonneesStatiques(AtelierDepot ateliers, ClientDepot clients, CommandeDepot commandes, PieceDepot pieces,
                            ColisDepot colis, UtilisateurService utilisateurs, TourneeService tournees) {
        this.ateliers = ateliers;
        this.clients = clients;
        this.commandes = commandes;
        this.pieces = pieces;
        this.colis = colis;
        this.utilisateurs = utilisateurs;
        this.tournees = tournees;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (clients.count() > 0) {
            return;
        }
        LocalDate jeudi = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY));
        LocalDate jeudiPasse = jeudi.minusWeeks(1);

        ateliers.save(new Atelier("Atelier T.G.V", "ZA de Bel-Air, 12000 Rodez", 44.3350, 2.5750));

        utilisateurs.creer("atelier", "Responsable atelier", "atelier", Role.ATELIER);
        Utilisateur chauffeur = utilisateurs.creer("chauffeur01", "Chauffeur 01", "1234", Role.CHAUFFEUR);
        utilisateurs.creer("chauffeur02", "Chauffeur 02", "1234", Role.CHAUFFEUR);

        Client fabre = client("Garage Fabre PL", "4 avenue de Bruxelles", "12740", "Sébazac-Concourès",
                "05 65 46 12 30", 44.4035, 2.6045);
        Client causse = client("Métallerie Causse", "12 rue des Forges", "12850", "Onet-le-Château",
                "05 65 42 18 77", 44.3665, 2.5930);
        Client hydro = client("Hydro Aveyron", "ZI de Cantaranne", "12000", "Rodez",
                "05 65 68 20 41", 44.3590, 2.5905);
        Client bati = client("Bâti-Rouergue", "8 route de Rodez", "12510", "Olemps",
                "05 65 69 03 52", 44.3470, 2.5450);
        Client marty = client("Scierie Marty", "Lieu-dit La Garrigue", "12510", "Druelle",
                "05 65 71 24 66", 44.3615, 2.4985);
        Client levezou = client("Ateliers du Lévézou", "ZA du Puech", "12450", "Flavin",
                "05 65 46 91 10", 44.2885, 2.6060);
        Client vabre = client("SARL Vabre Agri", "21 avenue de Toulouse", "12450", "Luc-la-Primaube",
                "05 65 71 50 08", 44.3065, 2.5375);
        Client bousquet = client("Menuiserie Bousquet", "3 rue du Causse", "12340", "Bozouls",
                "05 65 44 60 21", 44.4710, 2.7210);
        Client segala = client("Agri-Ségala", "Route d'Albi", "12160", "Baraqueville",
                "05 65 69 11 47", 44.2770, 2.4330);
        Client lacombe = client("Transports Lacombe", "ZA de la Plaine", "12310", "Laissac",
                "05 65 70 82 93", 44.3830, 2.8240);

        // --- Tournée du jeudi précédent : terminée, avec ses bons de livraison
        Commande c180 = commande("CMD-0180", bousquet, jeudiPasse);
        colis("C-0401", c180, 200, 120, 30, piece("P-1990", "Escalier hélicoïdal (limon)", 95, c180));
        Commande c181 = commande("CMD-0181", segala, jeudiPasse);
        colis("C-0402", c181, 240, 100, 60, piece("P-1991", "Châssis de remorque", 310, c181),
                piece("P-1992", "Timon", 45, c181));
        Commande c182 = commande("CMD-0182", lacombe, jeudiPasse);
        colis("C-0403", c182, 120, 80, 50, piece("P-1993", "Attelage renforcé", 70, c182));
        colis("C-0404", c182, 150, 80, 50, piece("P-1994", "Hayon élévateur (bras)", 85, c182));
        Tournee passee = tournees.preparer(jeudiPasse);
        tournees.calculer(passee.getId(), Algorithme.EXACT_MAISON);
        tournees.valider(passee.getId(), chauffeur);
        int heure = 9;
        for (var arret : List.copyOf(passee.getArrets())) {
            boolean reserves = arret.getClient() == segala;
            BonLivraison bon = tournees.enregistrerBon(passee.getId(), arret.getClient().getId(), List.of(),
                    reserves ? EtatBon.RESERVES : EtatBon.CONFORME,
                    reserves ? "Peinture rayée sur le timon, accepté avec réserve." : null, chauffeur);
            bon.setDateHeure(jeudiPasse.atTime(heure++, 20));
        }

        // --- Tournée du prochain jeudi : 12 colis chez 7 clients, en trois groupes de masses identiques.
        // Les masses imposent l'ordre des groupes (45 kg, puis 110 kg, puis 260 kg) ; l'optimisation choisit
        // l'ordre des clients à l'intérieur de chaque groupe : 2! × 3! × 2! = 24 ordres possibles.
        Commande c185 = commande("CMD-0185", fabre, jeudi);
        colis("C-0418", c185, 80, 60, 40,
                piece("P-2025", "Support de pont", 15, c185), piece("P-2026", "Patte de fixation", 10, c185),
                piece("P-2027", "Patte de fixation", 10, c185), piece("P-2028", "Lot de boulonnerie", 10, c185));
        Commande c187 = commande("CMD-0187", causse, jeudi);
        colis("C-0412", c187, 120, 80, 40,
                piece("P-2031", "Platine acier 600 × 400", 20, c187), piece("P-2032", "Platine acier 600 × 400", 20, c187),
                piece("P-2035", "Lot de visserie M16", 5, c187));
        colis("C-0413", c187, 200, 40, 40,
                piece("P-2033", "Cornière 80 × 80", 25, c187), piece("P-2034", "Cornière 60 × 60", 20, c187));
        Commande c189 = commande("CMD-0189", hydro, jeudi);
        colis("C-0420", c189, 120, 100, 80,
                piece("P-2045", "Bride DN300", 60, c189), piece("P-2046", "Bride DN200", 50, c189));
        colis("C-0421", c189, 160, 110, 80, piece("P-2047", "Vanne murale 600", 110, c189));
        Commande c186 = commande("CMD-0186", bati, jeudi);
        colis("C-0415", c186, 300, 60, 40,
                piece("P-2020", "Poutre IPN 200", 65, c186), piece("P-2021", "Poutre IPN 160", 45, c186));
        Commande c188 = commande("CMD-0188", marty, jeudi);
        colis("C-0416", c188, 250, 120, 50, piece("P-2040", "Carter de scie à ruban", 110, c188));
        colis("C-0417", c188, 220, 110, 60,
                piece("P-2041", "Table d'amenage", 70, c188), piece("P-2042", "Guide de coupe", 40, c188));
        Commande c190 = commande("CMD-0190", levezou, jeudi);
        colis("C-0423", c190, 240, 120, 90, piece("P-2050", "Bâti de presse", 260, c190));
        colis("C-0424", c190, 260, 120, 90,
                piece("P-2051", "Table de presse", 160, c190), piece("P-2052", "Coulisseau", 100, c190));
        Commande c191 = commande("CMD-0191", vabre, jeudi);
        colis("C-0414", c191, 400, 200, 80,
                piece("P-2055", "Godet de chargeur", 160, c191), piece("P-2057", "Lame niveleuse", 100, c191));
        colis("C-0419", c191, 300, 120, 60,
                piece("P-2056", "Bras de relevage", 130, c191), piece("P-2058", "Bras de relevage", 130, c191));
        Tournee prochaine = tournees.preparer(jeudi);
        tournees.calculer(prochaine.getId(), Algorithme.EXACT_MAISON);

        // --- Commandes en fabrication (pas encore livrables). Le batardeau P-2063 est fabriqué : l'emballer
        // depuis l'Atelier le place automatiquement dans la tournée du lendemain (jour ouvré).
        Commande c192 = commande("CMD-0192", bousquet, jeudi.plusWeeks(1));
        piece("P-2060", "Garde-corps 2 m", 65, c192).setEtat(EtatPiece.EN_FABRICATION);
        piece("P-2061", "Garde-corps 1,5 m", 50, c192);
        Commande c193 = commande("CMD-0193", segala, jeudi.plusWeeks(1));
        piece("P-2062", "Benne basculante", 540, c193);
        Commande c194 = commande("CMD-0194", hydro, jeudi.plusWeeks(2));
        piece("P-2063", "Batardeau aluminium", 75, c194).setEtat(EtatPiece.FABRIQUEE);
        piece("P-2064", "Batardeau aluminium", 75, c194).setEtat(EtatPiece.EN_FABRICATION);

        LOG.info("Données statiques chargées : {} clients, {} commandes, {} colis, tournée du {} prête",
                clients.count(), commandes.count(), colis.count(), jeudi);
    }

    private Client client(String nom, String adresse, String cp, String ville, String tel, double lat, double lon) {
        return clients.save(new Client(nom, adresse, cp, ville, tel, lat, lon));
    }

    private Commande commande(String reference, Client client, LocalDate datePrevue) {
        return commandes.save(new Commande(reference, client, datePrevue, datePrevue.minusDays(12)));
    }

    /** Pièce « à faire » ajoutée à la commande (l'état est ajusté ensuite si besoin). */
    private Piece piece(String reference, String designation, double poids, Commande commande) {
        Piece p = pieces.save(new Piece(reference, designation, poids, EtatPiece.A_FAIRE, commande));
        commande.getPieces().add(p);
        return p;
    }

    /**
     * Colis emballant des pièces, qui passent donc à l'état « fabriquée ». Il est emballé la veille de la date
     * prévue de la commande (un mercredi) : la règle de planification le fait donc partir le jeudi.
     */
    private void colis(String reference, Commande commande, int l, int la, int h, Piece... contenu) {
        Colis c = colis.save(new Colis(reference, commande, l, la, h));
        c.setDateDisponibilite(commande.getDatePrevue().minusDays(1));
        c.setDateLivraisonPrevue(PlanificationLivraisons.livraisonColisNeuf(c.getDateDisponibilite()));
        for (Piece p : contenu) {
            p.setEtat(EtatPiece.FABRIQUEE);
            p.setColis(c);
            c.getPieces().add(p);
        }
        commande.getColis().add(c);
    }
}
