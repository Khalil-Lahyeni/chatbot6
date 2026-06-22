# Chapitre explicatif du projet `alert-service`

Ce projet est un service d'alertes basé sur NestJS, Kafka, Prisma et WebSocket. Il reçoit des messages Kafka, les traite, les stocke dans une base de données PostgreSQL et les diffuse en temps réel via Socket.IO.

---

## 1. Structure générale

- `src/main.ts`
  - Point d'entrée de l'application.
  - Crée l'application NestJS et écoute sur le port `4100` ou la variable d'environnement `PORT`.

- `src/app.module.ts`
  - Module principal du service.
  - Importe `AlertModule`, `KafkaModule` et `WebsocketModule`.

- `src/alert/`
  - Contient la logique métier des alertes.
  - Services et handlers pour chaque type d'alerte.

- `src/kafka/`
  - Contient le consommateur Kafka.
  - Définit le flux de réception des messages Kafka.

- `src/websocket/`
  - Définit le gateway WebSocket pour envoyer les alertes aux clients.

- `src/config/`
  - Configuration Prisma (`prisma.service.ts`) et Kafka (`kafka.config.ts`).

- `prisma/schema.prisma`
  - Modèle de données pour PostgreSQL.

---

## 2. Flux principal de l'application

1. Le service démarre avec NestJS via `src/main.ts`.
2. `AppModule` initialise les modules Kafka et WebSocket.
3. `KafkaConsumer` se connecte au broker Kafka et s'abonne aux topics configurés.
4. Pour chaque message reçu, le consommateur convertit le payload JSON et choisit le handler correspondant.
5. Chaque handler :
   - diffuse une alerte en temps réel via `AlertGateway`,
   - enregistre l'alerte dans la base de données avec un service Prisma.

---

## 3. Modules clés

### 3.1 `KafkaModule`

- Définit le consommateur Kafka (`KafkaConsumer`).
- Importe `AlertModule` et `WebsocketModule` pour disposer des handlers et du gateway.

### 3.2 `AlertModule`

- Fournit les services de création en base de données et les handlers Kafka.
- Exporte `KAFKA_HANDLERS` qui regroupe :
  - `RealAlertHandler`
  - `AnomalyAlertHandler`
  - `PredictedAlertHandler`
- Ces handlers sont associés aux topics Kafka configurés.

### 3.3 `WebsocketModule`

- Fournit `AlertGateway`.
- Le gateway émet des événements Socket.IO vers tous les clients connectés.

---

## 4. Gestion des alertes

### `AlertGateway`

- `sendAlert(alert)` : émet l'événement `new-alert`.
- `sendRealAlert(data)` : émet `real-alert`.
- `sendAnomalyAlert(data)` : émet `anomaly-alert`.
- `sendPredictedAlert(data)` : émet `predicted-alert`.

Le gateway est utilisé par les handlers pour notifier immédiatement les clients.

#### 4.1 Détails WebSocket

- Le gateway est défini avec le décorateur `@WebSocketGateway({ cors: { origin: '*' } })`.
- Cela crée un serveur Socket.IO intégré à NestJS.
- Le champ `@WebSocketServer() server!: Server;` expose l'instance Socket.IO.
- Chaque méthode de `AlertGateway` utilise `this.server.emit(...)` pour envoyer un événement à tous les clients connectés.
- Les événements sont broadcastés globalement : il n’y a pas de rooms ou de namespace spécifique dans l’implémentation actuelle.
- Les clients doivent se connecter au même hôte/port que le serveur NestJS, puis écouter les événements :
  - `new-alert`
  - `real-alert`
  - `anomaly-alert`
  - `predicted-alert`
- Les données envoyées sont des objets JSON construits par les handlers, par exemple : `{ type: 'REAL', ...data }`.
- Le CORS est ouvert (`origin: '*'`), donc tous les clients peuvent se connecter depuis n’importe quelle origine.
- Cette configuration est simple mais peut être renforcée en production avec une liste blanche d’origines et une authentification WebSocket.

### Handlers Kafka

Chaque handler implémente l'interface `KafkaHandler` et gère un topic spécifique.

- `RealAlertHandler`
  - Topic : `real-alert-topic`
  - Sauvegarde `RealAlert` en base.
  - Émet `real-alert` via WebSocket.

- `AnomalyAlertHandler`
  - Topic : `anomaly-alert-topic`
  - Sauvegarde `AnomalyAlert` en base.
  - Émet `anomaly-alert` via WebSocket.

- `PredictedAlertHandler`
  - Topic : `predicted-alert-topic`
  - Sauvegarde `PredictedAlert` en base.
  - Émet `predicted-alert` via WebSocket.

---

## 5. Sauvegarde des données

### `PrismaService`

- Fournit la connexion à PostgreSQL.
- Se connecte lors de l'initialisation du module et se déconnecte à la fermeture.

### Modèles Prisma

- `Alert`
  - `id`, `message`, `type`, `status`, `createdAt`

- `RealAlert`
  - Contient `id`, et une alerte liée à un train avec `trainId`, `type`, `callState`, `description`, `car`, `carName`, `intercom`, `cameras`.

- `AnomalyAlert`
  - Contient `id`, `probability`, `type`, `trainId`, `description`.

- `PredictedAlert`
  - Contient `probability`, `type`, `trainId`, `description`.

---

## 6. Configuration Kafka

- Broker local : `localhost:9092`
- Client ID : `alert-service`
- Group ID : `alert-consumer-group`
- Topics :
  - `real-alert-topic`
  - `anomaly-alert-topic`
  - `predicted-alert-topic`

---

## 7. Lancer le projet

1. Installer les dépendances : `npm install`
2. Démarrer le service en développement : `npm run start:dev`
3. S'assurer que Kafka et PostgreSQL sont accessibles.
4. Vérifier que les clients WebSocket reçoivent les événements :
   - `new-alert`
   - `real-alert`
   - `anomaly-alert`
   - `predicted-alert`

---

## 8. Notes importantes

- Les handlers Kafka sont responsables à la fois de la diffusion en temps réel et de l'écriture en base.
- La configuration actuelle suppose un broker Kafka local.
- Le projet utilise NestJS 11, Prisma 6 et Socket.IO.

---

## 9. Suggestions pour aller plus loin

- Ajouter un `Controller` HTTP pour consulter les alertes depuis l'API.
- Sécuriser le gateway WebSocket (authentification, CORS restreint).
- Ajouter un module de logs plus structuré.
- Gérer les erreurs de base de données et repli en cas d'échec de persistence.
