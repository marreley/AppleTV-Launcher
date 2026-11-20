plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Clé API TMDB (optionnelle) pour afficher de vraies affiches de films et séries.
// Fournie par -PTMDB_API_KEY=... ou par la variable d'environnement TMDB_API_KEY (secret GitHub).
val tmdbKey: String = (project.findProperty("TMDB_API_KEY") as String?)
    ?: System.getenv("TMDB_API_KEY")
    ?: ""

base {
    archivesName.set("AppleTVLauncher")
}

android {
    namespace = "com.appletv.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.appletv.launcher"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "0.1.2-" + (System.getenv("GITHUB_SHA") ?: "local").take(7)

        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbKey\"")
    }

    // Clé de débogage fixe (non secrète) : chaque build est signé pareil,
    // donc une nouvelle version s'installe par-dessus l'ancienne sans désinstallation.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
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
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.leanback:leanback:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
