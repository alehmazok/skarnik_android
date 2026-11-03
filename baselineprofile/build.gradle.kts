import io.gitlab.arturbosch.detekt.getSupportedKotlinVersion

// Generates the app's Baseline Profile and benchmarks cold start (docs/adr/0010). Runs on a connected device or
// emulator: CI's baseline-profile workflow, or `./gradlew :app:generateReleaseBaselineProfile` locally.
plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

android {
    namespace = "by.mazokaleh.skarnik.baselineprofile"
    compileSdk = 37

    defaultConfig {
        // Profile generation without root needs Android 13; Android 9 is the lowest it runs on at all.
        minSdk = 28
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // CI measures on an emulator: its numbers compare None against the profile, not real devices.
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
    }

    targetProjectPath = ":app"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

baselineProfile {
    // The emulator CI starts (or a phone plugged in); no Gradle managed device to download.
    useConnectedDevices = true
}

androidComponents {
    // The tests read the tested app's id (by.mazokaleh.skarnik for the release-based variants) from this argument.
    onVariants { variant ->
        val artifactsLoader = variant.artifacts.getBuiltArtifactsLoader()
        variant.instrumentationRunnerArguments.put(
            "targetAppId",
            variant.testedApks.map { artifactsLoader.load(it)?.applicationId.orEmpty() }
        )
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    source.setFrom("src/main/java")
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
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
}
