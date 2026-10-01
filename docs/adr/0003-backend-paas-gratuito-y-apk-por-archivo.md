# ADR 0003: Backend desplegado en PaaS gratuito y APK distribuido por archivo

## Status
Accepted (2026-09-28). Desbloquea el punto "despliegue del backend e instalación del APK" de `docs/risks-and-open-questions.md`. La forma concreta de levantar el backend puede revisarse en el futuro sin cambiar lo aquí decidido.

## Context
El ADR 0002 fija un backend ligero obligatorio (proxy de la API de IA + API de datos) y una demo nativa en un único dispositivo Android del socio del proyecto. Requisitos duros: HTTPS, claves (`DEVEXPERT_API_KEY`, secreto JWT) solo en variables de entorno del servidor. Restricciones del proyecto: un solo desarrollador, alcance académico, coste objetivo 0 €, demo ante tribunal en un aula con WiFi y el riesgo nº1 del proyecto es la fricción del salto a nativa (build, firma, Gradle, dispositivo). El móvil de la demo es propio y permite activar la depuración USB.

## Decision
1. **Backend:** desplegarlo en un **PaaS con capa gratuita y HTTPS automático** (primera opción: Render), con secretos en variables de entorno del servicio. El backend es un único proceso (API de datos + proxy del gateway de inferencia) con base de datos ligera según el plan elegido. **Plan B, también 0 €:** el mismo backend corriendo en local en el portátil expuesto con un túnel público (`cloudflared`/`ngrok`); la app solo necesita una URL base configurable. Desde el día 1 el backend debe poder levantarse en local con un solo comando.
2. **Instalación del APK:** el día de la demo se instala un **APK release firmado distribuido como archivo** (Drive o cable), como lo haría un usuario. **Plan B y vía diaria de desarrollo:** `adb install` por USB.

## Alternatives Considered
- **Cloud Run (GCP) como plan A:** elimina el sleep del free tier (escala a cero sin dormir), pero exige cuenta con tarjeta de verificación y más setup (Dockerfile, gcloud). Queda como alternativa si el cold start del PaaS gratuito molesta en la práctica.
- **VPS barato (Oracle Always Free / Hetzner) + Caddy:** control total y sin cold start, pero carga de sysadmin ajena al objetivo del curso. Descartada.
- **Serverless (Vercel/Netlify + BBDD externa):** reparte el backend en funciones con límites de payload y cold start justo donde viaja el audio de la feature estrella. Descartada.
- **Depender solo del túnel a portátil como plan A:** 0 € y cero despliegue, pero la demo quedaría atada al portátil encendido y a la red del aula. Se conserva únicamente como plan B.
- **Firebase App Distribution / Google Play (internal track) para el APK:** realismo de tienda, a cambio de cuentas, builds firmados y esperas de revisión innecesarias para una demo. Descartada.

## Consequences
- Coste 0 € asumiendo el **cold start** del free tier: mitigarlo "despertando" el backend con una petición antes de empezar la demo.
- La demo depende solo de móvil + WiFi del aula como plan A; el plan B (portátil + túnel) cubre caídas del PaaS sin cambiar la app.
- La URL del backend es configurable en la app: cambiar de plan A a plan B es solo cambiar una variable.
- Las claves de IA y del JWT nunca salen del servidor; el APK solo contiene la URL base.
- Si en el futuro cambia cómo se levanta el backend (otro proveedor, otro proceso, otra BBDD), no hace falta un ADR nuevo salvo que cambie esta estructura de decisión.
- El riesgo "salto a app nativa" se mitiga fijando ya el único dispositivo objetivo y teniendo doble vía de instalación del APK.
