# syntax=docker/dockerfile:1
# =============================================================================
# Image de l'API TGV_WorkFloor (Spring Boot + Thymeleaf + Flyway)
# Construire depuis la RACINE du dépôt (le pom copie aussi ../../bdd/migrations) :
#   docker build -f serveur/TGV_WorkFloor/Dockerfile -t tgv-workfloor .
# =============================================================================

# ---- Étape 1 : compilation (JDK + Maven, jetée ensuite) ---------------------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

# Même arborescence que le dépôt, pour que ../../bdd/migrations soit trouvé
COPY bdd/migrations bdd/migrations
COPY serveur/TGV_WorkFloor/pom.xml serveur/TGV_WorkFloor/pom.xml
COPY serveur/TGV_WorkFloor/src serveur/TGV_WorkFloor/src

WORKDIR /build/serveur/TGV_WorkFloor
# Cache Maven conservé entre deux builds ; tests sautés (ils tournent dans la CI)
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q package -DskipTests \
 && cp target/*.jar /build/app.jar

# ---- Étape 2 : exécution (JRE seul, image plus légère) ----------------------
FROM eclipse-temurin:25-jre
WORKDIR /app

# L'API ne tourne pas en root
RUN useradd --system --no-create-home --shell /usr/sbin/nologin spring
COPY --from=build /build/app.jar app.jar
USER spring

EXPOSE 8080
# Limite la mémoire Java à 75 % de celle donnée au conteneur (limite du compose)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]