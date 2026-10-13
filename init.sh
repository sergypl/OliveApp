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
# comportamiento declarado en esa feature) y desde `aislamiento-de-datos-por-socio`
# cada cuenta solo ve los datos de su socio (cambio declarado en esa feature):
# la cuenta de verificación se vincula por su código de socio y se comprueba
# que le llegan sus datos sembrados.
curl -sf -X POST http://127.0.0.1:8000/api/v1/registro \
  -H 'Content-Type: application/json' \
  -d '{"email":"verificacion-init@ejemplo.com","contrasena":"segura123","codigo_socio":"Socio 1"}' >/dev/null 2>&1 || true
TOKEN=$(curl -sf -X POST http://127.0.0.1:8000/api/v1/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"verificacion-init@ejemplo.com","contrasena":"segura123"}' \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['token'])" 2>/dev/null)
PANEL=$(curl -sf -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8000/api/v1/mi-panel 2>/dev/null)
KILOS=$(echo "$PANEL" | python3 -c "import json,sys; print(json.load(sys.stdin)['datos_oficiales']['kilos_entregados'])" 2>/dev/null)
CAMPANAS=$(curl -sf -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8000/api/v1/mi-campanas 2>/dev/null \
  | python3 -c "import json,sys; print(len(json.load(sys.stdin)))" 2>/dev/null)
if [ -z "$TOKEN" ] || [ -z "$KILOS" ] || [ "$CAMPANAS" != "4" ]; then
  echo "ERROR: los datos sembrados del socio no responden (mi-panel y mi-campanas con token)"
  exit 1
fi
echo "Datos sembrados OK (mi-panel con datos oficiales y 4 campañas para el socio de la cuenta, con token de sesión)"

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

# La app llama al backend por 127.0.0.1:8000 en cada dispositivo: `adb reverse`
# redirige ese puerto del dispositivo (emulador o móvil físico) al del Mac.
DISPOSITIVOS=$("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}')
if [ -n "$DISPOSITIVOS" ]; then
  echo "Instalando y abriendo en cada dispositivo conectado..."
  for SERIAL in $DISPOSITIVOS; do
    "$ADB" -s "$SERIAL" reverse tcp:8000 tcp:8000 >/dev/null 2>&1 || true
    "$ADB" -s "$SERIAL" install -r "$APK"
    "$ADB" -s "$SERIAL" shell am start -n com.olivapp.app/.MainActivity
  done
else
  echo "Sin dispositivo por USB: salto la instalación."
  echo "Para instalarlo: conecta el móvil (depuración USB activada) y ejecuta:"
  echo "  $ADB reverse tcp:8000 tcp:8000"
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
echo "  - túnel al backend (por dispositivo): \$ANDROID_HOME/platform-tools/adb reverse tcp:8000 tcp:8000"
echo "  - instalación: \$ANDROID_HOME/platform-tools/adb install -r android/app/build/outputs/apk/debug/app-debug.apk"
echo "  - apertura: \$ANDROID_HOME/platform-tools/adb shell am start -n com.olivapp.app/.MainActivity"
