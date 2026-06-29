# Test Scenarios — Tracking Service

## Structure

```
1. Unit Tests          → DedupService, TrainMessageValidator, Services, Publisher
2. Integration Tests   → Consumers (Kafka + Redis + PostgreSQL via Testcontainers)
3. Manual / E2E Tests  → Docker Compose stack running end-to-end
```

---

## 1. DedupService — Unit Tests

### 1.1 Nouveau message (aucun hash en cache)
- **Pré-condition** : Redis retourne `null` pour la clé `dedup:train-location:1`
- **Action** : `dedupService.isNew("train-location", "1", "LyonParisMarseille")`
- **Résultat attendu** :
  - Retourne `true`
  - `redisTemplate.opsForValue().set("dedup:train-location:1", <hash>)` est appelé une fois
  - Log INFO contenant la clé

### 1.2 Message identique (doublon)
- **Pré-condition** : Redis retourne le SHA-256 de `"LyonParisMarseille"` pour la clé
- **Action** : `dedupService.isNew("train-location", "1", "LyonParisMarseille")`
- **Résultat attendu** :
  - Retourne `false`
  - Aucun appel `set(...)` effectué
  - Log DEBUG "Duplicate ignored"

### 1.3 Contenu modifié (nouveau hash)
- **Pré-condition** : Redis retourne le hash de `"LyonParisMarseille"` pour la clé
- **Action** : `dedupService.isNew("train-location", "1", "LyonBordeauxToulouse")`
- **Résultat attendu** :
  - Retourne `true`
  - `set(...)` appelé avec le nouveau hash

### 1.4 Redis indisponible — fail-open
- **Pré-condition** : `redisTemplate.opsForValue().get(...)` lève `RuntimeException("connection refused")`
- **Action** : `dedupService.isNew("train-location", "1", "content")`
- **Résultat attendu** :
  - Retourne `true` (fail-open : le message passe quand même)
  - Log ERROR contenant "failing open" et le nom de la clé
  - Aucun appel `set(...)` effectué

### 1.5 seedCache — écriture sans lecture
- **Action** : `dedupService.seedCache("train-location", "42", "ABCDest")`
- **Résultat attendu** :
  - `set("dedup:train-location:42", <hash>)` appelé exactement une fois
  - `get(...)` jamais appelé

### 1.6 seedCache — Redis indisponible (pas d'exception propagée)
- **Pré-condition** : `set(...)` lève une exception
- **Action** : `dedupService.seedCache("train-location", "42", "content")`
- **Résultat attendu** :
  - Aucune exception propagée vers l'appelant
  - Log ERROR contenant la clé

### 1.7 computeHash — déterminisme
- **Action** : appeler `computeHash("hello")` deux fois
- **Résultat attendu** : résultats identiques, longueur = 64 caractères hex

### 1.8 computeHash — unicité
- **Action** : `computeHash("aaa")` vs `computeHash("bbb")`
- **Résultat attendu** : hashs différents

### 1.9 Clés indépendantes par messageType
- **Action** :
  - `isNew("train-location", "1", "content")`
  - `isNew("train-system-status", "1", "content")`
- **Résultat attendu** :
  - Clés Redis distinctes : `dedup:train-location:1` et `dedup:train-system-status:1`
  - Les deux retournent `true` (pas d'interférence)

---

## 2. TrainMessageValidator — Unit Tests

### 2.1 DTO valide (TrainLocationDto)
- **Input** : `trainId=1, currentStation="Lyon", nextStation="Paris", destination="Marseille"`
- **Résultat attendu** : `validate(dto)` retourne `true`, aucun log ERROR

### 2.2 trainId nul
- **Input** : `trainId=null`
- **Résultat attendu** : `false` + log ERROR contenant "trainId: trainId is required"

### 2.3 trainId négatif ou zéro
- **Input** : `trainId=-1`
- **Résultat attendu** : `false` + log ERROR contenant "trainId: trainId must be a positive number"

### 2.4 currentStation vide
- **Input** : `trainId=1, currentStation="", nextStation="Paris", destination="Marseille"`
- **Résultat attendu** : `false` + log ERROR mentionnant `currentStation`

### 2.5 Plusieurs violations simultanées
- **Input** : `trainId=null, currentStation="", nextStation=null, destination=null`
- **Résultat attendu** : `false` + log ERROR listant toutes les violations en une seule ligne

### 2.6 DTO système — pacisStatus nul
- **Input** : `TrainSystemStatusDto(trainId=1, pacisStatus=null, cctvStatus=OK, rearViewStatus=OK)`
- **Résultat attendu** : `false` + log ERROR mentionnant `pacisStatus`

### 2.7 DTO système — enum invalide (désérialisation)
- **Input JSON** : `{"trainId":1,"pacisStatus":"INVALID","cctvStatus":"OK","rearViewStatus":"OK"}`
- **Résultat attendu** : le consumer catch la `JsonProcessingException`, log ERROR, message ignoré (pas d'appel au validator)

---

## 3. TrainLocationService — Unit Tests

### 3.1 Message nouveau → persistance + publication
- **Mocks** : `dedupService.isNew(...)` retourne `true`, `trainRepository.findById(1)` retourne un `Train`
- **Action** : `trainLocationService.process(dto)`
- **Résultat attendu** :
  - `trainLocationRepository.save(...)` appelé une fois
  - `eventPublisher.publish("1", dto)` appelé une fois
  - Entité sauvegardée avec `currentStation`, `nextStation`, `destination` corrects

### 3.2 Message doublon → aucune persistance
- **Mocks** : `dedupService.isNew(...)` retourne `false`
- **Action** : `trainLocationService.process(dto)`
- **Résultat attendu** :
  - `trainLocationRepository.save(...)` jamais appelé
  - `eventPublisher.publish(...)` jamais appelé

### 3.3 Train inconnu → création d'un placeholder
- **Mocks** : `dedupService.isNew(...)` retourne `true`, `trainRepository.findById(99)` retourne `Optional.empty()`
- **Action** : `trainLocationService.process(dto avec trainId=99)`
- **Résultat attendu** :
  - `trainRepository.save(...)` appelé avec `trainId=99, name="UNKNOWN-99"`
  - Log WARN contenant "creating placeholder"
  - Location sauvegardée ensuite normalement

---

## 4. TrainSystemStatusService — Unit Tests

### 4.1 Message nouveau → persistance avec UpdateStatus.UPDATED
- **Mocks** : `dedupService.isNew(...)` retourne `true`, train trouvé
- **Résultat attendu** :
  - Entité sauvegardée avec `updateStatus = UPDATED`
  - `eventPublisher.publish(...)` appelé

### 4.2 Même statut retransmis → ignoré (doublon)
- **Mocks** : `dedupService.isNew(...)` retourne `false`
- **Résultat attendu** : aucune persistance, aucune publication

### 4.3 Contenu hash = concaténation des noms d'enum
- **Vérification** : le hash transmis à DedupService est `"OK" + "FAILURE" + "PARTIALLY_OK"` et non les ordinals ou autre
- **Action** : capturer l'argument passé à `dedupService.isNew(...)` via Mockito `ArgumentCaptor`
- **Résultat attendu** : argument = `"OKFAILUREPARTIALLY_OK"`

---

## 5. TrainEventPublisher — Unit Tests

### 5.1 Publication réussie
- **Mock** : `kafkaTemplate.send(...)` retourne un future complété
- **Action** : `publisher.publish("1", dto)`
- **Résultat attendu** :
  - `kafkaTemplate.send("train-events", "1", <json>)` appelé une fois
  - Le payload JSON contient les champs du DTO

### 5.2 Échec de sérialisation (objet non-sérialisable)
- **Action** : passer un objet qui lève `JsonProcessingException`
- **Résultat attendu** :
  - Aucune exception propagée
  - Log ERROR contenant "Failed to serialise event payload"
  - `kafkaTemplate.send(...)` jamais appelé

### 5.3 Échec Kafka asynchrone
- **Mock** : future du `send(...)` complété exceptionnellement
- **Résultat attendu** :
  - Log ERROR contenant "Failed to publish event"

---

## 6. CacheWarmupService — Unit Tests

### 6.1 Train avec historique location et statut
- **Mocks** :
  - `trainRepository.findAll()` retourne `[Train(id=1)]`
  - `trainLocationRepository.findTopByTrainOrderByUpdateAtDesc(...)` retourne une `TrainLocation`
  - `trainSystemStatusRepository.findTopByTrainOrderByUpdateAtDesc(...)` retourne un `TrainSystemStatus`
- **Action** : `cacheWarmupService.run(args)`
- **Résultat attendu** :
  - `dedupService.seedCache("train-location", "1", ...)` appelé une fois
  - `dedupService.seedCache("train-system-status", "1", ...)` appelé une fois
  - 2 logs INFO "Warmed ... cache for trainId=1"

### 6.2 Train sans historique
- **Mocks** : repositories retournent `Optional.empty()`
- **Résultat attendu** : aucun appel à `dedupService.seedCache(...)`

### 6.3 Aucun train en base
- **Mocks** : `trainRepository.findAll()` retourne `[]`
- **Résultat attendu** : aucun appel aux autres repositories; log INFO "0 trains processed"

---

## 7. TrainLocationConsumer — Unit Tests

### 7.1 Message JSON valide → traitement complet
- **Mock** : `validator.validate(...)` retourne `true`, `service.process(...)` ne fait rien
- **Action** : `consumer.consume(validJson)`
- **Résultat attendu** : `service.process(dto)` appelé une fois

### 7.2 JSON malformé → ignoré
- **Action** : `consumer.consume("{invalid}")`
- **Résultat attendu** :
  - Log ERROR "Failed to deserialise"
  - `validator.validate(...)` jamais appelé
  - `service.process(...)` jamais appelé

### 7.3 DTO invalide → ignoré après validation
- **Mock** : `validator.validate(...)` retourne `false`
- **Résultat attendu** : `service.process(...)` jamais appelé

---

## 8. Integration Tests — TrainLocationConsumerIT (Testcontainers)

> Conteneurs : PostgreSQL 15, Kafka (apache/kafka:3.7.0), Redis 7

### 8.1 Nouveau message → persisté en base
- **Setup** : Train `id=100` pré-inséré
- **Action** : publier `{"trainId":100,"currentStation":"Lyon","nextStation":"Paris","destination":"Marseille"}` sur `train-location`
- **Vérification** (Awaitility, timeout 15s) :
  - `trainLocationRepository.findAll()` retourne exactement 1 entrée
  - L'entrée a `currentStation="Lyon"`, `nextStation="Paris"`, `destination="Marseille"`

### 8.2 Message identique → non persisté deux fois
- **Action** : publier le même JSON deux fois successivement
- **Vérification** :
  - Après le premier message : 1 entrée en base
  - Après le second message (attente 3s supplémentaires) : toujours 1 entrée

### 8.3 Message modifié → nouvelle entrée persistée
- **Action** :
  1. Publier `destination="Marseille"` → attendre 1 entrée
  2. Publier `destination="Bordeaux"` → attendre 2 entrées
- **Vérification** : 2 entrées distinctes en base, valeurs correctes

### 8.4 Événement publié sur `train-events`
- **Setup** : consommateur de test souscrit à `train-events` avant l'envoi
- **Action** : publier un message valide sur `train-location`
- **Vérification** (Awaitility, timeout 20s) :
  - Le consommateur de test reçoit au moins 1 message
  - La clé Kafka = `"100"` (trainId)
  - Le corps JSON contient `"currentStation":"Nice"`

### 8.5 Message JSON invalide → non persisté, pas d'erreur fatale
- **Action** : publier `"not-json"` sur `train-location`
- **Vérification** :
  - Aucune entrée en base
  - Le consumer continue à traiter les messages suivants (résilience)

### 8.6 DTO avec trainId nul → non persisté
- **Action** : publier `{"trainId":null,"currentStation":"A","nextStation":"B","destination":"C"}`
- **Vérification** :
  - Aucune entrée en base
  - Log ERROR visible

---

## 9. Integration Tests — TrainSystemStatusConsumerIT (Testcontainers)

### 9.1 Nouveau statut → persisté avec UpdateStatus.UPDATED
- **Action** : publier `{"trainId":200,"pacisStatus":"OK","cctvStatus":"OK","rearViewStatus":"OK"}` sur `train-status`
- **Vérification** :
  - 1 entrée en base avec `pacisStatus=OK`, `cctvStatus=OK`, `rearViewStatus=OK`
  - `updateStatus = UPDATED`

### 9.2 Même statut retransmis → non persisté (doublon)
- **Action** : publier le même JSON deux fois
- **Vérification** : toujours 1 entrée en base après les deux envois

### 9.3 Statut modifié → nouvelle entrée
- **Action** :
  1. Publier `pacisStatus=OK` → 1 entrée
  2. Publier `pacisStatus=FAILURE` → 2 entrées
- **Vérification** : 2 entrées, valeurs correctes

### 9.4 Enum invalide dans le JSON → ignoré
- **Action** : publier `{"trainId":200,"pacisStatus":"UNKNOWN","cctvStatus":"OK","rearViewStatus":"OK"}`
- **Vérification** : aucune entrée en base, service toujours fonctionnel

### 9.5 Déduplication indépendante des types de messages
- **Setup** : même `trainId=200` pour les deux topics
- **Action** :
  1. Publier `{"trainId":200,...}` sur `train-location` → 1 location sauvée
  2. Publier `{"trainId":200,...}` sur `train-status` → 1 statut sauvé
  3. Republier les deux mêmes messages → aucune nouvelle entrée
- **Vérification** : 1 location + 1 statut en base (pas d'interférence entre les clés Redis)

---

## 10. Tests de résilience

### 10.1 Redis redémarre pendant le traitement
- **Scénario** :
  1. Consumer actif, Redis disponible
  2. Stopper le conteneur Redis
  3. Publier 5 messages sur `train-location`
  4. Redémarrer Redis
  5. Publier 5 nouveaux messages
- **Résultat attendu** :
  - Les 5 messages pendant la panne sont persistés (fail-open)
  - Log ERROR "Redis unavailable — failing open" pour chaque message pendant la panne
  - Après redémarrage, déduplication normale reprend

### 10.2 PostgreSQL indisponible → message envoyé sur DLT après 3 tentatives
- **Scénario** :
  1. Stopper le conteneur PostgreSQL
  2. Publier 1 message valide sur `train-location`
- **Résultat attendu** :
  - 3 tentatives de traitement
  - Message redirigé vers `train-location.DLT`
  - Aucune exception non traitée

### 10.3 Kafka producer indisponible → log ERROR, pas d'exception propagée
- **Scénario** : brokers Kafka inaccessibles lors d'une publication vers `train-events`
- **Résultat attendu** : log ERROR "Failed to publish event", flux de traitement non interrompu

---

## 11. Tests de démarrage — CacheWarmupService

### 11.1 Warmup complet au démarrage de l'application
- **Setup** : 3 trains en base, chacun avec une location et un statut
- **Action** : démarrer l'application
- **Vérification** :
  - 6 clés Redis présentes : `dedup:train-location:{id}` et `dedup:train-system-status:{id}` pour chaque train
  - Logs INFO "Cache warmup complete — 3 trains processed"

### 11.2 Warmup partiel (train sans historique)
- **Setup** : 1 train sans `TrainLocation` ni `TrainSystemStatus` en base
- **Vérification** : aucune clé Redis créée pour ce train, pas d'erreur

### 11.3 Redis indisponible au démarrage
- **Setup** : Redis arrêté avant le lancement de l'application
- **Résultat attendu** :
  - L'application démarre quand même (fail-open)
  - Log ERROR pour chaque `seedCache` échoué
  - Les consumers Kafka fonctionnent normalement

---

## 12. Tests de sécurité / Actuator

### 12.1 `/actuator/health` accessible sans authentification
- **Action** : `GET http://localhost:8001/actuator/health`
- **Résultat attendu** : `200 OK` avec `{"status":"UP","components":{"redis":{"status":"UP"},"kafka":{"status":"UP"}}}`

### 12.2 Endpoint REST protégé — requête sans token
- **Action** : `GET http://localhost:8001/actuator/metrics` sans Bearer token
- **Résultat attendu** : `401 Unauthorized` (géré par OAuth2 Resource Server)

---

## 13. Tests de charge (non automatisés)

### 13.1 Throughput — 500 messages/seconde sans perte
- **Outil** : `kafka-producer-perf-test.sh`
- **Setup** : 500 msg/s pendant 60s sur `train-location`, 10 trains distincts
- **Vérification** :
  - Aucun message en retard (consumer lag ~0)
  - Aucun message sur le DLT
  - CPU < 70%, mémoire stable

### 13.2 Déduplication sous charge
- **Setup** : 1 000 messages identiques (même hash) pour le même train
- **Vérification** :
  - 1 seule ligne en base
  - Latence Redis < 2ms par opération

### 13.3 Redémarrage consumer sans perte de messages
- **Setup** : produire 1 000 messages, redémarrer le consumer à mi-chemin
- **Vérification** :
  - Tous les messages non-dupliqués sont finalement traités
  - Pas de double insertion (auto-offset-reset=earliest + déduplication Redis)
