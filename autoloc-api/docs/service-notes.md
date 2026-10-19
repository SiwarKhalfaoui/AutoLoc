# Atelier 4 — Notes sur la couche Service (IoC / DI)

## A. Modes d'injection comparés (étape 2)

| Critère | Constructeur (`@RequiredArgsConstructor` + `final`) | Attribut (`@Autowired` sur le champ) |
|---|---|---|
| Champ `final` possible ? | Oui | Non |
| Dépendance visible dans la signature de la classe ? | Oui (paramètre du constructeur) | Non (cachée dans le corps) |
| Utilisable avec `new` hors conteneur sans dépendance `null` ? | Oui : `new ContratServiceImpl(repo)` oblige à fournir le repository | Non : `new ContratServiceImpl()` compile mais le repository vaut `null` (NullPointerException) |
| Signalé par SonarQube for IDE ? | Non | Oui — règle S6813 |

## B. Services

| Service | Dépendances injectées | Mode | Justification |
|---|---|---|---|
| AgenceServiceImpl | IAgenceRepository | Constructeur | Dépendance obligatoire, champ `final`, typée par interface ; service sans état. |
| EmployeServiceImpl | IEmployeRepository | Constructeur | Idem. |
| VehiculeServiceImpl | IVehiculeRepository | Constructeur | Idem. |
| EquipementServiceImpl | IEquipementRepository | Constructeur | Idem. |
| ClientServiceImpl | IClientRepository | Constructeur | Idem. |
| ReservationServiceImpl | IReservationRepository | Constructeur | Idem ; aucune dépendance vers un autre service (pas de cycle). |
| ContratServiceImpl | IContratRepository | Constructeur | Idem ; suppression d'un contrat = suppression de ses paiements par cascade. |
| PaiementServiceImpl | IPaiementRepository | Constructeur | Idem ; lecture seule. |
| MaintenanceServiceImpl | IMaintenanceRepository | Constructeur | Idem. |

## C. Messages d'erreur du conteneur (étape 3)

**Message 1 — `IContratService` introuvable pour `ContratController`**
- Exception : `NoSuchBeanDefinitionException` (enveloppée dans `UnsatisfiedDependencyException`).
- Causes probables : (1) `@Service` oublié sur `ContratServiceImpl` ; (2) classe placée hors du package `tn.esprit.autoloc`, donc non détectée par le scan.
- Correction : ajouter `@Service`, vérifier le package.

**Message 2 — deux beans pour `NotificationController`**
- Exception : `NoUniqueBeanDefinitionException` (`emailNotificateur`, `smsNotificateur`).
- Correction 1 : `@Primary` sur la **classe** du bean par défaut.
- Correction 2 : `@Qualifier("smsNotificateur")` sur le **paramètre du constructeur** du contrôleur.

**Message 3 — cycle `clientServiceImpl` ↔ `reservationServiceImpl`**
- Exception : `BeanCurrentlyInCreationException` (dépendance circulaire).
- Avec le constructeur, A exige B complet et B exige A complet : aucun ne peut être créé en premier, donc le cycle est insoluble.
- Piste : extraire la responsabilité commune dans un troisième service, ou dépendre du repository de l'autre plutôt que de son service.

## D. Anomalies SonarQube for IDE

| # | Règle | Explication | Correction |
|---|---|---|---|
| 1 | S6813 — injection par attribut | `@Autowired` sur un champ cache la dépendance et empêche `final`. | Retour à `@RequiredArgsConstructor` + `private final`. |
| 2 | S112 — exception générique | `RuntimeException` dans `findById` : on ne distingue plus « absent » d'une autre erreur. | Restauration de `ResourceNotFoundException`. |
| 3 | S1128 — import inutile | Import non utilisé = code mort. | Suppression (Ctrl+Alt+O). |

(Vérifier les numéros exacts dans votre panneau SonarQube for IDE.)

## E. Questions de compréhension

1. **Où est le `new` de `ContratServiceImpl` ?** Nulle part dans notre code : le conteneur Spring (`ApplicationContext`) détecte `@Service` au scan, appelle le constructeur et lui fournit le proxy de `IContratRepository`.
2. **Pourquoi dépendre de `IContratService` ?** Pour dépendre d'une abstraction : on change l'implémentation (ou on fournit un faux service pour les tests) sans toucher au contrôleur.
3. **Pourquoi un singleton sans état ?** Une seule instance est partagée par tous les threads. Un attribut modifiable (ex. `contratCourant`) serait écrasé par une requête concurrente : l'utilisateur A verrait ou modifierait le contrat de B.
4. **Pourquoi `update` charge avant `save` ?** Passer l'objet reçu écraserait toutes les colonnes (valeurs absentes = `null`) et, avec `orphanRemoval`, pourrait vider la collection de paiements.
5. **Différences.** `@Component` se pose sur une classe qu'on possède ; `@Bean` sur une méthode de `@Configuration`, pour une classe non annotable. `@Primary` : candidat par défaut posé sur la classe ; `@Qualifier` : choix explicite posé à l'injection. `IPaiementService` est en lecture seule car un paiement n'existe qu'au sein de son contrat (cascade `ALL` + `orphanRemoval`).