package com.tgv.tgv_workfloor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class ViewController {

    /** La racine du site mène à l'espace chauffeur (ou à la connexion si besoin). */
    @GetMapping("/")
    public String racine() {
        return "redirect:/chauffeur";
    }

    @GetMapping("/connexion")
    public String connexion() {
        return "connexion";
    }
}
