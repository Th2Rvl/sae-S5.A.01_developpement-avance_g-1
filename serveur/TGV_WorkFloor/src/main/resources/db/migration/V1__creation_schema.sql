-- V1 : création du schéma initial
CREATE TABLE client (
                        id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                        nom         VARCHAR(100)  NOT NULL,
                        adresse     VARCHAR(255)  NOT NULL,
                        latitude    DECIMAL(9,6)  NOT NULL,
                        longitude   DECIMAL(9,6)  NOT NULL
);