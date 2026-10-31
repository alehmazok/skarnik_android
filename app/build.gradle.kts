import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import io.gitlab.arturbosch.detekt.getSupportedKotlinVersion
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.legacy.kapt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    // Must come after the kapt plugin: it hooks its annotation processor into kapt.
    alias(libs.plugins.objectbox)
    alias(libs.plugins.firebase.appdistribution)
}

// Release signing with the Play upload key, the same one skarnik-flutter signs with. key.properties
// (gitignored) has the same format as Flutter's: storeFile is relative to app/. CI writes it from secrets.
// Without it the release build is unsigned.
val keystoreProperties =
    Properties().apply {
        val file = rootProject.file("key.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }

android {
    // Same id as the Flutter app, so the native build installs as an update and keeps user data.
    namespace = "by.mazokaleh.skarnik"
    compileSdk = 37

    defaultConfig {
        applicationId = "by.mazokaleh.skarnik"
        minSdk = 24
        targetSdk = 37
        // Flutter 3.6.0 shipped versionCode 81; the native app must stay above it.
        // CI passes -Pskarnik.versionCode so every distributed build installs over the previous one.
        versionCode = providers.gradleProperty("skarnik.versionCode").orNull?.toInt() ?: 100
        versionName = "4.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Public endpoints, the same ones the Flutter and iOS apps ship (see gradle.properties).
        buildConfigField("String", "API_BASE_URL", "\"${property("skarnik.apiBaseUrl")}\"")
        buildConfigField("String", "SUPABASE_URL", "\"${property("skarnik.supabaseUrl")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${property("skarnik.supabaseKey")}\"")
        buildConfigField("String", "STARNIK_BASE_URL", "\"${property("skarnik.starnikBaseUrl")}\"")
        buildConfigField("String", "DEV_EMAIL", "\"${property("skarnik.devEmail")}\"")
        buildConfigField("String", "DEV_EMAIL_BCC", "\"${property("skarnik.devEmailBcc")}\"")
    }

    signingConfigs {
        if (!keystoreProperties.isEmpty) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Matches the Flutter debug build, so a native debug build upgrades a Flutter debug install.
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "Debug Skarnik")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            resValue("string", "app_name", "Skarnik")
            signingConfig = signingConfigs.findByName("release")
            // Firebase App Distribution (docs/adr/0005). The Firebase Android app is the one Flutter
            // registered for this application id; credentials and release notes come from CI.
            firebaseAppDistribution {
                appId = "1:690309283484:android:3c2521a8af9d651ad03759"
                artifactType = "APK"
                groups = "internal"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
        // AGP 9 turns resValue off by default; app_name is set per build type.
        resValues = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.all {
            // Show assertion messages in the CI log, not only in the uploaded report.
            it.testLogging {
                events("failed")
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }
        }
    }

    androidResources {
        // The search seed is already gzip-compressed; don't compress it a second time in the APK.
        noCompress += "gzip"
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
        checkDependencies = true
        // New AndroidX releases land weekly; version bumps are a deliberate chore, not a CI failure.
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
    }
}

ksp {
    arg("dagger.fastInit", "enabled")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    source.setFrom("src/main/java", "src/test/java")
}

// detekt 1.23 embeds its own Kotlin compiler; keep it on the version it was built with.
configurations.matching { it.name == "detekt" }.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(getSupportedKotlinVersion())
        }
    }
}

ktlint {
    android.set(true)
    filter {
        exclude { it.file.path.contains("/build/") }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.objectbox.kotlin)
    implementation(libs.okhttp)
    implementation(libs.jsoup)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.androidx.glance.appwidget.testing)
    testImplementation(libs.objectbox.linux)
    testImplementation(libs.objectbox.macos)
    testImplementation(libs.objectbox.windows)
}
