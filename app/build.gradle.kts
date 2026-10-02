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
        versionCode = 36
        versionName = "2.12.0"
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

/**
 * Kopiert die Änderungslisten aus fastlane/metadata in die App (assets/changelogs/de|en/<versionCode>.txt),
 * damit sie nach einem Update als „Neu in dieser Version“ erscheinen. Eine Quelle für Stores und App.
 */
abstract class CopyChangelogs : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val metadata: DirectoryProperty

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @TaskAction
    fun copy() {
        val out = output.get().asFile
        out.deleteRecursively()
        mapOf("de-DE" to "de", "en-US" to "en").forEach { (folder, lang) ->
            val target = out.resolve("changelogs/$lang").apply { mkdirs() }
            metadata.get().asFile.resolve("$folder/changelogs").listFiles { f -> f.extension == "txt" }
                ?.forEach { it.copyTo(target.resolve(it.name), overwrite = true) }
        }
    }
}

androidComponents {
    onVariants { variant ->
        val copy = tasks.register<CopyChangelogs>("copy${variant.name.replaceFirstChar { it.uppercase() }}Changelogs") {
            metadata.set(rootProject.layout.projectDirectory.dir("fastlane/metadata/android"))
        }
        variant.sources.assets?.addGeneratedSourceDirectory(copy, CopyChangelogs::output)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    // Die Icon-Bibliothek wird nicht mehr weiterentwickelt, 1.7.8 ist die letzte Version
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")
    implementation("io.coil-kt.coil3:coil-compose:3.4.0")
    // QR-Code für die Kopplung zum Abgleich (erzeugen und scannen, Apache 2.0)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
