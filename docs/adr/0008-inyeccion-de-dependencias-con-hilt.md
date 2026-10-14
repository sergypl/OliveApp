# ADR 0008: Inyección de dependencias con Hilt en la app Android

## Status
Accepted (2026-10-14). Aprobada por el usuario durante `refactor-arquitectura-android` (aviso previo a la dependencia dado en la sesión 035). Justifica la introducción de Hilt, que no existía en el proyecto.

## Context
La app Android nació como un único `MainActivity.kt` con toda la lógica (pantallas, estado en `remember`, llamadas al backend, Room y grabación de audio). El refactor `refactor-arquitectura-android` introduce arquitectura limpia pragmática por paquetes de feature (presentation / domain / data) con **un ViewModel por pantalla**, lo que exige construir repositorios, almacenes y servicios fuera de los composables y entregárselos a los ViewModels. Sin un mecanismo de inyección, cada ViewModel tendría que construir su propio grafo a mano.

## Decision
1. **Hilt** (`com.google.dagger:hilt.android:2.52`, con KSP) como mecanismo de inyección de dependencias.
2. Clase de aplicación `AplicacionOlivApp` con `@HiltAndroidApp` (registrada en el manifest con `android:name`; no cambia permisos) y `MainActivity` con `@AndroidEntryPoint`.
3. Los ViewModels se obtienen en Compose con `hiltViewModel()` (por eso sí se añade `androidx.hilt:hilt-navigation-compose:1.2.0`: ahí vive esa función, aunque la app no use Navigation library).
4. Dependencias de soporte añadidas en esta fase: `androidx.lifecycle:lifecycle-viewmodel-compose` y `lifecycle-runtime-compose` (para `StateFlow<UiState>` con `collectAsStateWithLifecycle`).

## Alternatives Considered
- **Inyección manual por constructor con un grafo simple en la Application:** sin dependencias nuevas y suficiente para los ~10 ViewModels del MVP, pero menos estándar, propenso a errores al crecer y con más código propio que mantener. Descartada por decisión del usuario a favor de Hilt.
- **Koin (service locator):** más ligero, pero añade semántica de resolución en tiempo de ejecución sin ganancia real para este tamaño. Descartado.

## Consequences
- Tres dependencias nuevas de compilación (`hilt-android`, `hilt-compiler`, `hilt-navigation-compose`) más el plugin de Gradle de Hilt y las dos de lifecycle para Compose; el APK de desarrollo crece un poco (Hilt genera código en compilación, sin coste en tiempo de ejecución apreciable).
- Los ViewModels se testean fuera de Hilt (con fakes por constructor), así que los tests de ViewModel no dependen del grafo.
- Cualquier clase que pida inyección en Android (Activity) debe estar anotada; los repositorios se declaran en módulos Hilt por capa (`data`) a medida que se crean en las fases siguientes del refactor.
