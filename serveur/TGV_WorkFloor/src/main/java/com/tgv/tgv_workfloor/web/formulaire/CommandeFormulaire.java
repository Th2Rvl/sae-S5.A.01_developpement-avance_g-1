package com.tgv.tgv_workfloor.web.formulaire;

import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class CommandeFormulaire {

    @NotNull(message = "Choisissez un client")
    private Long clientId;

    @NotNull(message = "La date de livraison prévue est obligatoire")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate datePrevue;

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public LocalDate getDatePrevue() {
        return datePrevue;
    }

    public void setDatePrevue(LocalDate datePrevue) {
        this.datePrevue = datePrevue;
    }
}
