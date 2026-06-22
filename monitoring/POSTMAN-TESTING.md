# API Train CRUD - Guide de test avec Postman

## Base URL
```
http://localhost:8001
```

## Endpoints

### 1. Créer un Train (POST)
**URL:** `POST http://localhost:8001/api/actia/trains`

**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "name": "Train A",
  "mission": "Transport de fret",
  "baseline": "1.0.0",
  "diversity": "haute",
  "database": "postgres_01"
}
```

**Réponse (201 Created):**
```json
{
  "id": 1,
  "name": "Train A",
  "mission": "Transport de fret",
  "baseline": "1.0.0",
  "diversity": "haute",
  "database": "postgres_01",
  "updateAt": "2026-06-15T10:30:45.123456Z"
}
```

---

### 2. Récupérer un Train par ID (GET)
**URL:** `GET http://localhost:8001/api/actia/trains/{id}`

**Exemple:**
```
GET http://localhost:8001/api/actia/trains/1
```

**Réponse (200 OK):**
```json
{
  "id": 1,
  "name": "Train A",
  "mission": "Transport de fret",
  "baseline": "1.0.0",
  "diversity": "haute",
  "database": "postgres_01",
  "updateAt": "2026-06-15T10:30:45.123456Z"
}
```

**Réponse en cas d'absence (404 Not Found):**
```json
{
  "timestamp": "2026-06-15T10:35:12.789123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Train not found: TRAIN-999",
  "path": "/api/actia/trains/TRAIN-999"
}
```

---

### 3. Récupérer tous les Trains avec Pagination (GET)
**URL:** `GET http://localhost:8001/api/actia/trains`

**Query Parameters:**
- `page`: Numéro de la page (défaut: 0)
- `size`: Nombre d'éléments par page (défaut: 20)
- `sort`: Champ et direction du tri (défaut: `updateAt,desc`)

**Exemples:**
```
GET http://localhost:8001/api/actia/trains
GET http://localhost:8001/api/actia/trains?page=0&size=10
GET http://localhost:8001/api/actia/trains?page=0&size=10&sort=mission,asc
GET http://localhost:8001/api/actia/trains?sort=updateAt,desc&sort=mission,asc
```

**Réponse (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "name": "Train A",
      "mission": "Transport de fret",
      "baseline": "1.0.0",
      "diversity": "haute",
      "database": "postgres_01",
      "updateAt": "2026-06-15T10:30:45.123456Z"
    },
    {
      "id": 2,
      "name": "Train B",
      "mission": "Transport de passagers",
      "mission": "Transport de passagers",
      "baseline": "2.0.1",
      "diversity": "moyenne",
      "database": "postgres_02",
      "updateAt": "2026-06-15T10:32:10.456789Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalPages": 1,
  "totalElements": 2,
  "last": true,
  "size": 20,
  "number": 0,
  "sort": {
    "empty": false,
    "sorted": true,
    "unsorted": false
  },
  "numberOfElements": 2,
  "first": true,
  "empty": false
}
```

---

### 4. Mettre à jour un Train (PUT)
**URL:** `PUT http://localhost:8001/api/actia/trains/{id}`

**Exemple:**
```
PUT http://localhost:8001/api/actia/trains/1
```

**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "name": "Train A",
  "mission": "Transport de fret haute vitesse",
  "baseline": "1.0.1",
  "diversity": "très haute",
  "database": "postgres_01_updated"
}
```

**Réponse (200 OK):**
```json
{
  "id": 1,
  "name": "Train A",
  "mission": "Transport de fret haute vitesse",
  "baseline": "1.0.1",
  "diversity": "très haute",
  "database": "postgres_01_updated",
  "updateAt": "2026-06-15T10:30:45.123456Z"
}
```

**Note:** `updateAt` et `id` ne sont pas mis à jour (protégés dans le mapper).

---

### 5. Supprimer un Train (DELETE)
**URL:** `DELETE http://localhost:8001/api/actia/trains/{id}`

**Exemple:**
```
DELETE http://localhost:8001/api/actia/trains/1
```

**Réponse (204 No Content):**
```
[Empty body]
```

**Réponse en cas d'absence (404 Not Found):**
```json
{
  "timestamp": "2026-06-15T10:40:00.000000Z",
  "status": 404,
  "error": "Not Found",
  "message": "Train not found: TRAIN-999",
  "path": "/api/actia/trains/TRAIN-999"
}
```

---

## Scénario de test complet

### Étape 1: Créer 3 trains
```bash
POST http://localhost:8001/api/actia/trains
Body: { "name": "Train A", "mission": "Fret", "baseline": "1.0", "diversity": "haute", "database": "db1" }

POST http://localhost:8001/api/actia/trains
Body: { "name": "Train B", "mission": "Passagers", "baseline": "2.0", "diversity": "moyenne", "database": "db2" }

POST http://localhost:8001/api/actia/trains
Body: { "name": "Train C", "mission": "Mixte", "baseline": "1.5", "diversity": "basse", "database": "db3" }
```

### Étape 2: Récupérer un train spécifique
```bash
GET http://localhost:8001/api/actia/trains/1
```

### Étape 3: Lister tous les trains avec pagination
```bash
GET http://localhost:8001/api/actia/trains?page=0&size=10&sort=updateAt,desc
```

### Étape 4: Mettre à jour un train
```bash
PUT http://localhost:8001/api/actia/trains/2
Body: { "name": "Train B", "mission": "Passagers express", "baseline": "2.1", "diversity": "haute", "database": "db2_updated" }
```

### Étape 5: Supprimer un train
```bash
DELETE http://localhost:8001/api/actia/trains/3
```

### Étape 6: Vérifier la liste après suppression
```bash
GET http://localhost:8001/api/actia/trains?page=0&size=10
```

---

## Codes HTTP possibles

| Code | Signification | Cas d'usage |
|------|---------------|-----------|
| 200 OK | Succès | GET, PUT (lecture/mise à jour réussie) |
| 201 Created | Ressource créée | POST (création réussie) |
| 204 No Content | Succès sans contenu | DELETE (suppression réussie) |
| 404 Not Found | Ressource absent | GET/PUT/DELETE avec ID inexistant |
| 500 Internal Server Error | Erreur serveur | Erreur générale non gérée |

---

## Collection Postman (JSON)

Vous pouvez importer cette collection dans Postman :

```json
{
  "info": {
    "name": "Train CRUD API",
    "description": "API CRUD pour la gestion des trains",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Create Train",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\"name\": \"Train A\", \"mission\": \"Transport de fret\", \"baseline\": \"1.0.0\", \"diversity\": \"haute\", \"database\": \"postgres_01\"}"
        },
        "url": {
          "raw": "http://localhost:8001/api/actia/trains",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8001",
          "path": ["api", "actia", "trains"]
        }
      }
    },
    {
      "name": "Get Train by ID",
      "request": {
        "method": "GET",
        "url": {
          "raw": "http://localhost:8001/api/actia/trains/1",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8001",
          "path": ["api", "actia", "trains", "1"]
        }
      }
    },
    {
      "name": "Get All Trains",
      "request": {
        "method": "GET",
        "url": {
          "raw": "http://localhost:8001/api/actia/trains?page=0&size=20&sort=updateAt,desc",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8001",
          "path": ["api", "actia", "trains"],
          "query": [
            {
              "key": "page",
              "value": "0"
            },
            {
              "key": "size",
              "value": "20"
            },
            {
              "key": "sort",
              "value": "updateAt,desc"
            }
          ]
        }
      }
    },
    {
      "name": "Update Train",
      "request": {
        "method": "PUT",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\"name\": \"Train A\", \"mission\": \"Transport de fret haute vitesse\", \"baseline\": \"1.0.1\", \"diversity\": \"très haute\", \"database\": \"postgres_01_updated\"}"
        },
        "url": {
          "raw": "http://localhost:8001/api/actia/trains/1",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8001",
          "path": ["api", "actia", "trains", "1"]
        }
      }
    },
    {
      "name": "Delete Train",
      "request": {
        "method": "DELETE",
        "url": {
          "raw": "http://localhost:8001/api/actia/trains/1",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8001",
          "path": ["api", "actia", "trains", "1"]
        }
      }
    }
  ]
}
```

---

## Instructions pour Postman

1. **Créer une nouvelle collection** ou importer le JSON ci-dessus
2. **Définir une variable d'environnement** (optionnel mais recommandé):
   - Variable: `baseUrl`
   - Valeur: `http://localhost:8001`
   - Utiliser dans les URLs: `{{baseUrl}}/api/actia/trains`
3. **Tester chaque endpoint** dans l'ordre du scénario de test complet
4. **Vérifier les réponses** contre les exemples fournis
5. **Inspecter les logs** de l'application pour les erreurs

---

## Prérequis

- Application Spring Boot démarrée sur le port `8001`
- PostgreSQL accessible sur `localhost:5431` avec la base `monitoring_db`
- Postman installé

