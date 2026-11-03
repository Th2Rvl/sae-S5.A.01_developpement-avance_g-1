# Base de données

Base **MySQL 8.4** du projet, exécutée dans un conteneur Docker dans tous les environnements.

## Environnements

| Environnement | Base | Où | Branche Git associée |
|---|---|---|---|
| Développement | `tgv_local` | PC du développeur (Docker Desktop) | `feature-*` |
| Recette | `tgv_recette` | VM du projet | `api-dev` |
| Production | `tgv_prod` | VM du projet | `main` |

Le schéma n'est jamais modifié à la main : il évolue par des **migrations Flyway** versionnées
(`bdd/migrations/V<n>__<description>.sql`), appliquées automatiquement au démarrage de l'API
dans chaque environnement.

Au build, Maven copie `bdd/migrations/` dans le jar sous `db/migration` (bloc `<resources>` de
`serveur/TGV_WorkFloor/pom.xml`) : ce dossier reste la seule source, également lue par la CI
(`.github/workflows/bdd.yml`) et par le service `flyway` du compose. Flyway s'exécute avant
Hibernate, dont le `ddl-auto: validate` vérifie ensuite que les entités correspondent au schéma.
Les tests (profil `test`, base H2) désactivent Flyway : les migrations MySQL sont vérifiées par la CI.

## Base de développement locale

Le fichier de configuration est [`docker/compose.dev.yml`](../docker/compose.dev.yml).
Toutes les commandes se lancent **depuis la racine du dépôt**.

### Démarrer

```bash
docker compose -f docker/compose.dev.yml up -d
```

### Vérifier l'état

```bash
docker compose -f docker/compose.dev.yml ps
```

Attendre que la colonne `STATUS` indique `healthy` (≈ 30 s au premier lancement).

### Arrêter (les données sont conservées)

```bash
docker compose -f docker/compose.dev.yml stop
```

### Réinitialiser (efface toutes les données)

```bash
docker compose -f docker/compose.dev.yml down -v
docker compose -f docker/compose.dev.yml up -d
```

La base repart vide ; les migrations Flyway seront rejouées au prochain démarrage de l'API.

### Consulter les logs

```bash
docker compose -f docker/compose.dev.yml logs -f bdd
```

### Paramètres de connexion

| Paramètre | Valeur |
|---|---|
| Hôte | `localhost` |
| Port | `3306` |
| Base | `tgv_local` |
| Utilisateur applicatif | `tgv_app` |
| Mot de passe | `tgv_app` |

Ces identifiants ne concernent **que** la base locale, sans données réelles. Ils peuvent être
surchargés par un fichier `docker/.env` (non commité) définissant `MYSQL_ROOT_PASSWORD`,
`MYSQL_DATABASE`, `MYSQL_USER` et `MYSQL_PASSWORD`.

Accès en ligne de commande, sans client externe :

```bash
docker exec -it tgv-bdd-dev mysql -u tgv_app -p tgv_local
```

## Bases de recette et de production

Elles tournent sur la VM du projet. Le port MySQL n'y est publié que sur la boucle locale
(`127.0.0.1`) : **aucun accès direct depuis Internet**. L'accès d'administration se fait via un
**tunnel SSH** (par exemple l'onglet *SSH/SSL* de DataGrip), avec les identifiants transmis
hors du dépôt. Les mots de passe sont stockés dans le fichier `.env` de la VM, jamais dans Git.

## Dépannage

| Symptôme | Cause probable | Solution |
|---|---|---|
| `port is already allocated` au démarrage | un autre MySQL utilise déjà le port 3306 | arrêter l'autre instance, ou changer le port de gauche dans `compose.dev.yml` (ex. `127.0.0.1:3307:3306`) |
| `Access denied` à la connexion | identifiants incorrects, ou `.env` modifié après la création du volume | vérifier les valeurs ; les identifiants ne sont lus qu'à la création : réinitialiser avec `down -v` |
| `The system cannot find the path specified` | commande lancée hors de la racine du dépôt | se placer à la racine du dépôt |
| Le conteneur reste en `starting` | initialisation en cours | patienter ; consulter les logs |
