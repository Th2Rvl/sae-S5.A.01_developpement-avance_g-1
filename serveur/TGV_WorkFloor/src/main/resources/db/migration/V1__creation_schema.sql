CREATE DATABASE IF NOT EXISTS tgv_local
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Suppression (permet de relancer le script)
SET FOREIGN_KEY_CHECKS = 0;
DROP VIEW  IF EXISTS V_POIDS_COMMANDE;
DROP VIEW  IF EXISTS V_POIDS_COLIS;
DROP TABLE IF EXISTS EXEMPLAIRE;
DROP TABLE IF EXISTS COLIS;
DROP TABLE IF EXISTS ETAPE;
DROP TABLE IF EXISTS TOURNEE;
DROP TABLE IF EXISTS COMMANDE;
DROP TABLE IF EXISTS PIECE_CATALOGUE;
DROP TABLE IF EXISTS CLIENT;
DROP TABLE IF EXISTS ATELIER;
DROP TABLE IF EXISTS CAMION;
DROP TABLE IF EXISTS UTILISATEUR;
DROP TABLE IF EXISTS PARAMETRE;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- PARAMETRE : règles métier modifiables sans changer le code
-- ---------------------------------------------------------------------
CREATE TABLE PARAMETRE (
    cle          VARCHAR(50)   NOT NULL,
    valeur       VARCHAR(100)  NOT NULL,
    description  VARCHAR(255)  NULL,
    CONSTRAINT pk_parametre PRIMARY KEY (cle)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- UTILISATEUR : opérateurs atelier et livreurs (authentification)
-- ---------------------------------------------------------------------
CREATE TABLE UTILISATEUR (
    id_utilisateur  INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    login           VARCHAR(50)   NOT NULL,
    mot_de_passe    VARCHAR(255)  NOT NULL,      -- hash (bcrypt/argon2), jamais en clair
    nom             VARCHAR(100)  NOT NULL,
    prenom          VARCHAR(100)  NOT NULL,
    role            ENUM('OPERATEUR', 'LIVREUR') NOT NULL,
    actif           BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_utilisateur PRIMARY KEY (id_utilisateur),
    CONSTRAINT uq_utilisateur_login UNIQUE (login)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- CAMION
-- ---------------------------------------------------------------------
CREATE TABLE CAMION (
    id_camion              INT UNSIGNED   NOT NULL AUTO_INCREMENT,
    immatriculation        VARCHAR(20)    NOT NULL,
    capacite_max_poids     DECIMAL(10,2)  NOT NULL,   -- kg
    capacite_max_nb_colis  INT UNSIGNED   NOT NULL,
    CONSTRAINT pk_camion PRIMARY KEY (id_camion),
    CONSTRAINT uq_camion_immat UNIQUE (immatriculation),
    CONSTRAINT ck_camion_poids    CHECK (capacite_max_poids > 0),
    CONSTRAINT ck_camion_nb_colis CHECK (capacite_max_nb_colis > 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- ATELIER (point de chargement ; plusieurs ateliers = extension prévue)
-- ---------------------------------------------------------------------
CREATE TABLE ATELIER (
    id_atelier   INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    nom_atelier  VARCHAR(100)  NOT NULL,
    adresse      VARCHAR(255)  NOT NULL,
    latitude     DECIMAL(9,6)  NULL,
    longitude    DECIMAL(9,6)  NULL,
    CONSTRAINT pk_atelier PRIMARY KEY (id_atelier)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- CLIENT (adresse de livraison unique)
-- ---------------------------------------------------------------------
CREATE TABLE CLIENT (
    id_client          INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    nom_client         VARCHAR(150)  NOT NULL,
    adresse_livraison  VARCHAR(255)  NOT NULL,
    latitude           DECIMAL(9,6)  NULL,
    longitude          DECIMAL(9,6)  NULL,
    CONSTRAINT pk_client PRIMARY KEY (id_client)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- PIECE_CATALOGUE : pièces que l'entreprise sait fabriquer
-- ---------------------------------------------------------------------
CREATE TABLE PIECE_CATALOGUE (
    id_piece_catalogue  INT UNSIGNED   NOT NULL AUTO_INCREMENT,
    reference           VARCHAR(50)    NOT NULL,
    designation         VARCHAR(150)   NOT NULL,
    poids               DECIMAL(10,2)  NOT NULL,   -- kg ; max (50) vérifié via PARAMETRE
    actif               BOOLEAN        NOT NULL DEFAULT TRUE,  -- retrait du catalogue sans perdre l'historique
    CONSTRAINT pk_piece_catalogue PRIMARY KEY (id_piece_catalogue),
    CONSTRAINT uq_piece_catalogue_ref UNIQUE (reference),
    CONSTRAINT ck_piece_catalogue_poids CHECK (poids > 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- COMMANDE
-- ---------------------------------------------------------------------
CREATE TABLE COMMANDE (
    id_commande  INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    id_client    INT UNSIGNED  NOT NULL,
    date_saisie  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_prevue  DATE          NOT NULL,           -- par défaut : jeudi suivant (calculé par l'API)
    etat         ENUM('EN_FABRICATION',
                      'PRETE_A_LIVRER',
                      'EN_COURS_LIVRAISON',
                      'LIVREE',
                      'PARTIELLEMENT_LIVREE') NOT NULL DEFAULT 'EN_FABRICATION',
    CONSTRAINT pk_commande PRIMARY KEY (id_commande),
    CONSTRAINT fk_commande_client FOREIGN KEY (id_client)
        REFERENCES CLIENT (id_client)
        ON DELETE RESTRICT,
    INDEX idx_commande_etat (etat)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- TOURNEE
-- ---------------------------------------------------------------------
CREATE TABLE TOURNEE (
    id_tournee         INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    id_livreur         INT UNSIGNED  NOT NULL,     -- UTILISATEUR de rôle LIVREUR (vérifié par l'API)
    id_camion          INT UNSIGNED  NOT NULL,
    id_atelier_depart  INT UNSIGNED  NOT NULL,
    date_tournee       DATE          NOT NULL,
    statut             ENUM('EN_PREPARATION',
                            'BOUCLE_VALIDEE',
                            'EN_COURS',
                            'TERMINEE') NOT NULL DEFAULT 'EN_PREPARATION',
    algorithme         ENUM('MANUEL',
                            'EXACT_MAISON',
                            'EXACT_BIBLIOTHEQUE',
                            'APPROCHE') NULL,     -- NULL tant que la boucle n'est pas calculée
    temps_calcul_ms    INT UNSIGNED  NULL,         -- mesures pour le dossier qualité algorithmique
    distance_km        DECIMAL(8,2)  NULL,
    CONSTRAINT pk_tournee PRIMARY KEY (id_tournee),
    CONSTRAINT fk_tournee_livreur FOREIGN KEY (id_livreur)
        REFERENCES UTILISATEUR (id_utilisateur)
        ON DELETE RESTRICT,
    CONSTRAINT fk_tournee_camion FOREIGN KEY (id_camion)
        REFERENCES CAMION (id_camion)
        ON DELETE RESTRICT,
    CONSTRAINT fk_tournee_atelier FOREIGN KEY (id_atelier_depart)
        REFERENCES ATELIER (id_atelier)
        ON DELETE RESTRICT,
    CONSTRAINT ck_tournee_distance CHECK (distance_km IS NULL OR distance_km >= 0),
    INDEX idx_tournee_date (date_tournee)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- ETAPE : un arrêt de la boucle (chez un client ou retour à un atelier)
-- Un même client peut apparaître plusieurs fois dans une boucle.
-- NB : MySQL interdit ON DELETE/ON UPDATE sur des colonnes utilisées
--      dans un CHECK -> pas d'action référentielle sur id_client/id_atelier.
-- ---------------------------------------------------------------------
CREATE TABLE ETAPE (
    id_etape       INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    id_tournee     INT UNSIGNED  NOT NULL,
    ordre          INT UNSIGNED  NOT NULL,
    type_etape     ENUM('CLIENT', 'ATELIER') NOT NULL,
    id_client      INT UNSIGNED  NULL,
    id_atelier     INT UNSIGNED  NULL,
    heure_passage  DATETIME      NULL,             -- renseignée par l'appli mobile
    CONSTRAINT pk_etape PRIMARY KEY (id_etape),
    CONSTRAINT uq_etape_ordre UNIQUE (id_tournee, ordre),
    CONSTRAINT uq_etape_tournee UNIQUE (id_etape, id_tournee),   -- cible de la FK composite de COLIS
    CONSTRAINT fk_etape_tournee FOREIGN KEY (id_tournee)
        REFERENCES TOURNEE (id_tournee)
        ON DELETE CASCADE,
    CONSTRAINT fk_etape_client  FOREIGN KEY (id_client)  REFERENCES CLIENT (id_client),
    CONSTRAINT fk_etape_atelier FOREIGN KEY (id_atelier) REFERENCES ATELIER (id_atelier),
    CONSTRAINT ck_etape_type CHECK (
        (type_etape = 'CLIENT'  AND id_client  IS NOT NULL AND id_atelier IS NULL) OR
        (type_etape = 'ATELIER' AND id_atelier IS NOT NULL AND id_client  IS NULL)
    )
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- COLIS : usage unique. Un colis refusé / client absent revient à
-- l'atelier et est détruit ; on le conserve en base pour l'historique.
-- Le bon de livraison est porté par le colis (1 bon par colis).
-- ---------------------------------------------------------------------
CREATE TABLE COLIS (
    id_colis          INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    id_commande       INT UNSIGNED  NOT NULL,
    id_tournee        INT UNSIGNED  NULL,          -- affecté par l'opérateur (préparation du mercredi)
    id_etape          INT UNSIGNED  NULL,          -- affecté une fois la boucle validée
    ordre_chargement  INT UNSIGNED  NULL,          -- 1 = chargé en premier (en bas de la pile)
    statut_livraison  ENUM('A_LIVRER',
                           'LIVRE',
                           'REFUSE',
                           'CLIENT_ABSENT') NOT NULL DEFAULT 'A_LIVRER',
    date_bon          DATETIME      NULL,          -- établissement du bon de livraison
    bon_synchronise   BOOLEAN       NOT NULL DEFAULT FALSE,   -- bon bien reçu par le serveur
    CONSTRAINT pk_colis PRIMARY KEY (id_colis),
    CONSTRAINT uq_colis_commande UNIQUE (id_colis, id_commande),     -- cible de la FK composite d'EXEMPLAIRE
    CONSTRAINT uq_colis_chargement UNIQUE (id_tournee, ordre_chargement),
    CONSTRAINT fk_colis_commande FOREIGN KEY (id_commande)
        REFERENCES COMMANDE (id_commande)
        ON DELETE CASCADE,
    CONSTRAINT fk_colis_tournee FOREIGN KEY (id_tournee)
        REFERENCES TOURNEE (id_tournee),
    -- garantit que l'étape appartient bien à la tournée du colis
    CONSTRAINT fk_colis_etape FOREIGN KEY (id_etape, id_tournee)
        REFERENCES ETAPE (id_etape, id_tournee),
    CONSTRAINT ck_colis_bon CHECK (statut_livraison = 'A_LIVRER' OR date_bon IS NOT NULL)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- EXEMPLAIRE : une pièce du catalogue fabriquée pour une commande
-- (une pièce commandée en 3 exemplaires = 3 lignes)
-- ---------------------------------------------------------------------
CREATE TABLE EXEMPLAIRE (
    id_exemplaire       INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    id_commande         INT UNSIGNED  NOT NULL,
    id_piece_catalogue  INT UNSIGNED  NOT NULL,
    id_colis            INT UNSIGNED  NULL,        -- NULL tant que non emballé
    etat                ENUM('A_FAIRE',
                             'EN_FABRICATION',
                             'FABRIQUEE',
                             'LIVREE',
                             'NON_LIVREE') NOT NULL DEFAULT 'A_FAIRE',
    date_fabrication    DATE          NULL,        -- priorisation des pièces en stock depuis longtemps
    CONSTRAINT pk_exemplaire PRIMARY KEY (id_exemplaire),
    CONSTRAINT fk_exemplaire_commande FOREIGN KEY (id_commande)
        REFERENCES COMMANDE (id_commande)
        ON DELETE CASCADE,
    CONSTRAINT fk_exemplaire_piece FOREIGN KEY (id_piece_catalogue)
        REFERENCES PIECE_CATALOGUE (id_piece_catalogue)
        ON DELETE RESTRICT,
    -- un exemplaire ne peut être emballé que dans un colis de SA commande
    CONSTRAINT fk_exemplaire_colis FOREIGN KEY (id_colis, id_commande)
        REFERENCES COLIS (id_colis, id_commande),
    CONSTRAINT ck_exemplaire_fabrication CHECK (
        etat IN ('A_FAIRE', 'EN_FABRICATION') OR date_fabrication IS NOT NULL
    ),
    INDEX idx_exemplaire_etat (etat)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Vues : poids calculés (aucune donnée redondante à maintenir)
-- ---------------------------------------------------------------------
CREATE VIEW V_POIDS_COLIS AS
SELECT c.id_colis,
       c.id_commande,
       COUNT(e.id_exemplaire)        AS nb_pieces,
       COALESCE(SUM(p.poids), 0)     AS poids_total
FROM COLIS c
LEFT JOIN EXEMPLAIRE e      ON e.id_colis = c.id_colis
LEFT JOIN PIECE_CATALOGUE p ON p.id_piece_catalogue = e.id_piece_catalogue
GROUP BY c.id_colis, c.id_commande;

CREATE VIEW V_POIDS_COMMANDE AS
SELECT co.id_commande,
       COUNT(e.id_exemplaire)        AS nb_pieces,
       COALESCE(SUM(p.poids), 0)     AS poids_total,
       MIN(e.date_fabrication)       AS plus_ancienne_fabrication
FROM COMMANDE co
LEFT JOIN EXEMPLAIRE e      ON e.id_commande = co.id_commande
LEFT JOIN PIECE_CATALOGUE p ON p.id_piece_catalogue = e.id_piece_catalogue
GROUP BY co.id_commande;

-- ---------------------------------------------------------------------
-- Paramètres par défaut (annexe du cahier des charges)
-- ---------------------------------------------------------------------
INSERT INTO PARAMETRE (cle, valeur, description) VALUES
    ('POIDS_MAX_COLIS_KG',       '50', 'Poids maximum d''un colis'),
    ('POIDS_MAX_PIECE_KG',       '50', 'Poids maximum d''une pièce du catalogue'),
    ('NB_MAX_COLIS_TOURNEE',     '8',  'Nombre maximum de colis chargés (à augmenter selon les perfs des algos)'),
    ('NB_MAX_PIECES_COMMANDE',   '8',  'Nombre maximum de pièces par commande saisie');
