plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.olivapp.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.olivapp.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-bootstrap"
        // La app solo existe en español: al compilar se descartan las traducciones
        // del resto de idiomas que aportan las librerías (resources.arsc).
        resourceConfigurations += listOf("es")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        // Robolectric necesita los recursos de Android para simular el entorno
        // en los tests de caracterización (JVM, sin dispositivo).
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.core:core-ktx:1.13.1")
    // Splash animado (fundamentos-visuales-modernos, ADR 0010).
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Almacenamiento seguro del token de sesión (Keystore del dispositivo).
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    // Preferencias del simulador avanzado (simulador-avanzado-precio-aceite).
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // Cola local de notas de voz (feature cola-offline-de-notas): Room sobre SQLite.
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    // Inyección de dependencias (ADR 0008): Hilt para ViewModels y repositorios.
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    // Un ViewModel por pantalla con StateFlow<UiState> (refactor-arquitectura-android).
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    // Tests de caracterización (red de seguridad de refactor-arquitectura-android).
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
}
