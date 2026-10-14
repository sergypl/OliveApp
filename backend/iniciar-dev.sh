#!/usr/bin/env bash
# Reanuda el backend de desarrollo con la configuración guardada por la
# sesión anterior (PROGRESS.md: "Arrancar el servidor con ese fichero para
# no volver a caducar sesiones"). El fichero de entorno es gitignored y su
# contenido nunca se imprime.
set -euo pipefail
cd "$(dirname "$0")"
set -a
. ./.dev-env
set +a
exec ./.venv/bin/uvicorn app.main:app --port 8000
