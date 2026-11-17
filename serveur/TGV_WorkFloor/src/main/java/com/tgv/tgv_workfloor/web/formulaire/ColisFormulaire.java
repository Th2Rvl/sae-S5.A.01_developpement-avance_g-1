package com.tgv.tgv_workfloor.web.formulaire;

import java.util.ArrayList;
import java.util.List;

/** Emballage : pièces cochées et dimensions du colis (contrôlées par le service). */
public class ColisFormulaire {

    private List<Long> pieceIds = new ArrayList<>();
    private int longueurCm = 120;
    private int largeurCm = 80;
    private int hauteurCm = 40;

    public List<Long> getPieceIds() {
        return pieceIds;
    }

    public void setPieceIds(List<Long> pieceIds) {
        this.pieceIds = pieceIds;
    }

    public int getLongueurCm() {
        return longueurCm;
    }

    public void setLongueurCm(int longueurCm) {
        this.longueurCm = longueurCm;
    }

    public int getLargeurCm() {
        return largeurCm;
    }

    public void setLargeurCm(int largeurCm) {
        this.largeurCm = largeurCm;
    }

    public int getHauteurCm() {
        return hauteurCm;
    }

    public void setHauteurCm(int hauteurCm) {
        this.hauteurCm = hauteurCm;
    }
}
