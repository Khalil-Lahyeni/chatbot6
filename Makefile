.PHONY: up down down-clean export-realm logs ps

# Démarrer tous les services
up:
	docker compose up -d
	@echo "✅ Services démarrés"
	@echo "   Frontend  → http://localhost:4200"
	@echo "   Keycloak  → http://localhost:8080"
	@echo "   Grafana   → http://localhost:3001"

up-db:
	docker compose up postgres pgadmin redis
db-init:
	@echo "⏳ Initialisation PostgreSQL..."
	@until docker compose exec -T postgres sh -lc 'pg_isready -U "$$POSTGRES_USER" -d keycloak_db >/dev/null 2>&1'; do sleep 1; done
	@docker compose exec -T postgres sh -lc 'psql -U "$$POSTGRES_USER" -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE railway_db"' >/dev/null 2>&1 || true
	@docker compose exec -T postgres sh -lc 'psql -U "$$POSTGRES_USER" -d postgres -v ON_ERROR_STOP=1 -c "GRANT ALL PRIVILEGES ON DATABASE railway_db TO \"$$POSTGRES_USER\""' >/dev/null
	@echo "✅ Base railway_db prête"	
# Arrêter sans supprimer les volumes (données conservées)
down:
	docker compose down
	@echo "✅ Services arrêtés — données conservées"

# Arrêter ET supprimer les volumes (export automatique avant)
down-clean: export-realm
	docker compose down -v
	@echo "⚠️  Volumes supprimés — realm exporté et commité avant suppression"

# Voir les logs
logs:
	docker compose logs -f

# Voir l'état des conteneurs
ps:
	docker compose ps