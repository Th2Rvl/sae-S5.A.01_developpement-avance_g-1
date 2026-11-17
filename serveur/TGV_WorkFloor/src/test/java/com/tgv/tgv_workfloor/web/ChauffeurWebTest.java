package com.tgv.tgv_workfloor.web;

import com.tgv.tgv_workfloor.modele.Arret;
import com.tgv.tgv_workfloor.modele.Colis;
import com.tgv.tgv_workfloor.modele.StatutTournee;
import com.tgv.tgv_workfloor.modele.Tournee;
import com.tgv.tgv_workfloor.service.TourneeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Espace chauffeur du site : écrans de la maquette « 1 · Préparer la tournée ». */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ChauffeurWebTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private TourneeService tournees;

    @Test
    void sansConnexionOnEstRedirigeVersLaConnexionChauffeur() throws Exception {
        mvc.perform(get("/chauffeur")).andExpect(redirectedUrl("/connexion"));
        mvc.perform(get("/connexion")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Connexion chauffeur")))
                .andExpect(content().string(containsString("Mémoriser ces informations")));
    }

    @Test
    void seulUnChauffeurPeutSeConnecter() throws Exception {
        mvc.perform(formLogin("/connexion").userParameter("identifiant").passwordParam("motDePasse")
                .user("chauffeur01").password("1234")).andExpect(redirectedUrl("/chauffeur"));
        mvc.perform(post("/connexion").with(csrf()).param("identifiant", "chauffeur01").param("motDePasse", "1234")
                .param("memoriser", "on")).andExpect(cookie().exists("remember-me"));
        mvc.perform(formLogin("/connexion").userParameter("identifiant").passwordParam("motDePasse")
                        .user("atelier").password("atelier"))
                .andExpect(redirectedUrl("/connexion?nonChauffeur"))
                .andExpect(unauthenticated());
    }

    @Test
    @WithMockUser(username = "atelier", roles = "ATELIER")
    void lAtelierNAccedePasALEspaceChauffeur() throws Exception {
        mvc.perform(get("/chauffeur")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "chauffeur01", roles = "CHAUFFEUR")
    void lInterfaceAtelierNExistePlus() throws Exception {
        mvc.perform(get("/atelier")).andExpect(status().isNotFound());
        mvc.perform(get("/atelier/commandes")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "chauffeur01", roles = "CHAUFFEUR")
    void ongletsColisCarteEtDetailDUnColis() throws Exception {
        Tournee tournee = tournees.courante();
        Colis colis = tournee.getColis().getFirst();
        mvc.perform(get("/chauffeur")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Tournée du jour")))
                .andExpect(content().string(containsString("Trier par client")))
                .andExpect(content().string(containsString(colis.getReference())))
                .andExpect(content().string(containsString("À livrer")));
        mvc.perform(get("/chauffeur").param("tri", "ordre")).andExpect(status().isOk());

        mvc.perform(get("/chauffeur/colis/" + colis.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Colis " + colis.getReference())))
                .andExpect(content().string(containsString(colis.getCommande().getReference())))
                .andExpect(content().string(containsString("Pièces emballées")))
                .andExpect(content().string(containsString("Voir sur la carte")));
        mvc.perform(get("/chauffeur/colis/999999")).andExpect(redirectedUrl("/chauffeur"))
                .andExpect(flash().attributeExists("erreur"));

        mvc.perform(get("/chauffeur/carte").param("client", colis.getClient().getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Voir l'ordre de passage")));
    }

    @Test
    @WithMockUser(username = "chauffeur01", roles = "CHAUFFEUR")
    void preparerLaTourneeCalculOrdreManuelEtValidation() throws Exception {
        Tournee tournee = tournees.courante();
        Long id = tournee.getId();
        mvc.perform(get("/chauffeur/boucle")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Boucle calculée")))
                .andExpect(content().string(containsString("Calculer la boucle")))
                .andExpect(content().string(containsString("Ordre manuel")))
                .andExpect(content().string(containsString("Valider la boucle")));

        mvc.perform(post("/chauffeur/tournees/" + id + "/calcul").with(csrf()).param("algorithme", "EXACT_MAISON"))
                .andExpect(redirectedUrl("/chauffeur/boucle"))
                .andExpect(flash().attribute("succes", containsString("km")));

        // ordre manuel : échanger les deux premiers arrêts (mêmes masses) est accepté, les deux suivants non
        mvc.perform(get("/chauffeur/ordre")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Enregistrer l'ordre")));
        List<Long> ordre = new ArrayList<>(tournee.getArrets().stream().map(a -> a.getClient().getId()).toList());
        Collections.swap(ordre, 0, 1);
        mvc.perform(post("/chauffeur/tournees/" + id + "/ordre").with(csrf()).param("clientIds", texte(ordre)))
                .andExpect(redirectedUrl("/chauffeur/boucle"))
                .andExpect(flash().attributeExists("succes"));
        assertThat(tournee.getArrets().getFirst().getClient().getId()).isEqualTo(ordre.getFirst());
        Collections.swap(ordre, 1, 2);
        mvc.perform(post("/chauffeur/tournees/" + id + "/ordre").with(csrf()).param("clientIds", texte(ordre)))
                .andExpect(redirectedUrl("/chauffeur/ordre"))
                .andExpect(flash().attribute("erreur", containsString("Ordre impossible")));

        mvc.perform(post("/chauffeur/tournees/" + id + "/validation").with(csrf()))
                .andExpect(redirectedUrl("/chauffeur/boucle"));
        assertThat(tournee.getStatut()).isEqualTo(StatutTournee.VALIDEE);
        mvc.perform(get("/chauffeur/boucle")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Validée")))
                .andExpect(content().string(not(containsString("Valider la boucle"))));
        mvc.perform(get("/chauffeur/ordre")).andExpect(redirectedUrl("/chauffeur/boucle"));
        assertThat(tournee.getArrets()).extracting(Arret::getOrdre).startsWith(1);
    }

    private static String[] texte(List<Long> ids) {
        return ids.stream().map(String::valueOf).toArray(String[]::new);
    }
}
