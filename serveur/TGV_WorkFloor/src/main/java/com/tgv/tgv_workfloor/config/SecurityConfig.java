package com.tgv.tgv_workfloor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import java.nio.charset.StandardCharsets;

/**
 * Deux chaînes de sécurité :
 * <ul>
 *   <li>/api/** (application mobile, Volley) : sans état, authentification HTTP Basic à chaque requête,
 *       réponses d'erreur en JSON ;</li>
 *   <li>le reste du site : l'espace chauffeur (maquette 01 · « Connexion chauffeur »), formulaire de connexion
 *       et session, avec « Mémoriser ces informations » (cookie remember-me). Seuls les chauffeurs peuvent
 *       s'y connecter.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    private static final String COOKIE_MEMORISER = "remember-me";

    private static boolean estChauffeur(Authentication authentification) {
        return authentification != null && authentification.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CHAUFFEUR".equals(a.getAuthority()));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    SecurityFilterChain securiteApi(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint nonAuthentifie = (requete, reponse, exception) -> {
            reponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            reponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            reponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
            reponse.getWriter().write("{\"statut\":401,\"message\":\"Identifiant ou mot de passe incorrect\"}");
        };
        http.securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST, "/api/auth/connexion").permitAll()
                        .anyRequest().hasAnyRole("CHAUFFEUR", "ATELIER"))
                .httpBasic(basic -> basic.authenticationEntryPoint(nonAuthentifie))
                .exceptionHandling(e -> e.authenticationEntryPoint(nonAuthentifie));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain securiteWeb(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(a -> a
                        .requestMatchers("/", "/connexion", "/css/**", "/js/**", "/images/**", "/favicon.ico", "/error")
                        .permitAll()
                        .requestMatchers("/chauffeur/**").hasRole("CHAUFFEUR")
                        .anyRequest().authenticated())
                .formLogin(f -> f
                        .loginPage("/connexion")
                        .loginProcessingUrl("/connexion")
                        .usernameParameter("identifiant")
                        .passwordParameter("motDePasse")
                        .successHandler((requete, reponse, authentification) -> {
                            if (estChauffeur(authentification)) {
                                reponse.sendRedirect(requete.getContextPath() + "/chauffeur");
                                return;
                            }
                            // un compte atelier ne peut pas ouvrir de session sur l'espace chauffeur
                            new SecurityContextLogoutHandler().logout(requete, reponse, authentification);
                            new CookieClearingLogoutHandler(COOKIE_MEMORISER).logout(requete, reponse, authentification);
                            reponse.sendRedirect(requete.getContextPath() + "/connexion?nonChauffeur");
                        })
                        .failureUrl("/connexion?erreur")
                        .permitAll())
                .rememberMe(r -> r
                        .key("tgv-espace-chauffeur")
                        .rememberMeParameter("memoriser")
                        .rememberMeCookieName(COOKIE_MEMORISER))
                .logout(l -> l
                        .logoutUrl("/deconnexion")
                        .logoutSuccessUrl("/connexion?deconnecte"));
        return http.build();
    }
}
