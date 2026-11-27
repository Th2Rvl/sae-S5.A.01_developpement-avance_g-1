package com.tgv.tgv_workfloor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class TableauDeBordController {

    @GetMapping("/atelier")
    public String afficher() {
        return "feature/tableauDeBord/tableauDeBord";
    }
}
