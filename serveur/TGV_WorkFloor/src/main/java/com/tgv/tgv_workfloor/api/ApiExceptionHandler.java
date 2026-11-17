package com.tgv.tgv_workfloor.api;

import com.tgv.tgv_workfloor.exception.RegleMetierException;
import com.tgv.tgv_workfloor.exception.RessourceIntrouvableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/** Toutes les erreurs de l'API sont renvoyées au format {"statut": …, "message": "…"}. */
@RestControllerAdvice(basePackageClasses = ApiExceptionHandler.class)
public class ApiExceptionHandler {

    @ExceptionHandler(RessourceIntrouvableException.class)
    ResponseEntity<Dto.Erreur> introuvable(RessourceIntrouvableException e) {
        return erreur(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(RegleMetierException.class)
    ResponseEntity<Dto.Erreur> regleMetier(RegleMetierException e) {
        return erreur(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Dto.Erreur> invalide(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " : " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return erreur(HttpStatus.BAD_REQUEST, "Requête invalide (" + detail + ")");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Dto.Erreur> illisible(HttpMessageNotReadableException e) {
        return erreur(HttpStatus.BAD_REQUEST, "Corps de requête JSON illisible ou valeur inconnue");
    }

    private static ResponseEntity<Dto.Erreur> erreur(HttpStatus statut, String message) {
        return ResponseEntity.status(statut).body(new Dto.Erreur(statut.value(), message));
    }
}
