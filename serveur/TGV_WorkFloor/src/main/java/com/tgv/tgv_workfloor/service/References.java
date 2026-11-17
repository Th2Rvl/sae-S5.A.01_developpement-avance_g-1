package com.tgv.tgv_workfloor.service;

import java.util.Collection;

/** Génère les références lisibles : CMD-0187, P-2031, C-0412… */
final class References {

    private References() {
    }

    /** Référence suivant la plus grande existante pour ce préfixe (ex. « C- » → « C-0426 »). */
    static String suivante(String prefixe, Collection<String> existantes, int premierNumero) {
        int max = premierNumero - 1;
        for (String reference : existantes) {
            if (reference != null && reference.startsWith(prefixe)) {
                try {
                    max = Math.max(max, Integer.parseInt(reference.substring(prefixe.length())));
                } catch (NumberFormatException ignoree) {
                    // référence saisie à la main : ignorée pour la numérotation
                }
            }
        }
        return prefixe + String.format("%04d", max + 1);
    }
}
