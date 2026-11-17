package com.tgv.tgv_workfloor.web;

import com.tgv.tgv_workfloor.depot.ClientDepot;
import com.tgv.tgv_workfloor.depot.CommandeDepot;
import com.tgv.tgv_workfloor.depot.TourneeDepot;
import com.tgv.tgv_workfloor.modele.Client;
import com.tgv.tgv_workfloor.modele.Commande;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rendu des pages Atelier et principaux formulaires. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AtelierWebTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ClientDepot clients;
    @Autowired
    private CommandeDepot commandes;
    @Autowired
    private TourneeDepot tournees;

    @Test
    void sansConnexionOnEstRedirigeVersLaPageDeConnexion() throws Exception {
        mvc.perform(get("/atelier")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/connexion"));
        mvc.perform(get("/connexion")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Connexion atelier")));
    }

    @Test
    @WithMockUser(roles = "CHAUFFEUR")
    void unChauffeurNAccedePasALAtelier() throws Exception {
        mvc.perform(get("/atelier")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "atelier", roles = "ATELIER")
    void toutesLesPagesSAffichent() throws Exception {
        Client client = clients.findAll().getFirst();
        Commande commande = commandes.findAll().getFirst();
        Long piece = commande.getPieces().getFirst().getId();
        for (String url : new String[]{"/atelier", "/atelier/clients", "/atelier/clients/nouveau",
                "/atelier/clients/" + client.getId(), "/atelier/commandes", "/atelier/commandes?etat=FABRIQUEE",
                "/atelier/commandes/nouvelle", "/atelier/commandes/" + commande.getId(),
                "/atelier/commandes/" + commande.getId() + "/modification", "/atelier/commandes/pieces/" + piece,
                "/atelier/tournees", "/atelier/bons", "/atelier/utilisateurs"}) {
            mvc.perform(get(url)).andExpect(status().isOk());
        }
        tournees.findAll().forEach(t -> {
            try {
                mvc.perform(get("/atelier/tournees/" + t.getId())).andExpect(status().isOk())
                        .andExpect(content().string(containsString("Plan de chargement")));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
        mvc.perform(get("/atelier/clients/99999")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "atelier", roles = "ATELIER")
    void creerUnClientPuisUneCommandeAvecUnePieceEtUnColis() throws Exception {
        long avant = clients.count();
        mvc.perform(post("/atelier/clients/nouveau").with(csrf())
                        .param("nom", "Forge Test").param("adresse", "1 rue du Test").param("codePostal", "12000")
                        .param("ville", "Rodez").param("telephone", "").param("latitude", "44.35")
                        .param("longitude", "2.57"))
                .andExpect(redirectedUrl("/atelier/clients"));
        assertThat(clients.count()).isEqualTo(avant + 1);
        Client client = clients.findAll().getLast();

        mvc.perform(post("/atelier/clients/nouveau").with(csrf()).param("nom", "").param("codePostal", "12"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("5 chiffres attendus")));

        mvc.perform(post("/atelier/commandes/nouvelle").with(csrf())
                        .param("clientId", client.getId().toString()).param("datePrevue", "2030-01-03"))
                .andExpect(status().is3xxRedirection());
        Commande commande = commandes.findByClient(client).getFirst();

        mvc.perform(post("/atelier/commandes/" + commande.getId() + "/pieces").with(csrf())
                        .param("designation", "Bride").param("poidsKg", "12.5").param("etat", "FABRIQUEE"))
                .andExpect(flash().attributeExists("succes"));
        Long piece = commande.getPieces().getFirst().getId();

        mvc.perform(post("/atelier/commandes/" + commande.getId() + "/colis").with(csrf())
                        .param("pieceIds", piece.toString()).param("longueurCm", "50").param("largeurCm", "40")
                        .param("hauteurCm", "30"))
                .andExpect(flash().attributeExists("succes"));
        assertThat(commande.getColis()).hasSize(1);

        // un client qui a des commandes ne peut pas être supprimé
        mvc.perform(post("/atelier/clients/" + client.getId() + "/suppression").with(csrf()))
                .andExpect(flash().attributeExists("erreur"));
        mvc.perform(post("/atelier/commandes/" + commande.getId() + "/suppression").with(csrf()))
                .andExpect(redirectedUrl("/atelier/commandes"));
        mvc.perform(post("/atelier/clients/" + client.getId() + "/suppression").with(csrf()))
                .andExpect(flash().attributeExists("succes"));
    }

    @Test
    @WithMockUser(username = "atelier", roles = "ATELIER")
    void recalculerLaBoucleDepuisLAtelier() throws Exception {
        Long id = tournees.findAll().stream().filter(t -> t.getStatut().name().equals("PREVUE")).findFirst()
                .orElseThrow().getId();
        mvc.perform(post("/atelier/tournees/" + id + "/calcul").with(csrf())
                        .param("algorithme", "EXACT_BIBLIOTHEQUE"))
                .andExpect(flash().attributeExists("succes"));
        mvc.perform(get("/atelier/tournees/" + id))
                .andExpect(content().string(containsString("Exacte — bibliothèque")))
                .andExpect(content().string(containsString("Masses identiques")));
    }
}
