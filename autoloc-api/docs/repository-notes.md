# Couche Repository — AutoLoc (Atelier 3)

## 1. Choix de l'interface

Les 9 repositories étendent `JpaRepository<Entité, Long>` (package `tn.esprit.autoloc.repository`, nommage `I<Entité>Repository`).

| Repository | Entité | Interface | Justification |
|---|---|---|---|
| IAgenceRepository | Agence | JpaRepository<Agence, Long> | CRUD complet + listes (`List`) + pagination/tri pour l'affichage des agences |
| IEmployeRepository | Employe | JpaRepository<Employe, Long> | Même besoin ; recherches par agence ou rôle à venir |
| IVehiculeRepository | Vehicule | JpaRepository<Vehicule, Long> | Catalogue volumineux : pagination et tri indispensables, filtres par statut/catégorie à venir |
| IEquipementRepository | Equipement | JpaRepository<Equipement, Long> | CRUD simple sur une petite table de référence |
| IClientRepository | Client | JpaRepository<Client, Long> | CRUD + recherche par email / permis à venir |
| IReservationRepository | Reservation | JpaRepository<Reservation, Long> | CRUD + requêtes par période et statut à venir |
| IContratRepository | Contrat | JpaRepository<Contrat, Long> | CRUD + `saveAndFlush` utile pour obtenir l'id avant d'ajouter des paiements |
| IPaiementRepository | Paiement | JpaRepository<Paiement, Long> | CRUD + tri par date de paiement |
| IMaintenanceRepository | Maintenance | JpaRepository<Maintenance, Long> | CRUD + historique par véhicule à venir |

## 2. Pourquoi JpaRepository plutôt que CrudRepository

- Les méthodes de lecture retournent des `List` (et non des `Iterable`).
- Pagination et tri inclus (`findAll(Pageable)`, `findAll(Sort)`), car `PagingAndSortingRepository` n'étend plus `CrudRepository` dans Spring Data 3.
- Opérations JPA utiles : `saveAndFlush`, `flush`, `deleteAllInBatch`, `getReferenceById`.
- Le surcoût est nul : `@EnableJpaRepositories` et `@Repository` ne sont pas nécessaires avec Spring Boot.

## 3. Points d'attention

- `save()` fait un `persist` si l'id est `null`, sinon un `merge`.
- `deleteById()` ne lève pas d'exception si l'id n'existe pas.
- Les suppressions en lot (`deleteAllInBatch`, `deleteAllByIdInBatch`) contournent la cascade et `orphanRemoval` : à éviter sur `Contrat` (ses `Paiement` ne seraient pas supprimés).

## 4. Analyse qualité

**SonarQube for IDE** (dossier `src`, 24 fichiers Java) : 0 issue, 0 Security Hotspot.

**Inspections IntelliJ** (onglet Problems) : anomalies détectées et traitées.

| # | Anomalie | Explication | Correction |
|---|---|---|---|
| 1 | `Non-null type argument is expected` sur les 9 repositories | Spring Data 4 déclare ses génériques non-null (JSpecify) ; le package n'avait aucune déclaration de nullabilité. | Ajout de `package-info.java` avec `@NullMarked` dans le package `repository` |
| 2 | `Cannot resolve table 'employe'` sur les entités | IntelliJ n'avait aucune source de données pour vérifier les noms de tables. | Connexion de l'IDE à la base MySQL `autoloc_db` |
| 3 | `Typo: In word 'Agence'`, `'Contrat'`, `'Employe'`… | Le correcteur orthographique ne connaît pas le vocabulaire métier français. | Mots ajoutés au dictionnaire du projet |
| — | `Interface 'IXxxRepository' is never used` | Normal à ce stade : aucun service n'injecte encore les repositories. Résolu à l'Atelier 4. | Aucune (comportement attendu) |

Résultat après correction : plus d'avertissement de nullabilité ni de table non résolue ; `Found 9 JPA repository interfaces` toujours affiché au démarrage.