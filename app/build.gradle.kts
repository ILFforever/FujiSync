import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.ilfforever.fujisync"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ilfforever.fujisync"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "1.2.4"
        buildConfigField("String", "GITHUB_REPO", "\"ILFforever/FujiSync\"")
    }

    flavorDimensions += listOf("discover", "store")
    productFlavors {
        create("full") {
            dimension = "discover"
            buildConfigField("Boolean", "DISCOVER_ENABLED", "true")
        }
        create("lean") {
            dimension = "discover"
            buildConfigField("Boolean", "DISCOVER_ENABLED", "false")
        }

        // Sideloaded from GitHub Releases: self-updates and exposes developer tooling.
        create("github") {
            dimension = "store"
            buildConfigField("Boolean", "UPDATER_ENABLED", "true")
            buildConfigField("Boolean", "DEV_TOOLS_ENABLED", "true")
            buildConfigField("Boolean", "SUPPORT_LINK_ENABLED", "true")
        }
        // Google Play: Play owns updates, and dev tooling is hidden. The in-app APK
        // downloader plus REQUEST_INSTALL_PACKAGES violates Play's Device and Network
        // Abuse policy, so it is compiled out and the permission lives in src/github.
        // SUPPORT_LINK_ENABLED is off because the support screen opens Buy Me a Coffee.
        // Play expects payment flows to go through Play Billing, so the external donation
        // link is dropped rather than risk a policy strike.
        create("play") {
            dimension = "store"
            buildConfigField("Boolean", "UPDATER_ENABLED", "false")
            buildConfigField("Boolean", "DEV_TOOLS_ENABLED", "false")
            buildConfigField("Boolean", "SUPPORT_LINK_ENABLED", "false")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            // Populate via local.properties or CI env vars:
            // storeFile, storePassword, keyAlias, keyPassword
            val props = Properties().also { p ->
                val f = rootProject.file("local.properties")
                if (f.exists()) p.load(f.inputStream())
            }
            storeFile = props.getProperty("releaseStoreFile")?.let { file(it) }
            storePassword = props.getProperty("releaseStorePassword") ?: System.getenv("RELEASE_STORE_PASSWORD")
            keyAlias = props.getProperty("releaseKeyAlias") ?: System.getenv("RELEASE_KEY_ALIAS")
            keyPassword = props.getProperty("releaseKeyPassword") ?: System.getenv("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    val cameraXVersion = "1.4.1"
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.camera:camera-camera2:$cameraXVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraXVersion")
    implementation("androidx.camera:camera-view:$cameraXVersion")
    implementation("com.google.guava:guava:33.3.1-android")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.browser:browser:1.8.0")

    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("androidx.emoji2:emoji2-emojipicker:1.6.0")

    implementation("com.drewnoakes:metadata-extractor:2.19.0")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    implementation("com.google.zxing:core:3.5.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("app.cash.turbine:turbine:1.2.0")
}
