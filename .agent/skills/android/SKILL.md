---
name: android
description: Conventions de l'application Android du chauffeur (mobile-livraison/) - stack imposée, architecture, arborescence, tests. À utiliser pour tout code, layout ou test Android.
---

# Android - mobile-livraison/

## Stack imposée (sujet)
- **Java uniquement**, JDK 17 minimum : `compileOptions` en `VERSION_17`. Pas de Kotlin.
- Réseau : **Volley ≥ 1.2.1** (pas Retrofit), JSON avec `org.json`.
- Carte : **OpenStreetMap via osmdroid** (pas Google Maps).
- Stockage local : **fichiers + SharedPreferences** uniquement (pas Room ni SQLite).
- Aucun calcul d'optimisation sur le mobile : on appelle l'API et on affiche le résultat.
- Dépendances déclarées dans `gradle/libs.versions.toml`, jamais en dur dans `build.gradle.kts`.

## Arborescence (package `com.tgv.tgv_delivery`)
```
ui/<ecran>/   Activity, Fragment, Adapter d'un écran (ex. ui/tournee/)
model/        POJO métier : Client, Colis, Piece, Tournee, Etape, BonLivraison
api/          RequeteQueue (singleton Volley) + un service par ressource (TourneeApi…)
data/         lecture/écriture fichiers et préférences
util/         utilitaires sans état
```
Le code existant à plat dans le package racine est à migrer progressivement dans cette structure.

## Règles
- **Une seule `RequestQueue`** pour toute l'app (singleton sur `getApplicationContext()`).
- Les services `api/` exposent des callbacks (`onSucces(T)` / `onErreur(String)`) et
  convertissent le JSON en `model/`. Les écrans ne manipulent jamais de JSON.
- Rien de bloquant sur le thread UI : un travail long va dans un `Thread`, et le résultat
  revient via `runOnUiThread`.
- Onglets : `ViewPager2` + `TabLayout` + `FragmentStateAdapter`. Listes : `RecyclerView` +
  Adapter (+ `CardView`).
- Rotation : sauvegarder l'état dans `onSaveInstanceState`.
- URL de l'API dans une seule constante. En émulateur, l'hôte est `10.0.2.2`.
- Textes dans `res/values/strings.xml` (aucun texte en dur), ids `snake_case`.
- Layouts nommés `activity_x.xml`, `fragment_x.xml`, `item_x.xml`.
- Permissions dans le manifeste : `INTERNET`, localisation si la carte en a besoin.

## Tests
- `src/test/` (JUnit 4) : modèle, parsing JSON, logique de `data/`.
- `src/androidTest/` (Espresso) : parcours d'écrans.
- Commandes depuis `mobile-livraison/` : `./gradlew test`, puis `./gradlew assembleDebug`.
