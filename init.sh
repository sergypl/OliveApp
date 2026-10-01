#!/usr/bin/env bash
set -euo pipefail

echo "Repositorio: $(pwd)"
echo "Estado del harness: bootstrap del backend realizado; app Android pendiente (feature bootstrap-android)."

echo ""
echo "Backend: instalando dependencias..."
cd backend
if [ ! -d .venv ]; then
  python3 -m venv .venv
fi
./.venv/bin/pip install -q -r requirements.txt

echo ""
echo "Backend: verificación base (tests)..."
./.venv/bin/python -m pytest -q

echo ""
echo "Backend: comprobación de arranque y healthcheck..."
DEVEXPERT_API_KEY=bootstrap-check JWT_SECRET=bootstrap-check \
  ./.venv/bin/uvicorn app.main:app --port 8000 >/dev/null 2>&1 &
SERVER_PID=$!
cleanup() { kill "$SERVER_PID" 2>/dev/null || true; }
trap cleanup EXIT

HEALTH_OK=0
for _ in $(seq 1 30); do
  if curl -sf http://127.0.0.1:8000/health >/dev/null 2>&1; then
    HEALTH_OK=1
    break
  fi
  sleep 0.5
done
cleanup
trap - EXIT

if [ "$HEALTH_OK" -ne 1 ]; then
  echo "ERROR: el healthcheck no respondió"
  exit 1
fi
echo "Healthcheck OK (GET /health -> 200)"

echo ""
echo "Comandos reales del backend:"
echo "  - instalación: cd backend && python3 -m venv .venv && ./.venv/bin/pip install -r requirements.txt"
echo "  - verificación: cd backend && ./.venv/bin/python -m pytest -q"
echo "  - arranque: cd backend && DEVEXPERT_API_KEY=... JWT_SECRET=... ./.venv/bin/uvicorn app.main:app --reload"
echo "Comandos de la app Android: pendientes tras la feature bootstrap-android."
