---
name: git-flow
description: Workflow Git de l'équipe - nommage des branches par domaine, format des messages de commit, mise à jour depuis main et pull request. À utiliser avant de créer une branche, commiter, fusionner ou ouvrir une PR.
---

# Git flow

## Rôle de l'agent IA : proposer, jamais exécuter
L'agent ne lance **aucune commande Git qui modifie le dépôt** : `commit`, `push`, `merge`,
`rebase`, `switch -c`, `branch`, `tag`, `reset`, `stash`, ni l'ouverture d'une PR. Il peut
seulement lire (`status`, `diff`, `log`, `show`). Il **propose** le nom de branche, le message
de commit et les commandes ci-dessous, et le développeur les exécute lui-même.

`main` est la seule branche longue : elle est protégée et déployée en production après
approbation. On ne pousse jamais directement dessus.

## Branches : `<domaine>-<type>/<nom-en-kebab-case>`

| Domaine | Périmètre |
|---|---|
| `android` | `mobile-livraison/` |
| `serveur` | API, Thymeleaf, `docker/` |
| `bdd` | migrations Flyway, schéma |
| `ci` | `.github/workflows/` |
| `docs` | `docs/`, skills, configuration IA |

Types : `feature` (nouveauté), `fix` (correction), `refactor`, `test`.

Exemples : `android-feature/carte-tournee`, `android-fix/crash-rotation-colis`,
`serveur-feature/crud-clients`, `serveur-fix/validation-poids-piece`,
`bdd-feature/table-bon-livraison`, `ci-fix/cache-maven`, `docs-feature/skills-ia`.

Une branche correspond à un seul sujet. Elle part de `main` à jour.

## Commits : `<type>(<portée>): <résumé>`

- Types : `feat`, `fix`, `refactor`, `test`, `docs`, `ci`, `chore`.
- Portée : le domaine (`android`, `serveur`, `bdd`, `ci`, `docs`).
- Résumé en français, à l'impératif, sans point final, 72 caractères maximum.
- Si besoin, un corps séparé par une ligne vide explique le pourquoi.

Exemples : `feat(android): afficher les colis sur la carte`,
`fix(bdd): retirer le USE de la migration V1`.

## Intégrer son travail dans main

```bash
git switch main && git pull
git switch <ma-branche> && git merge main   # résoudre les conflits ici, puis tester
git push -u origin <ma-branche>
```

Ensuite, ouvrir une pull request **vers `main`**. La fusion n'est possible que si la CI est
verte (dont le check « Migrations Flyway ») et la branche à jour. Si `main` a bougé,
refaire le `merge main`.

## Interdits
- `push --force` sur une branche partagée.
- Commiter un secret, un `.env`, `target/` ou `build/`.
