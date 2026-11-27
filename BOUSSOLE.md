# Boussole du projet T.G.V

SAE S5.A.01 : application de gestion des tournées de livraison de l'entreprise T.G.V
(Tournée Générale Véloce), une TPE qui livre chaque jeudi des pièces de grand gabarit
avec un seul camion-plateau.

- **Interface Atelier** (web, PC) : clients, commandes, pièces et leur état, suivi des bons
  de livraison, consultation des boucles (trajet, km).
- **Interface Livraison** (mobile, chauffeur) : colis à charger, carte des clients, boucle
  manuelle ou calculée, ordre de chargement, validation des bons de livraison.
- **Optimisation** : boucle la plus courte avec chargement par masse décroissante (le dessus
  du camion est livré en premier). Le calcul se fait **sur le serveur**, jamais sur le mobile.

Architecture N-tiers : mobile Android → API Spring Boot (Docker) → MySQL 8.4.

## Quel skill pour quel dossier

**Avant de travailler dans un dossier, lis le skill correspondant** (fichier `SKILL.md`).

| Dossier | Contenu | Skill à lire |
|---|---|---|
| `mobile-livraison/` | App Android du chauffeur (Java) | `.agent/skills/android/SKILL.md` |
| `serveur/TGV_WorkFloor/` | API REST + interface web Thymeleaf + migrations Flyway | `.agent/skills/serveur/SKILL.md` |
| `docker/` | Compose de dev (MySQL, Flyway) | `.agent/skills/serveur/SKILL.md` |
| `.github/workflows/` | CI/CD GitHub Actions | `.agent/skills/serveur/SKILL.md` |
| `docs/` | Documentation, suivi IA | — |
| Toute opération Git (branche, commit, PR) | | `.agent/skills/git-flow/SKILL.md` |

## Règles communes

- Code, commentaires, commits et docs en **français**.
- L'agent IA ne commite, ne pousse et ne fusionne jamais : il propose branche, message et
  commandes (voir `.agent/skills/git-flow/SKILL.md`).
- Ne jamais modifier une migration `V<n>__*.sql` déjà fusionnée dans `main`.
- Aucun secret dans le dépôt : variables d'environnement ou secrets GitHub.
- Tout usage de l'IA est tracé dans `docs/suivi_usage_ia.md` (tâche, durée, tokens),
  exigence IA001-IA003 du sujet.
