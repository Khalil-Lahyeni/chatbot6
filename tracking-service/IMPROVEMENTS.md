# IMPROVEMENTS.md — Analyse Qualité & Recommandations de Refactoring

> **Projet analysé** : `tracking-service` — microservice Spring Boot 3.5 / Java 21  
> **Date d'analyse** : 2026-06-25  
> **Périmètre** : ensemble du code source (`src/main` et `src/test`)

---

## Table des matières

1. [Principes SOLID](#1-principes-solid)
2. [Absence d'interfaces pour les services](#2-absence-dinterfaces-pour-les-services)
3. [Code dupliqué et responsabilités mal placées](#3-code-dupliqué-et-responsabilités-mal-placées)
4. [Design Patterns manquants ou applicables](#4-design-patterns-manquants-ou-applicables)
5. [Architecture en couches et organisation des packages](#5-architecture-en-couches-et-organisation-des-packages)
6. [Gestion centralisée des exceptions](#6-gestion-centralisée-des-exceptions)
7. [Bonnes pratiques JPA/Hibernate](#7-bonnes-pratiques-jpahibermate)
8. [DTOs, Mappers et transformation de données](#8-dtos-mappers-et-transformation-de-données)
9. [Validation des données](#9-validation-des-données)
10. [Sécurité et configuration](#10-sécurité-et-configuration)
11. [Gestion des transactions](#11-gestion-des-transactions)
12. [Optimisation des performances](#12-optimisation-des-performances)
13. [Testabilité du code](#13-testabilité-du-code)
14. [Conventions de nommage](#14-conventions-de-nommage)
15. [Résumé et plan de refactoring priorisé](#15-résumé-et-plan-de-refactoring-priorisé)

---

## Légende de sévérité

| Icône | Sévérité | Signification |
|-------|----------|---------------|
| 🔴 | **Critique** | Bug latent, violation de principe fondamental, dette technique bloquante |
| 🟠 | **Majeur** | Impact significatif sur la maintenabilité ou les performances |
| 🟡 | **Mineur** | Bonne pratique non suivie, amélioration de lisibilité |

---

## 1. Principes SOLID

### 1.1 🔴 Single Responsibility Principle (SRP) — Services trop larges

**Fichiers concernés** : `TrainLocationService.java`, `TrainSystemStatusService.java`

**Problème** : Chaque service fait **quatre choses simultanément** dans une seule méthode `process()` :
1. Résolution du train (recherche BDD + création placeholder si absent)
2. Déduplication (appel DedupService)
3. Mapping DTO → Entité
4. Persistance (appel repository)
5. Publication d'événement (appel publisher)

```java
// Extrait actuel — TrainLocationService.java
public void process(TrainLocationDto dto) {
    // 1. Dédup
    if (!dedupService.isNew(...)) return;
    // 2. Résolution train (+ création si absent)
    Train train = trainRepository.findById(...).orElseGet(() -> { ... trainRepository.save(t) });
    // 3. Mapping
    TrainLocation entity = TrainLocation.builder()...build();
    // 4. Persistance
    trainLocationRepository.save(entity);
    // 5. Publication
    eventPublisher.publish(...);
}
```

**Recommandation** : Extraire chaque responsabilité dans une classe dédiée.

```
TrainLocationService       → orchestration uniquement
TrainResolver              → findOrCreate(trainId) : Train
TrainLocationMapper        → toEntity(dto, train) : TrainLocation
TrainLocationPersister     → save(entity)
TrainEventPublisher        → publish(trainId, payload)
DedupService               → isNew(...)
```

---

### 1.2 🔴 Open/Closed Principle (OCP) — Extension impossible sans modification

**Fichier concerné** : `CacheWarmupService.java`

**Problème** : L'ajout d'un nouveau type de message (ex. `train-configuration`) nécessite de **modifier** `CacheWarmupService.run()`. La classe n'est pas fermée à la modification.

```java
// Actuel : chaque nouveau type de message = modification de cette méthode
public void run(ApplicationArguments args) {
    for (Train train : trains) {
        warmupLocation(train);       // hardcodé
        warmupSystemStatus(train);   // hardcodé
        // → il faudra ajouter warmupConfiguration(train) ici
    }
}
```

**Recommandation** : Utiliser une liste injectable de `CacheWarmupContributor` (voir §4 — Strategy Pattern).

---

### 1.3 🔴 Dependency Inversion Principle (DIP) — Dépendances vers les implémentations concrètes

**Fichiers concernés** : tous les consumers, services

**Problème** : Les classes de haut niveau dépendent directement des implémentations concrètes, pas d'abstractions.

```java
// Actuel — TrainLocationConsumer.java
private final TrainLocationService trainLocationService;   // classe concrète
private final TrainMessageValidator validator;             // classe concrète
```

**Recommandation** : Définir des interfaces (voir §2) et injecter les interfaces.

```java
// Cible
private final TrainLocationProcessor trainLocationProcessor;  // interface
private final MessageValidator validator;                      // interface
```

---

### 1.4 🟠 Interface Segregation Principle (ISP) — Absent

Aucune interface n'est définie pour les services, publisher ou validator. Chaque consommateur de ces services est forcé de dépendre de toute l'implémentation, ce qui nuit à la testabilité et à l'évolutivité. (Détaillé en §2.)

---

## 2. Absence d'interfaces pour les services

**Statut actuel** : ❌ **Aucune interface de service n'existe dans le projet.**

Toutes les classes de service, publisher et validator sont des classes concrètes directement injectées. Cela viole le principe de programmation par contrat et rend difficile le remplacement d'implémentation (ex. : changer de provider de cache, mocker proprement dans les tests).

### 2.1 🔴 Interfaces manquantes — Liste exhaustive

| Classe concrète actuelle | Interface recommandée | Package cible |
|---|---|---|
| `DedupService` | `DeduplicationService` | `service` |
| `TrainLocationService` | `TrainLocationProcessor` | `service` |
| `TrainSystemStatusService` | `TrainSystemStatusProcessor` | `service` |
| `TrainEventPublisher` | `EventPublisher` | `port.out` |
| `TrainMessageValidator` | `MessageValidator` | `validation` |
| `CacheWarmupService` | `CacheWarmupStrategy` | `warmup` |

### 2.2 Structure Service/ServiceImpl recommandée

```
service/
├── DeduplicationService.java          ← interface
├── impl/
│   └── RedisDedupService.java         ← implémentation Redis (renommée)
├── TrainLocationProcessor.java        ← interface
├── impl/
│   └── TrainLocationProcessorImpl.java
├── TrainSystemStatusProcessor.java    ← interface
└── impl/
    └── TrainSystemStatusProcessorImpl.java
```

```java
// Interface
public interface DeduplicationService {
    boolean isNew(String messageType, String entityId, String hashableContent);
    void seedCache(String messageType, String entityId, String hashableContent);
}

// Implémentation
@Service
@RequiredArgsConstructor
public class RedisDedupService implements DeduplicationService {
    // ...
}
```

**Bénéfices** :
- Remplacement de Redis par un autre backend de cache sans toucher aux consommateurs
- Mocking simplifié dans les tests unitaires (interface au lieu de classe concrète)
- Conformité au DIP

---

## 3. Code dupliqué et responsabilités mal placées

### 3.1 🔴 Duplication de la logique "find-or-create Train"

**Fichiers concernés** : `TrainLocationService.java` (lignes 38-46), `TrainSystemStatusService.java` (lignes 41-48)

**Problème** : Le bloc suivant est **copié à l'identique** dans les deux services :

```java
// Dupliqué dans TrainLocationService ET TrainSystemStatusService
Train train = trainRepository.findById(dto.getTrainId())
    .orElseGet(() -> {
        log.warn("Train {} not found in DB — creating placeholder", dto.getTrainId());
        Train t = new Train();
        t.setTrainId(dto.getTrainId());
        t.setName("UNKNOWN-" + dto.getTrainId());
        return trainRepository.save(t);
    });
```

**Recommandation** : Extraire dans un `TrainResolver` (ou `TrainRegistryService`) dédié :

```java
@Service
@RequiredArgsConstructor
public class TrainResolver {
    private final TrainRepository trainRepository;

    @Transactional
    public Train resolveOrCreate(Long trainId) {
        return trainRepository.findById(trainId)
            .orElseGet(() -> {
                log.warn("Auto-registering unknown train {}", trainId);
                return trainRepository.save(Train.builder()
                    .trainId(trainId)
                    .name("UNKNOWN-" + trainId)
                    .build());
            });
    }
}
```

---

### 3.2 🔴 Duplication de la logique de consumer

**Fichiers concernés** : `TrainLocationConsumer.java`, `TrainSystemStatusConsumer.java`

**Problème** : Les deux consumers sont identiques à 95% — seuls le type de DTO et le service appelé changent :

```java
// Logique identique dans les deux consumers :
// 1. Désérialisation JSON → DTO
// 2. Gestion JsonProcessingException
// 3. Appel validator.validate(dto)
// 4. Appel service.process(dto)
```

**Recommandation** : Créer une classe abstraite générique (voir §4 — Template Method Pattern).

---

### 3.3 🔴 Duplication des constantes de type de message

**Problème** : La chaîne `"train-location"` apparaît à **4 endroits différents** :
- `TrainLocationService.MESSAGE_TYPE`
- `TrainLocationConsumer` (`@KafkaListener topics`)
- `CacheWarmupService.LOCATION_TYPE`
- `KafkaConfig` (bean `trainLocationTopic`)

Et `"train-system-status"` apparaît à 3 endroits.

**Recommandation** : Centraliser dans une classe de constantes ou dans un enum :

```java
public enum MessageType {
    TRAIN_LOCATION("train-location"),
    TRAIN_SYSTEM_STATUS("train-system-status"),
    TRAIN_EVENTS("train-events");

    private final String topicName;

    MessageType(String topicName) { this.topicName = topicName; }
    public String topic() { return topicName; }
    public String dedupKey() { return topicName; }
}
```

---

### 3.4 🟠 Logique de construction du hash dupliquée

**Problème** : La logique de concaténation des champs pour le hash est définie dans **trois endroits distincts** :

| Endroit | Code |
|---|---|
| `TrainLocationService` | `dto.getCurrentStation() + dto.getNextStation() + dto.getDestination()` |
| `CacheWarmupService.warmupLocation()` | `loc.getCurrentStation() + loc.getNextStation() + loc.getDestination()` |
| `TrainSystemStatusService` | `dto.getPacisStatus().name() + dto.getCctvStatus().name() + ...` |
| `CacheWarmupService.warmupSystemStatus()` | `status.getPacisStatus().name() + ...` |

Si la logique de hash change (ex. : ajout d'un séparateur), elle doit être modifiée en plusieurs endroits.

**Recommandation** : Strategy Pattern pour l'extraction du contenu hashable (voir §4.2).

---

### 3.5 🟠 Responsabilité mal placée — `TrainEventPublisher` connaît le format JSON

**Fichier concerné** : `TrainEventPublisher.java`

**Problème** : `TrainEventPublisher` embarque la sérialisation JSON. Le publisher devrait être agnostique au format — c'est la responsabilité du serializer, pas du publisher.

```java
// Actuel : le publisher sérialise lui-même
String json = objectMapper.writeValueAsString(payload);
kafkaTemplate.send(TOPIC, trainId, json);
```

**Recommandation** : Utiliser un `KafkaTemplate<String, Object>` avec un `JsonSerializer` configuré, ou passer la sérialisation à un `EventSerializer` dédié.

---

## 4. Design Patterns manquants ou applicables

### 4.1 🔴 Template Method Pattern — Pipeline de traitement des consumers

**Problème** : Les deux consumers (`TrainLocationConsumer`, `TrainSystemStatusConsumer`) suivent le même algorithme en 4 étapes. Sans Template Method, chaque nouveau type de message implique une copie intégrale du consumer.

**Recommandation** : Classe abstraite générique :

```java
// Classe abstraite — définit le pipeline
public abstract class AbstractMessageConsumer<T> {

    private final ObjectMapper objectMapper;
    private final MessageValidator validator;
    private final Class<T> dtoClass;

    protected AbstractMessageConsumer(ObjectMapper objectMapper,
                                      MessageValidator validator,
                                      Class<T> dtoClass) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.dtoClass = dtoClass;
    }

    // Pipeline fixe — ne pas overrider
    public final void consume(String message) {
        T dto = deserialize(message);
        if (dto == null) return;
        if (!validator.validate(dto)) return;
        process(dto);
    }

    // Seule étape variable selon le type de message
    protected abstract void process(T dto);

    private T deserialize(String message) {
        try {
            return objectMapper.readValue(message, dtoClass);
        } catch (JsonProcessingException ex) {
            log.error("Deserialization failed for {}: {}", dtoClass.getSimpleName(), ex.getMessage());
            return null;
        }
    }
}

// Consumer simplifié — 10 lignes au lieu de 52
@Component
public class TrainLocationConsumer extends AbstractMessageConsumer<TrainLocationDto> {

    private final TrainLocationProcessor processor;

    public TrainLocationConsumer(ObjectMapper objectMapper,
                                 MessageValidator validator,
                                 TrainLocationProcessor processor) {
        super(objectMapper, validator, TrainLocationDto.class);
        this.processor = processor;
    }

    @KafkaListener(topics = "${kafka.topics.train-location}", groupId = "${kafka.consumer.group-id}")
    public void consume(String message) { super.consume(message); }

    @Override
    protected void process(TrainLocationDto dto) { processor.process(dto); }
}
```

---

### 4.2 🔴 Strategy Pattern — Extraction du contenu hashable

**Problème** : La logique d'extraction du contenu à hasher est éparpillée dans les services et le warmup (voir §3.4).

**Recommandation** :

```java
// Interface Strategy
@FunctionalInterface
public interface HashContentExtractor<T> {
    String extract(T message);
}

// Déclaration dans chaque type de message (ou via @Bean)
public class TrainLocationHashExtractor implements HashContentExtractor<TrainLocationDto> {
    private static final String SEPARATOR = "|";

    @Override
    public String extract(TrainLocationDto dto) {
        return dto.getCurrentStation() + SEPARATOR
             + dto.getNextStation()    + SEPARATOR
             + dto.getDestination();
    }
}

// Utilisation dans le service
public class TrainLocationProcessorImpl implements TrainLocationProcessor {
    private final HashContentExtractor<TrainLocationDto> hashExtractor;
    // ...
    public void process(TrainLocationDto dto) {
        String content = hashExtractor.extract(dto);
        if (!dedupService.isNew(MessageType.TRAIN_LOCATION.dedupKey(), ...)) return;
        // ...
    }
}
```

**Bénéfice supplémentaire** : Le `CacheWarmupService` peut utiliser la même `HashContentExtractor` avec l'entité en adaptant via un Adapter.

---

### 4.3 🔴 Strategy Pattern — CacheWarmupContributor (OCP fix)

**Recommandation** : Remplacer les méthodes hardcodées par une liste de contributeurs injectés.

```java
// Interface Strategy
public interface CacheWarmupContributor {
    void warmup(Train train);
}

// Implémentations enregistrées comme @Component
@Component
public class LocationCacheWarmupContributor implements CacheWarmupContributor {
    @Override
    public void warmup(Train train) { /* logique location */ }
}

@Component
public class SystemStatusCacheWarmupContributor implements CacheWarmupContributor {
    @Override
    public void warmup(Train train) { /* logique status */ }
}

// Service orchestrateur — fermé à la modification
@Component
@RequiredArgsConstructor
public class CacheWarmupService implements ApplicationRunner {
    private final TrainRepository trainRepository;
    private final List<CacheWarmupContributor> contributors; // injection automatique de tous les @Component

    @Override
    public void run(ApplicationArguments args) {
        List<Train> trains = trainRepository.findAll();
        trains.forEach(train ->
            contributors.forEach(contributor -> contributor.warmup(train))
        );
    }
}
```

---

### 4.4 🟠 Builder Pattern — Incohérence entre les deux entités

**Problème** : `TrainLocation` utilise correctement le builder Lombok, mais `TrainSystemStatus` utilise des setters dans `TrainSystemStatusService` :

```java
// TrainSystemStatusService.java — anti-pattern : setters manuels
TrainSystemStatus entity = new TrainSystemStatus();
entity.setPacisStatus(dto.getPacisStatus());
entity.setCctvStatus(dto.getCctvStatus());
entity.setRearViewStatus(dto.getRearViewStatus());
entity.setUpdateStatus(UpdateStatus.UPDATED);
entity.setTrain(train);
```

Alors que `@Builder` est disponible sur `TrainSystemStatus`.

**Recommandation** : Utiliser systématiquement le builder :

```java
TrainSystemStatus entity = TrainSystemStatus.builder()
    .pacisStatus(dto.getPacisStatus())
    .cctvStatus(dto.getCctvStatus())
    .rearViewStatus(dto.getRearViewStatus())
    .updateStatus(UpdateStatus.UPDATED)
    .train(train)
    .build();
```

---

### 4.5 🟠 Factory Pattern — Création de placeholders Train

**Problème** : La logique de création d'un `Train` placeholder est définie inline dans deux services, avec le nom `"UNKNOWN-{id}"` hardcodé.

**Recommandation** :

```java
public class TrainFactory {
    public static Train createPlaceholder(Long trainId) {
        return Train.builder()
            .trainId(trainId)
            .name("UNKNOWN-" + trainId)
            .build();
    }
}
```

---

### 4.6 🟡 Adapter Pattern — Mapper entre entité et contenu hashable

Le `CacheWarmupService` doit connaître les champs internes de `TrainLocation` et `TrainSystemStatus` pour reconstruire le contenu hashable. Un Adapter (`TrainLocationToHashAdapter`) isolerait cette dépendance.

---

## 5. Architecture en couches et organisation des packages

### 5.1 🔴 Structure de packages plate — absence de couches claires

**Structure actuelle** :
```
com.actia.tracking_service/
├── config/
├── consumer/
├── dto/
├── entity/
├── enums/
├── publisher/
├── repository/
├── service/
├── validation/
└── warmup/
```

**Problème** : Tous les packages sont au même niveau sans refléter les couches architecturales. Il n'y a pas de distinction entre le domaine métier, l'infrastructure et les ports d'entrée/sortie.

**Recommandation** — Structure Hexagonale (Ports & Adapters) :

```
com.actia.tracking_service/
│
├── domain/                          ← CŒUR DOMAINE (aucune dépendance externe)
│   ├── model/
│   │   ├── Train.java               (valeur objet domaine, pas forcément entité JPA)
│   │   └── TrainLocation.java
│   ├── port/
│   │   ├── in/                      ← ce que le domaine expose
│   │   │   ├── TrainLocationProcessor.java
│   │   │   └── TrainSystemStatusProcessor.java
│   │   └── out/                     ← ce dont le domaine a besoin
│   │       ├── TrainRepository.java
│   │       ├── DeduplicationService.java
│   │       └── EventPublisher.java
│   └── service/
│       ├── TrainLocationProcessorImpl.java
│       └── TrainSystemStatusProcessorImpl.java
│
├── infrastructure/                  ← ADAPTATEURS SECONDAIRES
│   ├── persistence/
│   │   ├── entity/                  ← entités JPA
│   │   ├── repository/              ← interfaces Spring Data
│   │   └── mapper/                  ← mappers entité ↔ domaine
│   ├── cache/
│   │   ├── RedisDedupService.java
│   │   └── CacheWarmupService.java
│   └── messaging/
│       └── producer/
│           └── KafkaEventPublisher.java
│
├── application/                     ← ADAPTATEURS PRIMAIRES
│   ├── consumer/                    ← @KafkaListener
│   │   ├── TrainLocationConsumer.java
│   │   └── TrainSystemStatusConsumer.java
│   └── dto/                         ← modèles de messages entrants
│       ├── TrainLocationDto.java
│       └── TrainSystemStatusDto.java
│
└── config/                          ← CONFIGURATION SPRING
    ├── KafkaConfig.java
    ├── KafkaErrorConfig.java
    └── RedisConfig.java
```

---

### 5.2 🟠 Mélange de packages techniques et domaine

**Problème** : `enums/SystemHealthStatus` et `enums/UpdateStatus` sont des types du domaine métier mais sont dans un package purement technique `enums/`.

**Recommandation** : Placer les enums domaine dans `domain/model/` :

```
domain/model/SystemHealthStatus.java
domain/model/UpdateStatus.java
```

---

### 5.3 🟡 `CacheWarmupService` mal placé

**Problème** : Le package `warmup/` est à la racine, au même niveau que les services métier. Le warmup est une préoccupation d'infrastructure.

**Recommandation** : Déplacer vers `infrastructure/cache/CacheWarmupService.java`.

---

## 6. Gestion centralisée des exceptions

### 6.1 🔴 Aucune hiérarchie d'exceptions métier

**Statut actuel** : ❌ **Aucune exception personnalisée n'existe dans le projet.**

**Problème** : Les erreurs sont gérées par des `log.error()` dispersés avec retour boolean ou silencieux. Aucune exception métier ne permet de distinguer une erreur de validation, une entité manquante, ou une erreur de cache.

**Recommandation** : Définir une hiérarchie d'exceptions :

```java
// Racine
public abstract class TrackingServiceException extends RuntimeException { ... }

// Exceptions métier
public class TrainNotFoundException extends TrackingServiceException {
    public TrainNotFoundException(Long trainId) {
        super("Train not found: " + trainId);
    }
}

public class MessageValidationException extends TrackingServiceException {
    private final List<String> violations;
    // ...
}

public class MessageProcessingException extends TrackingServiceException { ... }

// Exceptions infrastructure
public class CacheUnavailableException extends TrackingServiceException { ... }
public class EventPublishingException extends TrackingServiceException { ... }
```

---

### 6.2 🔴 `TrainLocationService` crée silencieusement des trains inconnus

**Fichiers concernés** : `TrainLocationService.java` (ligne 38), `TrainSystemStatusService.java` (ligne 41)

**Problème** : La création silencieuse d'un train placeholder est un **effet de bord non documenté** qui peut masquer des erreurs de configuration en production (un train envoyant des données avec un ID inconnu devrait alerter, pas être créé automatiquement).

**Recommandation** : Lever une `TrainNotFoundException` et laisser la décision de créer un placeholder à une stratégie configurable :

```java
@ConfigurationProperties("tracking.train")
public record TrainProperties(boolean autoRegisterUnknown) {}

// Dans TrainResolver
public Train resolveOrCreate(Long trainId) {
    return trainRepository.findById(trainId).orElseGet(() -> {
        if (!properties.autoRegisterUnknown()) {
            throw new TrainNotFoundException(trainId);
        }
        return trainRepository.save(TrainFactory.createPlaceholder(trainId));
    });
}
```

---

### 6.3 🟠 `TrainMessageValidator` retourne un boolean — anti-pattern

**Fichier concerné** : `TrainMessageValidator.java`

**Problème** : Retourner `false` au lieu de lever une exception oblige les appelants à brancher manuellement sur `if (!validator.validate(dto)) return;`. Ce pattern se duplique dans chaque consumer.

**Recommandation** :

```java
public interface MessageValidator {
    // Lève MessageValidationException si invalide
    <T> void validateOrThrow(T dto);
}
```

Le consumer gère alors l'exception de façon unifiée dans le pipeline abstrait (§4.1).

---

## 7. Bonnes pratiques JPA/Hibernate

### 7.1 🔴 `@Data` sur les entités JPA — problème de `equals`/`hashCode`

**Fichiers concernés** : `Train.java`, `TrainLocation.java`, `TrainSystemStatus.java`, `TrainConfiguration.java`, `MediaDatabase.java`, `TrainMessage.java`

**Problème** : Lombok `@Data` génère `equals()` et `hashCode()` basés sur **tous les champs**. Pour les entités JPA :
- Un champ `id` null (entité non persistée) + un champ `id` généré (entité persistée) = deux objets considérés différents alors qu'ils représentent la même ligne
- Hibernate peut lever des erreurs de proxy avec `equals()` sur des champs lazily loaded

**Recommandation** : Remplacer `@Data` par des annotations séparées sur les entités JPA :

```java
@Entity
@Table(name = "train")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"locations", "statuses"}) // évite les requêtes Hibernate non voulues
public class Train {

    @Id
    @EqualsAndHashCode.Include          // equals basé uniquement sur la PK
    @Column(name = "train_id", nullable = false, unique = true)
    private Long trainId;

    // ...
}
```

---

### 7.2 🔴 `@ManyToOne` sans `FetchType.LAZY` — chargement EAGER par défaut

**Fichiers concernés** : `TrainLocation.java`, `TrainSystemStatus.java`, `TrainConfiguration.java`, `MediaDatabase.java`, `TrainMessage.java`

**Problème** : La spécification JPA définit le comportement par défaut de `@ManyToOne` comme **EAGER**. Chaque récupération d'une `TrainLocation` chargera automatiquement le `Train` associé, même si ce dernier n'est pas nécessaire. Avec 1 000 locations, cela génère 1 000 requêtes supplémentaires (N+1).

```java
// Actuel — TrainLocation.java (EAGER implicite)
@ManyToOne
@JoinColumn(name = "train_id", nullable = false)
private Train train;
```

**Recommandation** :

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "train_id", nullable = false)
private Train train;
```

---

### 7.3 🔴 Colonne `database` — mot réservé SQL non échappé

**Fichier concerné** : `Train.java` (ligne 39)

**Problème** : `database` est un **mot réservé** dans la plupart des dialectes SQL (PostgreSQL, MySQL, H2). Sans guillemets, cela peut générer des erreurs à l'exécution selon les versions ou la configuration.

```java
// Actuel — peut échouer selon le dialecte
@Column
private String database;
```

**Recommandation** :

```java
@Column(name = "\"database\"")    // guillemets pour PostgreSQL
private String database;
// ou renommer le champ
@Column(name = "db_version")
private String dbVersion;
```

---

### 7.4 🟠 `CacheWarmupService` — chargement de tous les trains en mémoire

**Fichier concerné** : `CacheWarmupService.java` (ligne 36)

**Problème** : `trainRepository.findAll()` charge tous les trains en mémoire. Pour 10 000 trains, cela représente un chargement massif au démarrage. De plus, les appels `findTopByTrainOrderByUpdateAtDesc(train)` pour chaque train génèrent N requêtes supplémentaires (N+1).

**Recommandation** : Utiliser une requête JPQL unique avec JOIN FETCH et pagination :

```java
@Query("""
    SELECT t FROM Train t
    LEFT JOIN FETCH (
        SELECT l FROM TrainLocation l
        WHERE l.train = t
        ORDER BY l.updateAt DESC
        LIMIT 1
    )
""")
// Ou utiliser une projection native SQL avec DISTINCT ON (PostgreSQL)
@Query(value = """
    SELECT DISTINCT ON (train_id) *
    FROM train_location_status
    ORDER BY train_id, update_at DESC
    """, nativeQuery = true)
List<TrainLocation> findLatestPerTrain();
```

---

### 7.5 🟠 Repository accepte une entité comme paramètre — couplage inutile

**Fichier concerné** : `TrainLocationRepository.java`, `TrainSystemStatusRepository.java`

**Problème** : `findTopByTrainOrderByUpdateAtDesc(Train train)` force le caller à avoir une instance `Train` complète, alors qu'un simple `trainId` suffit.

```java
// Actuel — nécessite un objet Train chargé
Optional<TrainLocation> findTopByTrainOrderByUpdateAtDesc(Train train);
```

**Recommandation** :

```java
// Par trainId directement (Spring Data génère la requête correcte)
Optional<TrainLocation> findTopByTrain_TrainIdOrderByUpdateAtDesc(Long trainId);
```

---

### 7.6 🟡 Absence de `@Version` — pas de protection contre les mises à jour perdues

**Fichier concerné** : `Train.java`

**Problème** : En cas de mises à jour concurrentes d'un même train (ce qui peut arriver si plusieurs workers traitent des messages en parallèle), des mises à jour peuvent se perdre sans que l'application le détecte.

**Recommandation** :

```java
@Version
@Column(name = "version")
private Long version;
```

---

## 8. DTOs, Mappers et transformation de données

### 8.1 🔴 Absence totale de MapStruct — mapping manuel dans les services

**Statut actuel** : ❌ **Aucun mapper (MapStruct, ModelMapper) n'existe dans le projet.**

**Problème** : Le mapping DTO → Entité est effectué manuellement dans les services. Cela :
1. Viole le SRP (le service connaît les champs du DTO ET de l'entité)
2. Génère du code fragile (un champ oublié = bug silencieux)
3. Rend les tests plus verbeux

```java
// Actuel — mapping manuel dans TrainLocationService
TrainLocation entity = TrainLocation.builder()
    .currentStation(dto.getCurrentStation())
    .nextStation(dto.getNextStation())
    .destination(dto.getDestination())
    .train(train)
    .build();
```

**Recommandation** : Ajouter MapStruct et créer des mappers dédiés.

Dépendance Maven :
```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.6.0</version>
</dependency>
```

Mapper :
```java
@Mapper(componentModel = "spring")
public interface TrainLocationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "updateAt", ignore = true)
    @Mapping(target = "train", source = "train")
    TrainLocation toEntity(TrainLocationDto dto, Train train);

    TrainLocationDto toDto(TrainLocation entity);
}
```

Service simplifié :
```java
@Transactional
public void process(TrainLocationDto dto) {
    if (!dedupService.isNew(...)) return;
    Train train = trainResolver.resolveOrCreate(dto.getTrainId());
    TrainLocation entity = mapper.toEntity(dto, train);
    repository.save(entity);
    eventPublisher.publish(dto.getTrainId(), dto);
}
```

---

### 8.2 🟠 Pas de séparation entre DTO entrant (Kafka) et DTO de réponse/événement

**Problème** : `TrainLocationDto` est utilisé à la fois pour :
- Le message Kafka entrant (désérialisation)
- Le payload publié sur `train-events`

Ce double usage couplera le format d'entrée au format de sortie, rendant toute évolution l'un sans l'autre impossible.

**Recommandation** : Séparer les modèles :

```
dto/
├── inbound/
│   ├── TrainLocationMessage.java      ← message Kafka entrant (brut)
│   └── TrainSystemStatusMessage.java
└── event/
    ├── TrainLocationEvent.java        ← événement publié sur train-events
    └── TrainSystemStatusEvent.java
```

---

### 8.3 🟡 `@Data` sur les DTOs — mutable inutilement

**Problème** : Les DTOs entrants n'ont pas vocation à être modifiés après désérialisation. `@Data` génère des setters inutiles.

**Recommandation** : Utiliser des Java Records (Java 16+) pour les DTOs immuables :

```java
public record TrainLocationMessage(
    @NotNull @Positive Long trainId,
    @NotBlank String currentStation,
    @NotBlank String nextStation,
    @NotBlank String destination
) {}
```

> **Note** : Bean Validation fonctionne avec les Records depuis Jakarta EE 10 (compatible Spring Boot 3.x).

---

## 9. Validation des données

### 9.1 🟠 Risque de collision de hash — séparateur absent

**Fichiers concernés** : `TrainLocationService.java` (ligne 32), `TrainSystemStatusService.java` (ligne 33), `CacheWarmupService.java`

**Problème critique** : La concaténation sans séparateur peut générer des **faux positifs de déduplication** :

```
"Lyon" + "Paris" + "Marseille" → "LyonParisMarselle"  ← MÊME hash que
"LyonParis" + "" + "Marseille" → "LyonParisMarselle"   ← Stations différentes !
```

**Recommandation** : Utiliser un séparateur non ambigu :

```java
private static final String FIELD_SEPARATOR = " "; // caractère NUL, impossible dans les données

String content = String.join(FIELD_SEPARATOR,
    dto.getCurrentStation(),
    dto.getNextStation(),
    dto.getDestination()
);
```

---

### 9.2 🟠 Contraintes de taille absentes sur les champs texte

**Fichiers concernés** : `TrainLocationDto.java`, `TrainSystemStatusDto.java`

**Problème** : Aucune contrainte `@Size` n'est définie. Un champ `currentStation` de 10 000 caractères passerait la validation et tenterait d'être inséré en BDD (colonne `VARCHAR(255)` → troncature ou erreur).

**Recommandation** :

```java
@NotBlank
@Size(max = 255, message = "currentStation must not exceed 255 characters")
private String currentStation;
```

---

### 9.3 🟡 Pas de validation inter-champs

**Problème** : Aucune validation métier cross-fields n'est implémentée. Par exemple :
- `nextStation` ne devrait pas être identique à `currentStation`
- `destination` devrait être différente de `currentStation`

**Recommandation** : Créer une annotation de validation personnalisée :

```java
@Constraint(validatedBy = StationsValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidStationSequence {
    String message() default "Station sequence is invalid";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

---

### 9.4 🟡 `TrainMessageValidator` expose un détail d'implémentation dans son nom

**Problème** : La classe s'appelle `TrainMessageValidator` mais valide n'importe quel objet générique (`<T>`). Ce nom est trompeur.

**Recommandation** : Renommer en `BeanValidator` ou `DtoValidator` et extraire l'interface `MessageValidator` (voir §6.3).

---

## 10. Sécurité et configuration

### 10.1 🔴 Credentials en clair dans `application.yaml`

**Fichier concerné** : `application.yaml` (lignes 10-12)

**Problème** : Les credentials PostgreSQL sont en clair dans le fichier de configuration versionné.

```yaml
# PROBLÈME : ne jamais versionner des credentials
datasource:
  username: fleet_user
  password: fleet_pass
```

**Recommandation** : Utiliser des variables d'environnement ou Spring Vault :

```yaml
datasource:
  username: ${DB_USERNAME}
  password: ${DB_PASSWORD:}  # valeur vide par défaut pour les CI sans secret
```

```yaml
# application-docker.yaml — injecté par docker-compose via environment:
datasource:
  username: ${POSTGRES_USER}
  password: ${POSTGRES_PASSWORD}
```

---

### 10.2 🔴 Absence de `SecurityConfig` — règles d'autorisation non définies

**Statut actuel** : ❌ **Aucune `SecurityConfig` n'existe dans le projet.**

**Problème** : `spring-boot-starter-security` est présent et OAuth2 est déclaré, mais aucune règle d'autorisation n'est définie. Spring Security applique sa politique par défaut qui **bloque tous les endpoints HTTP** non authentifiés, y compris `/actuator/health`. Il n'est pas possible de savoir sans test si les endpoints sont correctement protégés ou non.

**Recommandation** :

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .build();
    }
}
```

---

### 10.3 🟠 `management.endpoint.health.show-details: always` — exposition en production

**Fichier concerné** : `application.yaml` (ligne 72)

**Problème** : `show-details: always` expose la configuration interne (URLs de BDD, statut Redis, etc.) à tout appelant non authentifié.

**Recommandation** :

```yaml
management:
  endpoint:
    health:
      show-details: when-authorized  # visible uniquement aux users authentifiés
      roles: ADMIN
```

---

### 10.4 🟠 Absence de `@ConfigurationProperties` — valeurs Kafka hardcodées

**Problème** : Les noms de topics (`"train-location"`, `"train-status"`, etc.) et les paramètres Kafka (retry count = 2) sont des littéraux éparpillés dans le code source.

**Recommandation** :

```java
@ConfigurationProperties(prefix = "tracking.kafka")
@Validated
public record KafkaProperties(
    @NotBlank String trainLocationTopic,
    @NotBlank String trainStatusTopic,
    @NotBlank String trainEventsTopic,
    @NotNull @Positive int retryAttempts
) {}
```

```yaml
tracking:
  kafka:
    train-location-topic: train-location
    train-status-topic: train-status
    train-events-topic: train-events
    retry-attempts: 3
```

Les `@KafkaListener` utilisent alors :
```java
@KafkaListener(topics = "${tracking.kafka.train-location-topic}", ...)
```

---

### 10.5 🟡 `format_sql: true` en production

**Fichier concerné** : `application.yaml` (ligne 22)

**Problème** : `format_sql: true` fait que Hibernate met en cache les requêtes SQL formatées, ce qui consomme de la mémoire inutilement en production.

**Recommandation** : Désactiver en production, activer uniquement en local :

```yaml
# application.yaml
jpa.properties.hibernate.format_sql: false

# application-local.yaml (profil dev)
jpa.properties.hibernate.format_sql: true
```

---

## 11. Gestion des transactions

### 11.1 🔴 `@Transactional` couvre la publication Kafka — rollback impossible

**Fichiers concernés** : `TrainLocationService.java` (ligne 29), `TrainSystemStatusService.java` (ligne 30)

**Problème** : L'annotation `@Transactional` englobe à la fois la persistance JPA **et** la publication Kafka. Or, Kafka n'est pas un gestionnaire de transaction JTA. Si la publication Kafka réussit mais que la transaction JPA est rollbackée (ex. : erreur en fin de méthode), le message sera publié mais la donnée ne sera pas persistée — **incohérence garantie**.

```java
@Transactional
public void process(TrainLocationDto dto) {
    // ...
    trainLocationRepository.save(entity);  // transaction JPA
    eventPublisher.publish(trainIdStr, dto); // Kafka — hors transaction JPA
}
```

**Recommandation** : Utiliser le pattern **Outbox** ou `TransactionSynchronizationManager` pour publier **après** le commit :

```java
@Transactional
public void process(TrainLocationDto dto) {
    // ...
    trainLocationRepository.save(entity);

    // Publication après commit — jamais en cas de rollback
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                eventPublisher.publish(trainIdStr, dto);
            }
        }
    );
}
```

> **Alternative recommandée pour la production** : Pattern Outbox avec une table `outbox_event` persistée dans la même transaction, puis un relayer asynchrone (Debezium CDC ou scheduler).

---

### 11.2 🟠 Pas de `@Transactional(readOnly = true)` sur les requêtes de lecture

**Fichier concerné** : `CacheWarmupService.java`

**Problème** : `trainRepository.findAll()` et les requêtes de warmup s'exécutent sans contexte transactionnel, ce qui peut provoquer des problèmes de chargement lazy si des associations sont accédées.

**Recommandation** : Annoter les méthodes de lecture :

```java
@Transactional(readOnly = true)
public void run(ApplicationArguments args) {
    // findAll() et autres lectures dans une transaction read-only
    // optimisation Hibernate : pas de dirty checking, snapshot, etc.
}
```

---

## 12. Optimisation des performances

### 12.1 🔴 N+1 dans `CacheWarmupService` — O(2n) requêtes BDD

**Fichier concerné** : `CacheWarmupService.java`

**Problème** : Pour N trains :
- 1 requête `findAll()` → N trains
- N requêtes `findTopByTrainOrderByUpdateAtDesc()` → location par train
- N requêtes `findTopByTrainOrderByUpdateAtDesc()` → status par train
- **Total : 2N+1 requêtes**

**Recommandation** : Utiliser `DISTINCT ON` PostgreSQL pour récupérer toutes les dernières locations en une seule requête :

```java
@Query(value = """
    SELECT DISTINCT ON (train_id) id, update_at, current_station, next_station, destination, train_id
    FROM train_location_status
    ORDER BY train_id, update_at DESC
    """, nativeQuery = true)
List<TrainLocation> findLatestLocationPerTrain();
```

**Résultat : 2 requêtes totales au lieu de 2N+1.**

---

### 12.2 🟠 `MessageDigest` instancié à chaque message — contention CPU

**Fichier concerné** : `DedupService.java` (ligne 85)

**Problème** : `MessageDigest.getInstance("SHA-256")` crée une nouvelle instance à chaque appel. Avec des centaines de messages/seconde, cela génère une pression sur le GC.

**Recommandation** : Utiliser un `ThreadLocal<MessageDigest>` :

```java
private static final ThreadLocal<MessageDigest> DIGEST = ThreadLocal.withInitial(() -> {
    try {
        return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException(e);
    }
});

public String computeHash(String content) {
    MessageDigest digest = DIGEST.get();
    digest.reset();                                         // important : reset avant usage
    byte[] bytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(bytes);
}
```

---

### 12.3 🟠 Vérification Redis non atomique — race condition potentielle

**Fichier concerné** : `DedupService.java` (lignes 44-53)

**Problème** : Les opérations `GET` puis `SET` ne sont pas atomiques. Sous haute charge avec plusieurs instances du service (scale horizontal), deux pods peuvent lire le même hash absent simultanément et tous deux considérer le message comme nouveau.

```
Pod A: GET dedup:train-location:1 → null
Pod B: GET dedup:train-location:1 → null
Pod A: SET dedup:train-location:1 = hash1  → persiste
Pod B: SET dedup:train-location:1 = hash1  → persiste aussi ← DOUBLON
```

**Recommandation** : Utiliser `SET NX` (SET if Not eXists) ou un script Lua atomique :

```java
// Option 1 : SETNX pour les premières occurrences
Boolean wasNew = redisTemplate.opsForValue().setIfAbsent(key, newHash);

// Option 2 : Script Lua atomique pour GET+COMPARE+SET
private static final RedisScript<Long> DEDUP_SCRIPT = RedisScript.of("""
    local current = redis.call('GET', KEYS[1])
    if current == ARGV[1] then
        return 0
    end
    redis.call('SET', KEYS[1], ARGV[1])
    return 1
    """, Long.class);

public boolean isNew(String messageType, String trainId, String hashableContent) {
    String key = buildKey(messageType, trainId);
    String newHash = computeHash(hashableContent);
    Long result = redisTemplate.execute(DEDUP_SCRIPT, List.of(key), newHash);
    return result != null && result == 1L;
}
```

---

### 12.4 🟡 `String.valueOf(dto.getTrainId())` répété

**Problème** : Dans chaque service, `String.valueOf(dto.getTrainId())` est calculé plusieurs fois (ligne 31 et 58 dans `TrainLocationService`). Mineur mais signature d'un refactoring partiel.

---

## 13. Testabilité du code

### 13.1 🔴 Tests unitaires manquants pour des classes critiques

**Statut actuel** : ❌

| Classe | Tests unitaires |
|---|---|
| `TrainLocationService` | ❌ Absent |
| `TrainSystemStatusService` | ❌ Absent |
| `TrainLocationConsumer` | ❌ Absent |
| `TrainSystemStatusConsumer` | ❌ Absent |
| `TrainEventPublisher` | ❌ Absent |
| `TrainMessageValidator` | ❌ Absent |
| `CacheWarmupService` | ❌ Absent |
| `DedupService` | ✅ Présent (9 tests) |

Seul `DedupService` est couvert par des tests unitaires. L'objectif de 70% de couverture JaCoCo sera **impossible à atteindre** avec la configuration actuelle.

**Recommandation** : Créer les tests manquants selon le document `testScenario.md`. La priorité va à `TrainLocationService` et `TrainSystemStatusService` car ils encapsulent la logique métier critique.

---

### 13.2 🔴 `computeHash` public — rupture d'encapsulation pour les tests

**Fichier concerné** : `DedupService.java`

**Problème** : `computeHash` et `buildKey` sont `public` uniquement parce que `DedupServiceTest` les utilise directement. C'est un anti-pattern : les tests ne doivent pas forcer les méthodes internes à être publiques.

**Recommandation** : Tester le comportement observable (`isNew`) plutôt que l'implémentation. Si une vérification du hash est nécessaire dans le test, utiliser un argument captor :

```java
// Test comportemental — pas besoin d'appeler computeHash
void isNew_sameContent_returnsFalse() {
    String content = "A|B|C";
    // Premier appel — stocke le hash
    dedupService.isNew("train-location", "1", content);
    reset(valueOps); // réinitialise les interactions Mockito

    // Second appel — même contenu → doublon
    boolean result = dedupService.isNew("train-location", "1", content);

    assertThat(result).isFalse();
    verify(valueOps, never()).set(anyString(), anyString());
}
```

Une fois `computeHash` rendu `private`, l'implémentation peut changer (ex. : SHA-3) sans casser les tests.

---

### 13.3 🟠 `Thread.sleep()` dans les tests d'intégration — tests flaky

**Fichier concerné** : `TrainLocationConsumerIT.java` (ligne 74)

**Problème** :

```java
// Anti-pattern : valeur arbitraire, flaky sur CI lents
Thread.sleep(3_000);
assertThat(trainLocationRepository.findAll()).hasSize(1);
```

**Recommandation** : Remplacer systématiquement par Awaitility :

```java
await().during(Duration.ofSeconds(3))
       .atMost(Duration.ofSeconds(5))
       .untilAsserted(() -> assertThat(trainLocationRepository.findAll()).hasSize(1));
```

---

### 13.4 🟠 Tests d'intégration sans profil Spring dédié

**Problème** : Les tests IT chargent le contexte Spring complet via `@SpringBootTest` sans activer le profil `test`. Les configurations de profil définies dans `application-test.yaml` ne sont pas automatiquement chargées.

**Recommandation** :

```java
@SpringBootTest
@ActiveProfiles("test")     // active application-test.yaml
@Testcontainers
class TrainLocationConsumerIT { ... }
```

---

### 13.5 🟡 `DedupService` couplé à `RedisTemplate` — difficulté de mock

**Problème** : `DedupService` dépend directement de `RedisTemplate`. Si demain le cache est changé (Hazelcast, Caffeine), tous les tests qui mockent `RedisTemplate` devront être réécrits.

**Recommandation** : Ajouter une abstraction `CachePort` entre `DedupService` et Redis.

```java
public interface CachePort {
    Optional<String> get(String key);
    void set(String key, String value);
}

@Component
public class RedisCacheAdapter implements CachePort {
    private final RedisTemplate<String, String> redisTemplate;
    // ...
}
```

`DedupService` dépend de `CachePort` → les tests mockent `CachePort` (1 méthode) au lieu de `RedisTemplate` (interface complexe).

---

## 14. Conventions de nommage

### 14.1 🟠 `TrainLocation` ↔ table `train_location_status` — incohérence

**Fichier concerné** : `TrainLocation.java`

**Problème** : La classe Java s'appelle `TrainLocation` mais la table est `train_location_status`. Ce décalage rend la navigation BDD ↔ code difficile.

**Recommandation** : Aligner : soit renommer la classe en `TrainLocationStatus`, soit renommer la table. Étant donné que la table existe en BDD (Flyway), renommer la classe est plus simple :

```java
@Entity
@Table(name = "train_location_status")
public class TrainLocationStatus { ... }
```

---

### 14.2 🟠 `SystemHealthStatus` vs `SystemStatus` (spec)

**Fichier concerné** : `SystemHealthStatus.java`

**Problème** : La spécification nomme l'enum `SystemStatus` mais il a été implémenté comme `SystemHealthStatus`. Ce n'est pas un bug, mais toute documentation externe qui référence `SystemStatus` sera incohérente avec le code.

**Recommandation** : Aligner avec la spécification ou documenter explicitement le choix de nommage.

---

### 14.3 🟡 `TrainMessageValidator` — nom trompeur

**Fichier concerné** : `TrainMessageValidator.java`

**Problème** : Le nom suggère que cette classe valide les `TrainMessage` (entité JPA) mais elle valide en réalité n'importe quel DTO générique.

**Recommandation** : Renommer en `DtoValidator` ou `BeanConstraintValidator`.

---

### 14.4 🟡 Package `warmup` — terme trop spécifique à une implémentation

**Problème** : Le package s'appelle `warmup` mais la classe pourrait évoluer vers un mécanisme de synchronisation plus général.

**Recommandation** : Renommer en `infrastructure.cache` ou `startup`.

---

### 14.5 🟡 Espace blanc dans `TrackingServiceApplication.java`

**Fichier concerné** : `TrackingServiceApplication.java` (ligne 7)

**Problème** : Un espace blanc (tab) précède `TrackingServiceApplication` sur la ligne de déclaration de classe — il s'agit probablement d'une coquille.

```java
// Ligne 7 — tab avant le nom de classe (à corriger)
public class 	TrackingServiceApplication {
```

---

## 15. Résumé et plan de refactoring priorisé

### Matrice Impact / Effort

| # | Amélioration | Sévérité | Effort | Impact |
|---|---|---|---|---|
| 1 | Ajouter interfaces services + DIP | 🔴 | Moyen | Très élevé |
| 2 | Extraire `TrainResolver` (éliminer duplication) | 🔴 | Faible | Élevé |
| 3 | Template Method pour les consumers | 🔴 | Moyen | Élevé |
| 4 | Corriger `@Transactional` + publication post-commit | 🔴 | Moyen | Critique |
| 5 | Ajouter `SecurityConfig` | 🔴 | Faible | Critique |
| 6 | Externaliser les credentials (variables d'env) | 🔴 | Faible | Critique |
| 7 | `@ManyToOne(fetch = LAZY)` sur toutes les associations | 🔴 | Faible | Élevé |
| 8 | Remplacer `@Data` par `@Getter/@Setter` + `@EqualsAndHashCode(onlyExplicitlyIncluded)` | 🔴 | Faible | Élevé |
| 9 | Ajouter MapStruct (mappers DTO → Entité) | 🟠 | Moyen | Élevé |
| 10 | Ajouter séparateur dans la concaténation de hash | 🔴 | Faible | Critique |
| 11 | Atomicité Redis (script Lua SET+GET) | 🟠 | Moyen | Élevé (multi-instances) |
| 12 | Tests unitaires manquants (services, consumers, publisher) | 🔴 | Élevé | Élevé |
| 13 | Strategy Pattern pour `CacheWarmupContributor` | 🟠 | Moyen | Élevé |
| 14 | `@ConfigurationProperties` pour les topics Kafka | 🟠 | Faible | Moyen |
| 15 | Requête DISTINCT ON pour le warmup N+1 | 🟠 | Moyen | Moyen |
| 16 | Remplacer `Thread.sleep` par Awaitility dans les IT | 🟠 | Faible | Moyen |
| 17 | Corriger la colonne `database` (mot réservé SQL) | 🔴 | Faible | Critique |
| 18 | Supprimer `show-details: always` | 🟠 | Faible | Moyen |
| 19 | Aligner nommage `TrainLocation` ↔ `train_location_status` | 🟠 | Moyen | Moyen |
| 20 | Records Java pour les DTOs immuables | 🟡 | Faible | Faible |

---

### Plan de refactoring en 3 sprints

#### Sprint 1 — Correctifs critiques (1 semaine)
1. ✅ Externaliser credentials → variables d'environnement
2. ✅ Corriger colonne `database` (mot réservé SQL)
3. ✅ Ajouter séparateur dans la construction des contenus hashables
4. ✅ Ajouter `SecurityConfig` avec règles d'autorisation explicites
5. ✅ `@ManyToOne(fetch = LAZY)` sur toutes les entités
6. ✅ Corriger `@Data` → `@Getter/@Setter` + `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`
7. ✅ Remplacer `Thread.sleep` par Awaitility

#### Sprint 2 — Architecture et qualité (2 semaines)
1. ✅ Créer interfaces services (`DeduplicationService`, `TrainLocationProcessor`, etc.)
2. ✅ Extraire `TrainResolver`
3. ✅ Appliquer Template Method pour les consumers
4. ✅ Ajouter MapStruct (mappers)
5. ✅ Corriger `@Transactional` + publication post-commit
6. ✅ `@ConfigurationProperties` pour la configuration Kafka
7. ✅ Écrire les tests unitaires manquants

#### Sprint 3 — Optimisation et extensibilité (2 semaines)
1. ✅ Strategy Pattern pour `CacheWarmupContributor`
2. ✅ Atomicité Redis (script Lua)
3. ✅ Requête DISTINCT ON pour le warmup
4. ✅ `CachePort` abstraction entre DedupService et Redis
5. ✅ Séparer DTOs entrants / événements sortants
6. ✅ Envisager le pattern Outbox pour la garantie de livraison événements
7. ✅ Aligner nommages (`TrainLocationStatus`, `SystemStatus`)
