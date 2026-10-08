# Stratégie de fetch et de cascade — AutoLoc (Atelier 2)

## 1. Principes retenus

- **Fetch** : toutes les associations sont en `FetchType.LAZY`. Les `@ManyToOne` et `@OneToOne` sont EAGER par défaut en JPA ; on les force donc explicitement en LAZY pour éviter de charger toute une chaîne d'objets liés (ex. charger une réservation ne doit pas charger son client, son véhicule, l'agence du véhicule, etc.). Les `@OneToMany` et `@ManyToMany` sont déjà LAZY par défaut, on le déclare quand même pour que le choix soit visible dans le code.
- **Cascade** : une cascade n'est justifiée que si l'enfant ne peut pas vivre sans son parent (composition). Dans les autres cas (agrégation), aucune propagation de suppression n'est appliquée.
- **Côté propriétaire** : c'est toujours le côté qui porte la clé étrangère (`@ManyToOne`, ou `@OneToOne` côté `Contrat`, ou `@JoinTable` côté `Vehicule`). Le côté inverse utilise `mappedBy` et ne crée aucune colonne.
- **Lombok** : `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor` uniquement. Pas de `@Data`, car son `toString`/`hashCode` parcourrait les deux côtés des associations bidirectionnelles et provoquerait un `StackOverflowError`.
- **Collections** : `List` pour les `@OneToMany`, `Set` pour le `@ManyToMany` (pas de doublons dans une relation Vehicule/Equipement).
- **Noms des clés étrangères** : générés par Hibernate (`<attribut>_<clé primaire référencée>`), sans `@JoinColumn`. La table de jointure `vehicule_equipement` est nommée explicitement.

## 2. Tableau des associations

| Association | Annotations | Côté propriétaire (clé étrangère) | Fetch | Cascade | Justification |
|---|---|---|---|---|---|
| Contrat → Paiement | `@OneToMany` / `@ManyToOne` | `Paiement` (`contrat_id_contrat`) | LAZY | `ALL` + `orphanRemoval = true` | Un paiement n'existe que rattaché à son contrat (composition). Supprimer le contrat supprime ses paiements ; retirer un paiement de la liste le supprime en base. LAZY : la liste des paiements n'est chargée que si on l'utilise. |
| Agence → Vehicule | `@OneToMany` / `@ManyToOne` | `Vehicule` (`agence_id_agence`) | LAZY | aucune | Un véhicule survit à la suppression de son agence (il peut être réaffecté) : supprimer une agence ne doit jamais supprimer sa flotte. Une agence peut avoir une flotte importante, donc la liste n'est pas chargée systématiquement. |
| Agence → Employe | `@OneToMany` / `@ManyToOne` | `Employe` (`agence_id_agence`) | LAZY | aucune | Un employé existe indépendamment de l'agence où il est affecté (mutation possible). Pas de suppression en cascade, pas de chargement systématique du personnel. |
| Vehicule ↔ Equipement | `@ManyToMany` | `Vehicule` (`@JoinTable` `vehicule_equipement` : `id_vehicule`, `id_equipement`) | LAZY | aucune | Les équipements (GPS, siège bébé…) sont partagés entre plusieurs véhicules : supprimer un véhicule ne doit pas supprimer un équipement utilisé ailleurs, et inversement. `Set` pour éviter les doublons. LAZY par défaut conservé. |
| Client → Reservation | `@OneToMany` / `@ManyToOne` | `Reservation` (`client_id_client`) | LAZY | `PERSIST` | Enregistrer un nouveau client avec ses premières réservations les enregistre en même temps. Pas de `REMOVE` : l'historique des réservations ne doit pas disparaître avec le client. L'historique d'un client peut être long : LAZY. |
| Reservation → Vehicule | `@ManyToOne` | `Reservation` (`vehicule_id_vehicule`) | LAZY | aucune | Le véhicule a son propre cycle de vie : annuler ou supprimer une réservation ne supprime pas le véhicule, et inversement. Association unidirectionnelle : le véhicule n'a pas besoin de connaître la liste de ses réservations pour l'instant. |
| Reservation ↔ Contrat | `@OneToOne` | `Contrat` (`reservation_id_reservation`) | LAZY | `ALL` côté `Reservation` | Le contrat est généré à partir de la réservation et n'a pas de sens sans elle : sauvegarder, mettre à jour ou supprimer la réservation se propage au contrat (et, par lui, à ses paiements). `@OneToOne` est EAGER par défaut, on force LAZY. |
| Vehicule → Maintenance | `@OneToMany` / `@ManyToOne` | `Maintenance` (`vehicule_id_vehicule`) | LAZY | `PERSIST` | Une intervention est créée avec le véhicule qu'elle concerne, d'où `PERSIST`. Pas de `REMOVE` : l'historique de maintenance est conservé pour la traçabilité. L'historique peut être long : LAZY. |

## 3. Remarques

- **`LazyInitializationException`** : avec LAZY, accéder à une collection en dehors d'une transaction lève cette exception. Elle sera traitée avec des requêtes dédiées (Atelier 7).
- **`@OneToOne` côté inverse** : sur `Reservation.contrat` (côté `mappedBy`), Hibernate doit interroger la base pour savoir si un contrat existe. Le LAZY n'y est donc pas pleinement effectif sans amélioration de bytecode. Le côté propriétaire (`Contrat.reservation`) est lui bien paresseux.
- **EAGER écarté** : testé sur `Contrat.paiements`, il alourdit chaque lecture d'un contrat, même quand les paiements ne servent pas. Configuration finale : LAZY.
- **Schéma attendu** : 9 tables des entités + la table de jointure `vehicule_equipement` (10 au total). Les autres associations ne créent qu'une colonne de clé étrangère côté propriétaire.