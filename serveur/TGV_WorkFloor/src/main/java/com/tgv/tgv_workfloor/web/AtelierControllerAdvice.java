package com.tgv.tgv_workfloor.web;

import com.tgv.tgv_workfloor.exception.RessourceIntrouvableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

/** Données communes à toutes les pages Atelier (menu actif, utilisateur connecté) et page 404. */
@ControllerAdvice(basePackageClasses = AtelierControllerAdvice.class)
public class AtelierControllerAdvice {

    @ModelAttribute("uri")
    String uri(HttpServletRequest requete) {
        return requete.getRequestURI();
    }

    @ModelAttribute("utilisateurConnecte")
    String utilisateurConnecte(Principal principal) {
        return principal == null ? null : principal.getName();
    }

    @ExceptionHandler(RessourceIntrouvableException.class)
    String introuvable(RessourceIntrouvableException e, Model model, HttpServletRequest requete,
                       HttpServletResponse reponse, Principal principal) {
        // les @ModelAttribute ne s'appliquent pas au modèle d'un gestionnaire d'exception
        reponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
        model.addAttribute("uri", requete.getRequestURI());
        model.addAttribute("utilisateurConnecte", utilisateurConnecte(principal));
        model.addAttribute("message", e.getMessage());
        return "atelier/introuvable";
    }
}
