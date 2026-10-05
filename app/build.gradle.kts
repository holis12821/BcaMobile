plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    // Membaca app/google-services.json. Tanpa berkas itu build gagal —
    // berkasnya diunduh dari Firebase Console untuk package id.bca.bcamobile.
    alias(libs.plugins.google.services)
}

android {
    namespace = "id.bca.bcamobile"
    compileSdk = 37

    defaultConfig {
        applicationId = "id.bca.bcamobile"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    /**
     * Alamat server per lingkungan — sumbernya `bca-mobile-api/docs/10-BASE-URL-DAN-ENDPOINT.md`
     * di repo backend: §2 alamat per lingkungan, §4a konstanta base URL. Kontrak API
     * sengaja tidak disalin ke repo ini supaya tidak ada dua versi yang bisa berselisih.
     *
     * Alamat ditaruh di flavor, bukan di buildType, karena yang menentukan alamat
     * adalah **lingkungan server**, bukan apakah build-nya di-minify. Itu juga yang
     * membuat `local` dan `ngrok` bisa dibangun sebagai release, dan `production`
     * bisa dijalankan sebagai debug saat menelusuri masalah.
     *
     * Certificate pinning ikut flavor karena alasan yang sama: pinning terhadap
     * ngrok atau emulator pasti gagal handshake. Perlu diingat, AGP memenangkan
     * buildConfigField milik buildType di atas flavor — jadi nilai ini sengaja
     * tidak diset di buildType mana pun.
     *
     * **URL WebSocket signaling sengaja tidak ada di sini.** Dokumen §3e dan §4b
     * menegaskan klien tidak merakitnya sendiri; alamatnya datang utuh sebagai
     * `signaling_url` pada response `POST /v1/onboarding/video-call/queue`.
     */
    flavorDimensions += "environment"

    productFlavors {
        create("local") {
            dimension = "environment"
            isDefault = true
            // 10.0.2.2 = alias loopback host dari dalam emulator. `localhost` di
            // emulator adalah emulator itu sendiri — dokumen §4a.
            buildConfigField("String", "APP_BASE_URL", "\"http://10.0.2.2:8080/v1/\"")
            buildConfigField("String", "ONBOARDING_BASE_URL", "\"http://10.0.2.2:8080/v1/onboarding/\"")
            buildConfigField("String", "INTERNAL_BASE_URL", "\"http://10.0.2.2:8080/internal/v1/\"")
            buildConfigField("boolean", "CERTIFICATE_PINNING_ENABLED", "false")
        }

        create("ngrok") {
            dimension = "environment"
            // Domain statis; hidup hanya selama `make tunnel` berjalan di laptop
            // developer. Bukan alamat rilis — dokumen §2b.
            buildConfigField("String", "APP_BASE_URL", "\"https://fibromatous-jerald-postsurgical.ngrok-free.dev/v1/\"")
            buildConfigField("String", "ONBOARDING_BASE_URL", "\"https://fibromatous-jerald-postsurgical.ngrok-free.dev/v1/onboarding/\"")
            buildConfigField("String", "INTERNAL_BASE_URL", "\"https://fibromatous-jerald-postsurgical.ngrok-free.dev/internal/v1/\"")
            buildConfigField("boolean", "CERTIFICATE_PINNING_ENABLED", "false")
        }

        create("staging") {
            dimension = "environment"
            // TODO(infra): host belum dikonfirmasi. Dokumen §2c: belum ada deployment.
            //  Nilai ini terbawa dari konfigurasi lama, bukan alamat yang sudah terbukti.
            buildConfigField("String", "APP_BASE_URL", "\"https://api-staging.bcamobile.id/v1/\"")
            buildConfigField("String", "ONBOARDING_BASE_URL", "\"https://api-staging.bcamobile.id/v1/onboarding/\"")
            buildConfigField("String", "INTERNAL_BASE_URL", "\"https://api-staging.bcamobile.id/internal/v1/\"")
            buildConfigField("boolean", "CERTIFICATE_PINNING_ENABLED", "false")
        }

        create("production") {
            dimension = "environment"
            // TODO(infra): host belum dikonfirmasi. Dokumen §2c: belum ada deployment.
            buildConfigField("String", "APP_BASE_URL", "\"https://api.bcamobile.id/v1/\"")
            buildConfigField("String", "ONBOARDING_BASE_URL", "\"https://api.bcamobile.id/v1/onboarding/\"")
            buildConfigField("String", "INTERNAL_BASE_URL", "\"https://api.bcamobile.id/internal/v1/\"")
            // Daftar pin masih kosong di NetworkModule.certificatePinner() — TODO infra
            // tersendiri yang sudah dicatat di CLAUDE.md.
            buildConfigField("boolean", "CERTIFICATE_PINNING_ENABLED", "true")
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    // Signaling-nya memakai WebSocket OkHttp di atas; ini hanya media (PeerConnection,
    // track, SurfaceViewRenderer).
    implementation(libs.stream.webrtc.android)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.face.detection)
    implementation(libs.mlkit.barcode.scanning)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}