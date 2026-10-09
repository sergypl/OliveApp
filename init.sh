#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"

echo "Repositorio: $ROOT"
echo "Estado del harness: bootstrap del backend y de la app Android realizado."

echo ""
echo "== Backend =="
echo "Instalando dependencias..."
cd "$ROOT/backend"
if [ ! -d .venv ]; then
  python3 -m venv .venv
fi
./.venv/bin/pip install -q -r requirements.txt

echo "Verificación base (tests)..."
./.venv/bin/python -m pytest -q

echo "Comprobación de arranque, healthcheck y datos sembrados..."
DEVEXPERT_API_KEY=bootstrap-check JWT_SECRET=bootstrap-check \
  ./.venv/bin/uvicorn app.main:app --port 8000 >/dev/null 2>&1 &
SERVER_PID=$!
disown "$SERVER_PID" 2>/dev/null || true
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

if [ "$HEALTH_OK" -ne 1 ]; then
  echo "ERROR: el healthcheck no respondió"
  exit 1
fi
echo "Healthcheck OK (GET /health -> 200)"

# Desde `inicio-de-sesion` los datos exigen token de sesión (cambio de
# comportamiento declarado en esa feature): se crea o reutiliza una cuenta de
# verificación y se comprueban los datos sembrados con su token.
curl -sf -X POST http://127.0.0.1:8000/api/v1/registro \
  -H 'Content-Type: application/json' \
  -d '{"email":"verificacion-init@ejemplo.com","contrasena":"segura123"}' >/dev/null 2>&1 || true
TOKEN=$(curl -sf -X POST http://127.0.0.1:8000/api/v1/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"verificacion-init@ejemplo.com","contrasena":"segura123"}' \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['token'])" 2>/dev/null)
SOCIOS=$(curl -sf -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8000/api/v1/socios 2>/dev/null)
CONTADOS=$(echo "$SOCIOS" | grep -o 'socio-00' | wc -l | tr -d ' ')
if [ -z "$TOKEN" ] || [ "$CONTADOS" != "2" ]; then
  echo "ERROR: los datos sembrados no responden (GET /api/v1/socios con token)"
  exit 1
fi
echo "Datos sembrados OK (GET /api/v1/socios -> 2 socios, con token de sesión)"

cleanup
trap - EXIT

echo ""
echo "== App Android =="
echo "Compilando APK de desarrollo..."
cd "$ROOT/android"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
./gradlew --console=plain -q assembleDebug
APK="$ROOT/android/app/build/outputs/apk/debug/app-debug.apk"
echo "APK compilado: $APK"

if "$ADB" devices | grep -qE '^[^[:space:]]+[[:space:]]+device$'; then
  echo "Instalando y abriendo en el dispositivo conectado..."
  "$ADB" install -r "$APK"
  "$ADB" shell am start -n com.olivapp.app/.MainActivity
else
  echo "Sin dispositivo por USB: salto la instalación."
  echo "Para instalarlo: conecta el móvil (depuración USB activada) y ejecuta:"
  echo "  $ADB install -r $APK"
  echo "  $ADB shell am start -n com.olivapp.app/.MainActivity"
fi

echo ""
echo "Comandos reales del backend:"
echo "  - instalación: cd backend && python3 -m venv .venv && ./.venv/bin/pip install -r requirements.txt"
echo "  - verificación: cd backend && ./.venv/bin/python -m pytest -q"
echo "  - arranque: cd backend && DEVEXPERT_API_KEY=... JWT_SECRET=... ./.venv/bin/uvicorn app.main:app --reload"
echo "Comandos reales de la app Android:"
echo "  - compilación: cd android && ./gradlew assembleDebug (requiere ANDROID_HOME apuntando al SDK)"
echo "  - instalación: \$ANDROID_HOME/platform-tools/adb install -r android/app/build/outputs/apk/debug/app-debug.apk"
echo "  - apertura: \$ANDROID_HOME/platform-tools/adb shell am start -n com.olivapp.app/.MainActivity"
