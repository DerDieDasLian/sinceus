import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Signier-Schlüssel für Releases, liegt NICHT im Repo (keystore.properties, siehe README)
val releaseKeystore = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
    Properties().apply { file.inputStream().use { load(it) } }
}

android {
    namespace = "app.sinceus"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.sinceus"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "2.2.0"
        // Repository, in dem die GitHub-Version nach neuen Releases sucht
        val githubRepo = providers.gradleProperty("githubRepo").getOrElse("DerDieDasLian/sinceus")
        buildConfigField("String", "GITHUB_REPO", "\"$githubRepo\"")
    }

    // play:   Google Play, Updates über den Play Store
    // github: Releases auf GitHub, sucht selbst nach Updates (einzige Variante mit Internet)
    // fdroid: F-Droid, Updates über F-Droid
    flavorDimensions += "distribution"
    productFlavors {
        create("play") {
            dimension = "distribution"
            buildConfigField("boolean", "UPDATE_CHECK", "false")
        }
        create("github") {
            dimension = "distribution"
            buildConfigField("boolean", "UPDATE_CHECK", "true")
        }
        create("fdroid") {
            dimension = "distribution"
            buildConfigField("boolean", "UPDATE_CHECK", "false")
        }
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = rootProject.file(releaseKeystore.getProperty("storeFile"))
                storePassword = releaseKeystore.getProperty("storePassword")
                keyAlias = releaseKeystore.getProperty("keyAlias")
                keyPassword = releaseKeystore.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // Ohne keystore.properties bleibt die APK unsigniert (so erwartet es F-Droid, das selbst signiert)
            signingConfig = signingConfigs.findByName("release")
        }
    }

    // Keine verschlüsselten Abhängigkeits-Infos in der APK (F-Droid kann sie nicht prüfen); im Play-Bundle bleiben sie
    dependenciesInfo {
        includeInApk = false
        includeInBundle = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Sprachauswahl pro App in den Android-Einstellungen (Englisch, Deutsch)
    androidResources {
        generateLocaleConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        // Optional: Robolectric-Laufzeit aus lokalem Ordner statt Download
        System.getenv("ROBOLECTRIC_DEPS_DIR")?.let { dir ->
            unitTests.all {
                it.systemProperty("robolectric.offline", "true")
                it.systemProperty("robolectric.dependency.dir", dir)
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
