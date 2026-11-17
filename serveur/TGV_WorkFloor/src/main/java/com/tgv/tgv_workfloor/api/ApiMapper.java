package com.tgv.tgv_workfloor.api;

import com.tgv.tgv_workfloor.modele.Arret;
import com.tgv.tgv_workfloor.modele.BonLivraison;
import com.tgv.tgv_workfloor.modele.Client;
import com.tgv.tgv_workfloor.modele.Colis;
import com.tgv.tgv_workfloor.modele.Piece;
import com.tgv.tgv_workfloor.modele.Tournee;
import com.tgv.tgv_workfloor.modele.Utilisateur;
import com.tgv.tgv_workfloor.optimisation.EstimationDifficulte;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Conversion du modèle métier vers les objets JSON de l'API. */
final class ApiMapper {

    private ApiMapper() {
    }

    static double arrondi(double valeur) {
        return Math.round(valeur * 10) / 10.0;
    }

    static Dto.UtilisateurDto utilisateur(Utilisateur u) {
        return new Dto.UtilisateurDto(u.getId(), u.getIdentifiant(), u.getNom(), u.getRole().name());
    }

    static Dto.ClientDto client(Client c) {
        return new Dto.ClientDto(c.getId(), c.getNom(), c.getAdresse(), c.getCodePostal(), c.getVille(),
                c.getTelephone(), c.getLatitude(), c.getLongitude());
    }

    static Dto.PieceDto piece(Piece p) {
        return new Dto.PieceDto(p.getId(), p.getReference(), p.getDesignation(), p.getPoidsKg(), p.getEtat().name(),
                p.getEtat().getLibelle());
    }

    static Dto.ColisResume colisResume(Colis c) {
        return new Dto.ColisResume(c.getId(), c.getReference(), c.getStatut().name(), c.getStatut().getLibelle(),
                c.getClient().getId(), c.getClient().getNom(), c.getClient().getVille(), c.getPieces().size(),
                arrondi(c.getPoidsKg()), numeroArret(c));
    }

    static Dto.ColisDetail colisDetail(Colis c) {
        return new Dto.ColisDetail(c.getId(), c.getReference(), c.getStatut().name(), c.getStatut().getLibelle(),
                c.getCommande().getReference(), arrondi(c.getPoidsKg()), c.getPieces().size(), numeroArret(c),
                c.getLongueurCm(), c.getLargeurCm(), c.getHauteurCm(), client(c.getClient()),
                c.getPieces().stream().map(ApiMapper::piece).toList());
    }

    static Dto.BonDto bon(BonLivraison b) {
        return new Dto.BonDto(b.getId(), b.getNumero(), b.getClient().getId(), b.getClient().getNom(), b.getDateHeure(),
                b.getEtat().name(), b.getEtat().getLibelle(), b.getCommentaire(),
                b.getColis().stream().map(Colis::getReference).toList());
    }

    static List<Dto.ChargementDto> chargement(Tournee t) {
        List<Dto.ChargementDto> liste = new ArrayList<>();
        int position = 1;
        for (Colis c : t.getChargement()) {
            liste.add(new Dto.ChargementDto(position++, c.getId(), c.getReference(), c.getClient().getNom(),
                    arrondi(c.getPoidsKg()), numeroArret(c)));
        }
        return liste;
    }

    static Dto.TourneeDto tournee(Tournee t) {
        List<Dto.ArretDto> arrets = new ArrayList<>();
        for (Arret a : t.getArrets()) {
            List<Colis> colis = t.colisDuClient(a.getClient());
            BonLivraison bon = t.bonDuClient(a.getClient()).orElse(null);
            arrets.add(new Dto.ArretDto(a.getOrdre(), client(a.getClient()), colis.size(),
                    arrondi(colis.stream().mapToDouble(Colis::getPoidsKg).sum()),
                    arrondi(a.getDistanceDepuisPrecedentKm()), a.getGroupe(), t.tailleGroupe(a.getGroupe()) > 1,
                    colis.stream().map(Colis::getId).toList(),
                    bon != null, bon == null ? null : bon.getNumero()));
        }
        List<Dto.ColisResume> colis = t.getColis().stream()
                .sorted(Comparator.comparing((Colis c) -> c.getClient().getNom()).thenComparing(Colis::getReference))
                .map(ApiMapper::colisResume).toList();
        return new Dto.TourneeDto(t.getId(), t.getDate(), t.getStatut().name(), t.getStatut().getLibelle(),
                new Dto.PointDto(t.getAtelier().getNom(), t.getAtelier().getAdresse(), t.getAtelier().getLatitude(),
                        t.getAtelier().getLongitude()),
                t.getColis().size(), t.getClients().size(), arrondi(t.getPoidsTotalKg()),
                t.getAlgorithme() == null ? null : t.getAlgorithme().name(),
                t.getAlgorithme() == null ? null : t.getAlgorithme().getLibelle(),
                t.isOptimaliteProuvee(), arrondi(t.getDistanceTotaleKm()),
                arrondi(t.getDistanceRetourKm()), t.getDureeCalculMs(), colis, arrets, chargement(t),
                t.getBons().stream().map(ApiMapper::bon).toList());
    }

    static Dto.EstimationDto estimation(EstimationDifficulte e) {
        return new Dto.EstimationDto(e.algorithme().name(), e.algorithme().getLibelle(), e.difficulte().name(),
                e.difficulte().getLibelle(), e.operationsEstimees(), e.complexite(), e.realisable());
    }

    private static Integer numeroArret(Colis c) {
        if (c.getTournee() == null) {
            return null;
        }
        return c.getTournee().arretDuClient(c.getClient()).map(Arret::getOrdre).orElse(null);
    }
}
