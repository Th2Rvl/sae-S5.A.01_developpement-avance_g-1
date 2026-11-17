package com.tgv.tgv_workfloor.api;

import com.tgv.tgv_workfloor.modele.Algorithme;
import com.tgv.tgv_workfloor.modele.EtatBon;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Objets échangés en JSON avec l'application mobile (aucune entité métier n'est exposée directement).
 * Les distances sont en km arrondies à 0,1 ; les poids en kg.
 */
public final class Dto {

    private Dto() {
    }

    // ------------------------------------------------------------------ requêtes

    public record ConnexionDemande(@NotBlank String identifiant, @NotBlank String motDePasse) {
    }

    public record CalculDemande(@NotNull Algorithme algorithme) {
    }

    /** Refusé (409) si l'ordre ne respecte pas les masses : seuls les arrêts d'un même groupe se permutent. */
    public record OrdreManuelDemande(@NotEmpty List<Long> clientIds) {
    }

    /** colisIds absent ou vide = tous les colis du client sont remis. */
    public record BonDemande(@NotNull Long clientId, List<Long> colisIds, @NotNull EtatBon etat, String commentaire) {
    }

    // ------------------------------------------------------------------ réponses

    public record Erreur(int statut, String message) {
    }

    public record UtilisateurDto(Long id, String identifiant, String nom, String role) {
    }

    public record PointDto(String nom, String adresse, double latitude, double longitude) {
    }

    public record ClientDto(Long id, String nom, String adresse, String codePostal, String ville, String telephone,
                            double latitude, double longitude) {
    }

    public record PieceDto(Long id, String reference, String designation, double poidsKg, String etat,
                           String etatLibelle) {
    }

    public record ColisResume(Long id, String reference, String statut, String statutLibelle, Long clientId,
                              String clientNom, String ville, int nbPieces, double poidsKg, Integer numeroArret,
                              LocalDate dateLivraisonPrevue, boolean reporte) {
    }

    public record ColisDetail(Long id, String reference, String statut, String statutLibelle, String commandeReference,
                              double poidsKg, int nbPieces, Integer numeroArret, int longueurCm, int largeurCm,
                              int hauteurCm, ClientDto client, List<PieceDto> pieces, LocalDate dateDisponibilite,
                              LocalDate dateLivraisonPrevue, boolean reporte) {
    }

    /**
     * groupe : arrêts aux colis de masses identiques, permutables entre eux (flèches de l'ordre manuel) ;
     * permutable : vrai si le groupe compte plusieurs arrêts.
     */
    public record ArretDto(int ordre, ClientDto client, int nbColis, double poidsKg, double distanceDepuisPrecedentKm,
                           int groupe, boolean permutable, List<Long> colisIds, boolean livre, String numeroBon) {
    }

    /** position 1 = colis du bas du plateau (chargé en premier, déchargé en dernier). */
    public record ChargementDto(int position, Long colisId, String colisReference, String clientNom, double poidsKg,
                                Integer numeroArret) {
    }

    public record BonDto(Long id, String numero, Long clientId, String clientNom, LocalDateTime dateHeure, String etat,
                         String etatLibelle, String commentaire, List<String> colisReferences) {
    }

    public record TourneeDto(Long id, LocalDate date, String statut, String statutLibelle, PointDto atelier,
                             int nbColis, int nbClients, double poidsTotalKg, String algorithme,
                             String algorithmeLibelle, boolean optimaliteProuvee,
                             double distanceKm, double distanceRetourKm, long dureeCalculMs,
                             List<ColisResume> colis, List<ArretDto> arrets, List<ChargementDto> chargement,
                             List<BonDto> bons) {
    }

    public record EstimationDto(String algorithme, String libelle, String difficulte, String difficulteLibelle,
                                double operationsEstimees, String complexite, boolean realisable) {
    }

    /** nbOrdresPossibles : ordres de livraison compatibles avec les masses (0 si conflit). */
    public record DifficultesDto(int nbClients, int nbColis, double nbOrdresPossibles, List<String> conflitsDeMasses,
                                 List<EstimationDto> estimations) {
    }
}
