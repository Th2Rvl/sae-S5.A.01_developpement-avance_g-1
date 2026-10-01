# SAE S5.A.01 – Gestion de tournée de livraison (T.G.V)

Application de gestion et d'optimisation des tournées de livraison de l'entreprise T.G.V (Tournée Générale Véloce).

## Arborescence

```
.
├── serveur/            # Application Spring Boot : API REST (/api) + interface web Atelier (/atelier)
├── mobile-livraison/   # Application Android (Java) : interface Livraison du chauffeur
├── docker/             # docker-compose, scripts d'initialisation de la base de données
├── docs/               # Documentation : UML, suivi de l'IA...
└── README.md
```

| Dossier | Rôle |
|---|---|
| `serveur/` | API consommée par le mobile, pages web de l'interface Atelier et algorithmes d'optimisation du chargement et de la boucle de livraison. |
| `mobile-livraison/` | Application du chauffeur : consultation des colis, carte OpenStreetMap, boucle de livraison, bons de livraison. |
| `docker/` | Conteneurisation des serveurs (application + base de données). |
| `docs/` | Livrables et documents de suivi du projet. |
