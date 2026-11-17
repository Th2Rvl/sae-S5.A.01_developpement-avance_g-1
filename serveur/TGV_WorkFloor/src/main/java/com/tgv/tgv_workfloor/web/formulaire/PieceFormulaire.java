package com.tgv.tgv_workfloor.web.formulaire;

import com.tgv.tgv_workfloor.modele.EtatPiece;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PieceFormulaire {

    @NotBlank(message = "La désignation est obligatoire")
    @Size(max = 120)
    private String designation;

    @NotNull(message = "Le poids est obligatoire")
    @DecimalMin(value = "0.1", message = "Le poids doit être positif")
    private Double poidsKg;

    private EtatPiece etat = EtatPiece.A_FAIRE;

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public Double getPoidsKg() {
        return poidsKg;
    }

    public void setPoidsKg(Double poidsKg) {
        this.poidsKg = poidsKg;
    }

    public EtatPiece getEtat() {
        return etat;
    }

    public void setEtat(EtatPiece etat) {
        this.etat = etat;
    }
}
