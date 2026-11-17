package com.tgv.tgv_workfloor.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Parcours complet de l'application mobile sur le jeu de données statique. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TourneeApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void laConnexionVerifieLeMotDePasse() throws Exception {
        mvc.perform(post("/api/auth/connexion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifiant\":\"chauffeur01\",\"motDePasse\":\"1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifiant", is("chauffeur01")))
                .andExpect(jsonPath("$.role", is("CHAUFFEUR")));
        mvc.perform(post("/api/auth/connexion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifiant\":\"chauffeur01\",\"motDePasse\":\"faux\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statut", is(401)));
    }

    @Test
    void lApiExigeUneAuthentification() throws Exception {
        mvc.perform(get("/api/tournees/courante")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/tournees/courante").with(httpBasic("chauffeur01", "mauvais")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/moi").with(httpBasic("chauffeur01", "1234")))
                .andExpect(jsonPath("$.nom", is("Chauffeur 01")));
    }

    @Test
    void laTourneeCouranteReprendLesMaquettes() throws Exception {
        mvc.perform(get("/api/tournees/courante").with(httpBasic("chauffeur01", "1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nbColis", is(12)))
                .andExpect(jsonPath("$.nbClients", is(7)))
                .andExpect(jsonPath("$.poidsTotalKg", is(1725.0)))
                .andExpect(jsonPath("$.statut", is("PREVUE")))
                .andExpect(jsonPath("$.algorithme", is("EXACT_MAISON")))
                .andExpect(jsonPath("$.optimaliteProuvee", is(true)))
                .andExpect(jsonPath("$.arrets", hasSize(7)))
                // groupes imposés par les masses : 45 kg (2 clients), 110 kg (3 clients), 260 kg (2 clients)
                .andExpect(jsonPath("$.arrets[0].client.nom", anyOf(is("Garage Fabre PL"), is("Métallerie Causse"))))
                .andExpect(jsonPath("$.arrets[0].groupe", is(1)))
                .andExpect(jsonPath("$.arrets[0].permutable", is(true)))
                .andExpect(jsonPath("$.arrets[1].groupe", is(1)))
                .andExpect(jsonPath("$.arrets[2].groupe", is(2)))
                .andExpect(jsonPath("$.arrets[4].groupe", is(2)))
                .andExpect(jsonPath("$.arrets[5].groupe", is(3)))
                .andExpect(jsonPath("$.arrets[6].client.nom", anyOf(is("Ateliers du Lévézou"), is("SARL Vabre Agri"))))
                .andExpect(jsonPath("$.chargement", hasSize(12)))
                .andExpect(jsonPath("$.chargement[0].poidsKg", is(260.0)))
                .andExpect(jsonPath("$.chargement[11].poidsKg", is(45.0)));
    }

    @Test
    void leDetailDUnColisDonneSesPiecesEtSonClient() throws Exception {
        String json = mvc.perform(get("/api/tournees/courante").with(httpBasic("chauffeur01", "1234")))
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(json.replaceAll("(?s).*\"id\":(\\d+),\"reference\":\"C-0412\".*", "$1"));
        mvc.perform(get("/api/colis/" + id).with(httpBasic("chauffeur01", "1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commandeReference", is("CMD-0187")))
                .andExpect(jsonPath("$.poidsKg", is(45.0)))
                .andExpect(jsonPath("$.pieces", hasSize(3)))
                .andExpect(jsonPath("$.client.nom", is("Métallerie Causse")))
                .andExpect(jsonPath("$.numeroArret", anyOf(is(1), is(2))));
        mvc.perform(get("/api/colis/99999").with(httpBasic("chauffeur01", "1234")))
                .andExpect(status().isNotFound());
    }

    @Test
    void chaqueAlgorithmeCalculeLaBoucle() throws Exception {
        long id = idTourneeCourante();
        mvc.perform(get("/api/tournees/" + id + "/difficultes").with(httpBasic("chauffeur01", "1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimations", hasSize(3)))
                .andExpect(jsonPath("$.nbOrdresPossibles", is(24.0)))
                .andExpect(jsonPath("$.conflitsDeMasses", hasSize(0)));
        java.util.Map<String, Double> distances = new java.util.HashMap<>();
        for (String algo : new String[]{"EXACT_MAISON", "EXACT_BIBLIOTHEQUE", "APPROCHE_MAISON"}) {
            String json = mvc.perform(post("/api/tournees/" + id + "/calcul").with(httpBasic("chauffeur01", "1234"))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"algorithme\":\"" + algo + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.algorithme", is(algo)))
                    .andExpect(jsonPath("$.arrets", hasSize(7)))
                    .andExpect(jsonPath("$.chargement[0].poidsKg", is(260.0)))
                    .andReturn().getResponse().getContentAsString();
            distances.put(algo, Double.valueOf(json.replaceAll("(?s).*?\"distanceKm\":([0-9.]+).*", "$1")));
        }
        org.assertj.core.api.Assertions.assertThat(distances.get("EXACT_BIBLIOTHEQUE"))
                .isEqualTo(distances.get("EXACT_MAISON"));
        org.assertj.core.api.Assertions.assertThat(distances.get("APPROCHE_MAISON"))
                .isGreaterThanOrEqualTo(distances.get("EXACT_MAISON"));
        mvc.perform(post("/api/tournees/" + id + "/calcul").with(httpBasic("chauffeur01", "1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"algorithme\":\"INCONNU\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void parcoursCompletOrdreManuelValidationBonsEtFin() throws Exception {
        long id = idTourneeCourante();
        String json = mvc.perform(get("/api/tournees/" + id).with(httpBasic("chauffeur01", "1234")))
                .andReturn().getResponse().getContentAsString();
        java.util.List<Long> clients = new java.util.ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"ordre\":\\d+,\"client\":\\{\"id\":(\\d+)").matcher(json);
        while (m.find()) {
            clients.add(Long.valueOf(m.group(1)));
        }
        // ordre inversé : refusé, les clients aux colis lourds ne peuvent pas passer avant les plus légers
        java.util.List<Long> inverse = new java.util.ArrayList<>(clients);
        java.util.Collections.reverse(inverse);
        mvc.perform(put("/api/tournees/" + id + "/ordre").with(httpBasic("chauffeur01", "1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"clientIds\":" + inverse + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Ordre impossible")));
        mvc.perform(put("/api/tournees/" + id + "/ordre").with(httpBasic("chauffeur01", "1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"clientIds\":[" + clients.get(0) + "]}"))
                .andExpect(status().isConflict());

        // permuter les deux premiers arrêts (même groupe de masses) : accepté
        java.util.Collections.swap(clients, 0, 1);
        mvc.perform(put("/api/tournees/" + id + "/ordre").with(httpBasic("chauffeur01", "1234"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"clientIds\":" + clients + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithme", is("MANUEL")))
                .andExpect(jsonPath("$.arrets[0].client.id", is(clients.get(0).intValue())))
                .andExpect(jsonPath("$.chargement[11].poidsKg", is(45.0)));

        mvc.perform(post("/api/tournees/" + id + "/validation").with(httpBasic("chauffeur01", "1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("VALIDEE")));

        mvc.perform(post("/api/tournees/" + id + "/bons").with(httpBasic("chauffeur01", "1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + clients.get(0) + ",\"etat\":\"CONFORME\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero", startsWith("BL-")))
                .andExpect(jsonPath("$.numero", containsString("-01")));
        mvc.perform(post("/api/tournees/" + id + "/bons").with(httpBasic("chauffeur01", "1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + clients.get(0) + ",\"etat\":\"CONFORME\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/tournees/" + id + "/bons").with(httpBasic("chauffeur01", "1234"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":" + clients.get(1) + ",\"etat\":\"RESERVES\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("réserves")));

        mvc.perform(get("/api/tournees/" + id).with(httpBasic("chauffeur01", "1234")))
                .andExpect(jsonPath("$.statut", is("EN_COURS")))
                .andExpect(jsonPath("$.arrets[0].livre", is(true)));

        mvc.perform(post("/api/tournees/" + id + "/fin").with(httpBasic("chauffeur01", "1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("TERMINEE")));
        mvc.perform(get("/api/tournees/courante").with(httpBasic("chauffeur01", "1234")))
                .andExpect(status().isNotFound());
    }

    private long idTourneeCourante() throws Exception {
        String json = mvc.perform(get("/api/tournees/courante").with(httpBasic("chauffeur01", "1234")))
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(json.replaceAll("^\\{\"id\":(\\d+),.*", "$1").replaceAll("(?s)^\\{\"id\":(\\d+),.*", "$1"));
    }
}
