#!/usr/bin/env bash
set -euo pipefail

echo "Repositorio: $(pwd)"
echo "Estado del harness: pre-bootstrap"
echo "El stack de la aplicación todavía no está inicializado."
echo "Siguiente paso: elegir la primera feature lista en feature_list.json y hacer el bootstrap técnico."
echo "Comandos futuros esperados (tras el bootstrap):"
echo "  - instalación: p. ej. './gradlew assembleDebug' (app Android) + instalación de dependencias del backend"
echo "  - verificación: p. ej. tests/lint de app y backend"
echo "  - arranque: p. ej. servidor backend local + instalación del APK en el dispositivo"
echo "Registrar aquí los comandos reales cuando existan."
