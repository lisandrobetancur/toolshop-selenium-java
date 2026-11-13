#!/usr/bin/env bash
# Levanta Toolshop en Docker, siembra la base de datos y espera a que la API y la UI respondan.
# Uso: ci/toolshop/start.sh   (UI en http://localhost:4200, API en http://localhost:8091)
set -euo pipefail

compose=(docker compose -f "$(dirname "$0")/docker-compose.yml")

"${compose[@]}" up -d --quiet-pull

echo "Sembrando la base de datos..."
# PHP-FPM puede tardar unos segundos en aceptar comandos después de arrancar el contenedor.
for attempt in $(seq 1 20); do
  if "${compose[@]}" exec -T laravel-api php artisan migrate:fresh --seed --force; then
    break
  fi
  if [ "$attempt" -eq 20 ]; then
    echo "No se pudo sembrar la base de datos." >&2
    exit 1
  fi
  echo "La API todavía no está lista (intento $attempt), reintentando..."
  sleep 3
done

echo "Esperando a la API..."
curl --fail --silent --show-error --retry 40 --retry-delay 3 --retry-all-errors \
  http://localhost:8091/status
echo

# La UI corre con "ng serve": la primera compilación tarda un par de minutos.
echo "Esperando a la UI..."
curl --fail --silent --show-error --output /dev/null --retry 90 --retry-delay 3 --retry-all-errors \
  http://localhost:4200/
echo "Toolshop listo: UI http://localhost:4200 · API http://localhost:8091"
