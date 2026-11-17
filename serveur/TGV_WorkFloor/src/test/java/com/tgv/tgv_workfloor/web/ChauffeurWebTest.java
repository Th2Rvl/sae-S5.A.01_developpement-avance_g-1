package com.tgv.tgv_workfloor.web;

import com.tgv.tgv_workfloor.depot.TourneeDepot;
import com.tgv.tgv_workfloor.modele.Arret;
import com.tgv.tgv_workfloor.modele.Colis;
import com.tgv.tgv_workfloor.modele.StatutColis;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Espace chauffeur du site (téléphone) : parcours complet d'une tournée. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ChauffeurWebTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private TourneeService tournees;
    @Autowired
    private TourneeDepot depot;

    @Test
    void chacunArriveDansSonEspaceApresConnexion() throws Exception {
        mvc.perform(formLogin("/connexion").userParameter("identifiant").passwordParam("motDePasse")
                .user("chauffeur01").password("1234")).andExpect(redirectedUrl("/chauffeur"));
        mvc.perform(formLogin("/connexion").userParameter("identifiant").passwordParam("motDePasse")
                .user("atelier").password("atelier")).andExpect(redirectedUrl("/atelier"));
    }

    @Test
    @WithMockUser(username = "atelier", roles = "ATELIER")
    void lAtelierNAccedePasALEspaceChauffeur() throws Exception {
        mvc.perform(get("/chauffeur")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "chauffeur01", roles = "CHAUFFEUR")
    void parcoursCompletDepuisLeTelephone() throws Exception {
        Tournee tournee = tournees.courante();
        Long id = tournee.getId();
        mvc.perform(get("/chauffeur")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Ma tournée")))
                .andExpect(content().string(containsString("Valider et partir")))
                .andExpect(content().string(containsString("Masses identiques")));

        // permuter deux arrêts du même groupe de masses : accepté ; descendre le dernier : refusé
        Long premier = tournee.getArrets().getFirst().getClient().getId();
        mvc.perform(post("/chauffeur/tournees/" + id + "/arrets/1/deplacement").with(csrf()).param("sens", "1"))
                .andExpect(flash().attributeExists("succes"));
        assertThat(tournee.getArrets().get(1).getClient().getId()).isEqualTo(premier);
        mvc.perform(post("/chauffeur/tournees/" + id + "/arrets/2/deplacement").with(csrf()).param("sens", "1"))
                .andExpect(flash().attribute("erreur", containsString("Ordre impossible")));
        mvc.perform(post("/chauffeur/tournees/" + id + "/arrets/7/deplacement").with(csrf()).param("sens", "1"))
                .andExpect(flash().attributeExists("erreur"));

        mvc.perform(post("/chauffeur/tournees/" + id + "/calcul").with(csrf()).param("algorithme", "EXACT_MAISON"))
                .andExpect(flash().attribute("succes", containsString("km")));
        mvc.perform(post("/chauffeur/tournees/" + id + "/validation").with(csrf()))
                .andExpect(redirectedUrl("/chauffeur"));
        assertThat(tournee.getStatut()).isEqualTo(StatutTournee.VALIDEE);
        mvc.perform(get("/chauffeur")).andExpect(content().string(containsString("Établir le bon de livraison")));

        Arret a1 = tournee.getArrets().get(0);
        Arret a2 = tournee.getArrets().get(1);
        List<Colis> colisA1 = tournee.colisDuClient(a1.getClient());
        mvc.perform(post("/chauffeur/tournees/" + id + "/bons").with(csrf())
                        .param("clientId", a1.getClient().getId().toString())
                        .param("colisIds", colisA1.getFirst().getId().toString()).param("etat", "CONFORME"))
                .andExpect(flash().attribute("succes", containsString("BL-")));
        assertThat(colisA1.getFirst().getStatut()).isEqualTo(StatutColis.LIVRE);
        // réserves sans commentaire, puis aucun colis coché : refusés
        mvc.perform(post("/chauffeur/tournees/" + id + "/bons").with(csrf())
                        .param("clientId", a2.getClient().getId().toString())
                        .param("colisIds", tournee.colisDuClient(a2.getClient()).getFirst().getId().toString())
                        .param("etat", "RESERVES"))
                .andExpect(flash().attribute("erreur", containsString("réserves")));
        mvc.perform(post("/chauffeur/tournees/" + id + "/bons").with(csrf())
                        .param("clientId", a2.getClient().getId().toString()).param("etat", "CONFORME"))
                .andExpect(flash().attribute("erreur", containsString("Cochez")));

        long avant = depot.count();
        mvc.perform(post("/chauffeur/tournees/" + id + "/fin").with(csrf()))
                .andExpect(flash().attribute("succes", containsString("reportés au jeudi suivant")));
        assertThat(tournee.getStatut()).isEqualTo(StatutTournee.TERMINEE);
        // les colis non livrés partent dans une nouvelle tournée, le jeudi d'après
        assertThat(depot.count()).isEqualTo(avant + 1);
        Tournee suivante = tournees.courante();
        assertThat(suivante.getDate()).isEqualTo(tournee.getDate().plusWeeks(1));
        assertThat(suivante.getColis()).isNotEmpty().allMatch(Colis::isReporte);
        mvc.perform(get("/chauffeur")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Reporté")));
    }
}
