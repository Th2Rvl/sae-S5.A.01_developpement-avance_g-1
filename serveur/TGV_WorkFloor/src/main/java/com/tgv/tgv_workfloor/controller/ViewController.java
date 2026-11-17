package com.tgv.tgv_workfloor.controller;

import com.tgv.tgv_workfloor.config.SecurityConfig;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class ViewController {

    /** La racine du site mène à l'espace de l'utilisateur connecté (ou à la connexion si besoin). */
    @GetMapping("/")
    public String racine(Authentication authentification) {
        return "redirect:" + SecurityConfig.accueilSelonRole(authentification);
    }

    @GetMapping("/connexion")
    public String connexion() {
        return "connexion";
    }
}
