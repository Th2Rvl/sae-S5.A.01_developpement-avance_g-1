package com.tgv.tgv_workfloor.service;

import com.tgv.tgv_workfloor.depot.ColisDepot;
import com.tgv.tgv_workfloor.depot.CommandeDepot;
import com.tgv.tgv_workfloor.depot.PieceDepot;
import com.tgv.tgv_workfloor.exception.RegleMetierException;
import com.tgv.tgv_workfloor.exception.RessourceIntrouvableException;
import com.tgv.tgv_workfloor.modele.Client;
import com.tgv.tgv_workfloor.modele.Colis;
import com.tgv.tgv_workfloor.modele.Commande;
import com.tgv.tgv_workfloor.modele.EtatPiece;
import com.tgv.tgv_workfloor.modele.Piece;
import com.tgv.tgv_workfloor.modele.StatutColis;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** Saisie des commandes, de leurs pièces et emballage des pièces fabriquées en colis. */
@Service
public class CommandeService {

    private final CommandeDepot commandes;
    private final PieceDepot pieces;
    private final ColisDepot colis;
    private final ClientService clientService;
    private final TourneeService tourneeService;

    public CommandeService(CommandeDepot commandes, PieceDepot pieces, ColisDepot colis, ClientService clientService,
                           TourneeService tourneeService) {
        this.commandes = commandes;
        this.pieces = pieces;
        this.colis = colis;
        this.clientService = clientService;
        this.tourneeService = tourneeService;
    }

    // ------------------------------------------------------------------ commandes

    /** Commandes de la date prévue la plus proche à la plus lointaine. */
    public List<Commande> lister(EtatPiece etat) {
        return commandes.findAll().stream()
                .filter(c -> etat == null || c.getEtat() == etat)
                .sorted(Comparator.comparing(Commande::getDatePrevue).thenComparing(Commande::getReference))
                .toList();
    }

    public Commande trouver(Long id) {
        return commandes.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Commande", id));
    }

    public synchronized Commande creer(Long clientId, LocalDate datePrevue) {
        Client client = clientService.trouver(clientId);
        String reference = References.suivante("CMD-",
                commandes.findAll().stream().map(Commande::getReference).toList(), 1);
        return commandes.save(new Commande(reference, client, datePrevue, LocalDate.now()));
    }

    public synchronized Commande modifier(Long id, Long clientId, LocalDate datePrevue) {
        Commande commande = trouver(id);
        Client client = clientService.trouver(clientId);
        if (!client.getId().equals(commande.getClient().getId()) && !commande.getColis().isEmpty()) {
            throw new RegleMetierException("Des colis sont déjà emballés pour ce client : "
                    + "supprimez-les avant de changer le client de la commande.");
        }
        commande.setClient(client);
        commande.setDatePrevue(datePrevue);
        return commande;
    }

    public synchronized void supprimer(Long id) {
        Commande commande = trouver(id);
        for (Colis c : commande.getColis()) {
            verifierColisModifiable(c);
        }
        for (Colis c : List.copyOf(commande.getColis())) {
            supprimerColisSansControle(c);
        }
        commande.getPieces().forEach(pieces::delete);
        commandes.delete(commande);
    }

    // ------------------------------------------------------------------ pièces

    public Piece trouverPiece(Long id) {
        return pieces.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Pièce", id));
    }

    public synchronized Piece ajouterPiece(Long commandeId, String designation, double poidsKg, EtatPiece etat) {
        verifierPoids(poidsKg);
        Commande commande = trouver(commandeId);
        String reference = References.suivante("P-",
                pieces.findAll().stream().map(Piece::getReference).toList(), 2001);
        Piece piece = pieces.save(new Piece(reference, designation, poidsKg,
                etat == null ? EtatPiece.A_FAIRE : etat, commande));
        commande.getPieces().add(piece);
        return piece;
    }

    public synchronized Piece modifierPiece(Long pieceId, String designation, double poidsKg) {
        verifierPoids(poidsKg);
        Piece piece = trouverPiece(pieceId);
        if (piece.getColis() != null && piece.getPoidsKg() != poidsKg) {
            verifierColisModifiable(piece.getColis());
            if (piece.getColis().getTournee() != null) {
                piece.getColis().getTournee().effacerBoucle();
            }
        }
        piece.setDesignation(designation);
        piece.setPoidsKg(poidsKg);
        return piece;
    }

    public synchronized Piece changerEtat(Long pieceId, EtatPiece etat) {
        Piece piece = trouverPiece(pieceId);
        if (piece.getColis() != null && etat.compareTo(EtatPiece.FABRIQUEE) < 0) {
            throw new RegleMetierException("La pièce " + piece.getReference() + " est emballée dans le colis "
                    + piece.getColis().getReference() + " : retirez-la du colis avant de la remettre en fabrication.");
        }
        piece.setEtat(etat);
        return piece;
    }

    public synchronized void supprimerPiece(Long pieceId) {
        Piece piece = trouverPiece(pieceId);
        if (piece.getColis() != null) {
            throw new RegleMetierException("La pièce " + piece.getReference() + " est emballée dans le colis "
                    + piece.getColis().getReference() + " : supprimez d'abord le colis.");
        }
        piece.getCommande().getPieces().remove(piece);
        pieces.delete(piece);
    }

    // ------------------------------------------------------------------ colis

    public Colis trouverColis(Long id) {
        return colis.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Colis", id));
    }

    public List<Colis> listerColis() {
        return colis.findAll();
    }

    /**
     * Emballe des pièces fabriquées (d'une même commande) dans un nouveau colis. Le colis est disponible
     * aujourd'hui : il est aussitôt placé dans la tournée du lendemain (jour ouvré).
     */
    public synchronized Colis emballer(Long commandeId, Collection<Long> pieceIds, int longueurCm, int largeurCm,
                                       int hauteurCm) {
        Commande commande = trouver(commandeId);
        if (pieceIds == null || pieceIds.isEmpty()) {
            throw new RegleMetierException("Sélectionnez au moins une pièce à emballer.");
        }
        if (longueurCm <= 0 || largeurCm <= 0 || hauteurCm <= 0) {
            throw new RegleMetierException("Les dimensions du colis doivent être positives.");
        }
        List<Piece> aEmballer = pieceIds.stream().map(this::trouverPiece).toList();
        for (Piece p : aEmballer) {
            if (!p.getCommande().getId().equals(commande.getId())) {
                throw new RegleMetierException("La pièce " + p.getReference() + " n'appartient pas à la commande.");
            }
            if (p.getColis() != null) {
                throw new RegleMetierException("La pièce " + p.getReference() + " est déjà emballée.");
            }
            if (p.getEtat() != EtatPiece.FABRIQUEE) {
                throw new RegleMetierException("La pièce " + p.getReference() + " n'est pas fabriquée.");
            }
        }
        String reference = References.suivante("C-",
                colis.findAll().stream().map(Colis::getReference).toList(), 401);
        Colis nouveau = colis.save(new Colis(reference, commande, longueurCm, largeurCm, hauteurCm));
        for (Piece p : aEmballer) {
            p.setColis(nouveau);
            nouveau.getPieces().add(p);
        }
        commande.getColis().add(nouveau);
        LocalDate aujourdhui = LocalDate.now();
        nouveau.setDateDisponibilite(aujourdhui);
        nouveau.setDateLivraisonPrevue(PlanificationLivraisons.livraisonColisNeuf(aujourdhui));
        tourneeService.planifier(nouveau);
        return nouveau;
    }

    public synchronized void supprimerColis(Long colisId) {
        Colis c = trouverColis(colisId);
        verifierColisModifiable(c);
        supprimerColisSansControle(c);
    }

    private void supprimerColisSansControle(Colis c) {
        tourneeService.retirerDuPlanning(c);
        c.getPieces().forEach(p -> p.setColis(null));
        c.getCommande().getColis().remove(c);
        colis.delete(c);
    }

    private static void verifierColisModifiable(Colis c) {
        if (c.getStatut() != StatutColis.A_LIVRER) {
            throw new RegleMetierException("Le colis " + c.getReference() + " est déjà " + c.getStatut().getLibelle()
                    .toLowerCase() + " : il ne peut plus être modifié.");
        }
        if (c.getTournee() != null && c.getTournee().getStatut().estFigee()) {
            throw new RegleMetierException("Le colis " + c.getReference() + " est chargé dans la tournée du "
                    + c.getTournee().getDate() + " (" + c.getTournee().getStatut().getLibelle().toLowerCase() + ").");
        }
    }

    private static void verifierPoids(double poidsKg) {
        if (!(poidsKg > 0)) {
            throw new RegleMetierException("Le poids d'une pièce doit être strictement positif.");
        }
    }
}
