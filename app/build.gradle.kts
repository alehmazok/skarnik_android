import com.android.build.api.artifact.SingleArtifact
import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import io.gitlab.arturbosch.detekt.getSupportedKotlinVersion
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory

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
    alias(libs.plugins.baselineprofile)
}

// Release signing with the Play upload key, the same one skarnik-flutter signs with. key.properties
// (gitignored) has the same format as Flutter's: storeFile is relative to app/. CI writes it from secrets.
// Without it the release build is unsigned.
val keystoreProperties =
    Properties().apply {
        val file = rootProject.file("key.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }

// The Firebase Android app Flutter registered for this application id (gradle.properties).
val firebaseAppId = property("skarnik.firebaseAppId").toString()

// Sentry (docs/adr/0008): -Pskarnik.sentryDsn or the SENTRY_DSN environment variable; empty means off.
val sentryDsn =
    providers.gradleProperty("skarnik.sentryDsn").orElse(providers.environmentVariable("SENTRY_DSN")).getOrElse("")
val sentryEnvironment = providers.gradleProperty("skarnik.sentryEnvironment").getOrElse("production")
val sentryProguardUuid = providers.gradleProperty("skarnik.sentryProguardUuid").getOrElse("")

// The app shows no ads and promises anonymous statistics, so it must not declare access to any advertising id.
// The manifest removes what Firebase Analytics merges in; this list is what CheckManifestPermissions guards.
val forbiddenPermissions =
    listOf(
        "com.google.android.gms.permission.AD_ID",
        "android.permission.ACCESS_ADSERVICES_AD_ID",
        "android.permission.ACCESS_ADSERVICES_ATTRIBUTION"
    )

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
        versionName = "4.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Public endpoints, the same ones the Flutter and iOS apps ship (see gradle.properties).
        buildConfigField("String", "API_BASE_URL", "\"${property("skarnik.apiBaseUrl")}\"")
        buildConfigField("String", "SUPABASE_URL", "\"${property("skarnik.supabaseUrl")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${property("skarnik.supabaseKey")}\"")
        buildConfigField("String", "STARNIK_BASE_URL", "\"${property("skarnik.starnikBaseUrl")}\"")
        buildConfigField("String", "DEV_EMAIL", "\"${property("skarnik.devEmail")}\"")
        buildConfigField("String", "DEV_EMAIL_BCC", "\"${property("skarnik.devEmailBcc")}\"")

        // Firebase without the google-services plugin (docs/adr/0007): FirebaseInitProvider reads these resources.
        // They are the public client config of the Firebase Android app Flutter registered for this id.
        resValue("string", "google_app_id", firebaseAppId)
        resValue("string", "google_api_key", property("skarnik.firebaseApiKey").toString())
        resValue("string", "gcm_defaultSenderId", property("skarnik.firebaseSenderId").toString())
        resValue("string", "project_id", property("skarnik.firebaseProjectId").toString())
        resValue("string", "google_storage_bucket", property("skarnik.firebaseStorageBucket").toString())
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
            // Debug builds never send analytics: events only go to Logcat (tag "Analytics").
            manifestPlaceholders["analyticsDeactivated"] = "true"
            // Debug builds never report crashes: an empty DSN keeps Sentry off.
            buildConfigField("String", "SENTRY_DSN", "\"\"")
            buildConfigField("String", "SENTRY_ENVIRONMENT", "\"debug\"")
            buildConfigField("String", "SENTRY_PROGUARD_UUID", "\"\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            resValue("string", "app_name", "Skarnik")
            manifestPlaceholders["analyticsDeactivated"] = "false"
            // Crash reporting (docs/adr/0008). The DSN comes from the SENTRY_DSN secret in CI; without it (a local
            // build) Sentry stays off. The ProGuard UUID ties a crash to the R8 mapping CI uploads for this build.
            buildConfigField("String", "SENTRY_DSN", "\"$sentryDsn\"")
            buildConfigField("String", "SENTRY_ENVIRONMENT", "\"$sentryEnvironment\"")
            buildConfigField("String", "SENTRY_PROGUARD_UUID", "\"$sentryProguardUuid\"")
            signingConfig = signingConfigs.findByName("release")
            // Firebase App Distribution (docs/adr/0005). The Firebase Android app is the one Flutter
            // registered for this application id; credentials and release notes come from CI.
            firebaseAppDistribution {
                appId = firebaseAppId
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

// Baseline and startup profiles (docs/adr/0010). The baselineprofile module generates them on CI's emulator
// (.github/workflows/baseline-profile.yml) into src/release/generated/baselineProfiles/, which is committed; a
// release build only packs that file and never runs the generator.
baselineProfile {
    automaticGenerationDuringBuild = false
    // R8 puts the classes of the startup profile into the first dex file, so cold start reads less of the APK.
    dexLayoutOptimization = true
}

androidComponents {
    // The plugin's benchmarkRelease and nonMinifiedRelease build types copy release, which is unsigned without the
    // upload key (pull requests, local builds). They only ever run on test devices, so they use the debug key.
    onVariants(selector().withBuildType("benchmarkRelease")) { variant ->
        variant.signingConfig.setConfig(android.signingConfigs.getByName("debug"))
    }
    onVariants(selector().withBuildType("nonMinifiedRelease")) { variant ->
        variant.signingConfig.setConfig(android.signingConfigs.getByName("debug"))
    }
    // check<Variant>ManifestPermissions (docs/adr/0007): fails when a library merges an advertising-id permission
    // back in, e.g. after a Firebase BoM bump. CI runs it on the release manifest.
    onVariants { variant ->
        val name = variant.name.replaceFirstChar { it.uppercase() }
        tasks.register<CheckManifestPermissions>("check${name}ManifestPermissions") {
            manifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
            forbidden.set(forbiddenPermissions)
            required.set(listOf("android.permission.INTERNET"))
            report.set(layout.buildDirectory.file("reports/manifest-permissions/${variant.name}.txt"))
        }
    }
}

abstract class CheckManifestPermissions : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val manifest: RegularFileProperty

    @get:Input
    abstract val forbidden: ListProperty<String>

    // A permission that must be there, so a manifest this task cannot read never passes.
    @get:Input
    abstract val required: ListProperty<String>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val document = factory.newDocumentBuilder().parse(manifest.get().asFile)
        val declared =
            listOf("uses-permission", "uses-permission-sdk-23").flatMap { tag ->
                val nodes = document.getElementsByTagName(tag)
                (0 until nodes.length).map { index ->
                    (nodes.item(index) as org.w3c.dom.Element).getAttributeNS(ANDROID_NS, "name")
                }
            }
        val missing = required.get() - declared.toSet()
        check(missing.isEmpty()) { "Merged manifest ${manifest.get().asFile} has no $missing; is it the right file?" }
        val found = forbidden.get().filter { it in declared }
        check(found.isEmpty()) {
            "Merged manifest declares $found. Remove them in app/src/main/AndroidManifest.xml with " +
                "tools:node=\"remove\" (docs/adr/0007)."
        }
        report.get().asFile.writeText(declared.sorted().joinToString("\n", postfix = "\n"))
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
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

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.config)
    implementation(libs.play.review)
    implementation(libs.sentry.android)
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))

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
