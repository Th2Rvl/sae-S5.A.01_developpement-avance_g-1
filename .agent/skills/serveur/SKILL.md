---
name: serveur
description: Conventions du serveur (serveur/TGV_WorkFloor/) - API REST Spring Boot, interface web Thymeleaf, migrations Flyway, Docker et CI. À utiliser pour tout code Java serveur, template, SQL, Dockerfile, compose ou workflow.
---

# Serveur - serveur/TGV_WorkFloor/

Spring Boot 4.1 / Java 25 / Maven, package `com.tgv.tgv_workfloor`, MySQL 8.4 via JPA.

## Arborescence Java
```
controller/vue/   @Controller Thymeleaf (interface Atelier), renvoie "app"
controller/api/   @RestController sous /api/** (consommé par le mobile), renvoie des DTO
service/          logique métier + @Transactional (l'optimisation des tournées aussi)
repository/       interfaces Spring Data JPA
model/ (+enums/)  entités JPA, alignées sur les migrations
dto/ + mapper/    entrées/sorties de l'API ; une entité ne sort jamais de l'API
exception/        exceptions métier + @ControllerAdvice
config/, security/
```
Règles : les contrôleurs ne contiennent aucune logique et passent par un service, qui seul
accède aux repositories. Injection par constructeur, pas de `@Autowired` sur un champ.

## Interface web (Thymeleaf)
- `templates/app.html` est le layout (header + nav) et insère la feature via
  `th:replace="~{${feature} :: contenu}"`.
- `templates/feature/<nomCamelCase>/` : une page par feature, avec un fragment `contenu`.
- `templates/component/...` : fragments réutilisables à paramètres (`th:fragment="x(a, b)"`).
- `static/css|js|img/` pour les ressources.
- La valeur de `feature` est une constante du contrôleur, jamais une donnée de l'URL.

## Base de données (Flyway)
- Migrations dans `src/main/resources/db/migration/V<n>__description.sql`, appliquées
  au démarrage de l'API.
- Jamais de `CREATE DATABASE` ni de `USE` : c'est l'URL de connexion qui choisit la base.
- Une migration fusionnée dans `main` ne se modifie plus : on en ajoute une nouvelle.
- `ddl-auto: validate` : toute entité doit correspondre exactement au schéma.
- Base locale : `docker compose -f docker/compose.dev.yml up -d` (depuis la racine).

## Tests et qualité
- JUnit 5 : `@WebMvcTest` pour les contrôleurs, tests unitaires pour les services.
- Le profil `test` utilise H2 et désactive Flyway ; les migrations sont testées par la CI
  (`.github/workflows/bdd.yml`).
- La couverture (JaCoCo) et l'analyse statique doivent respecter la DoD.
- Depuis `serveur/TGV_WorkFloor/` : `./mvnw test`, puis `./mvnw package`.
- Image Docker : `docker build -t tgv-workfloor serveur/TGV_WorkFloor`.

## Configuration
Les secrets passent par des variables d'environnement (`DB_URL`, `DB_USER`, `DB_PASSWORD`),
jamais en clair dans `application.yaml`.
