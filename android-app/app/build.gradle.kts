plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val corinthiansDataUrl = providers.gradleProperty("CORINTHIANS_DATA_URL")
    .orElse("https://fabriciobentes.github.io/Corinthians-Torcedor-app_")
    .get()

android {
    namespace = "com.fabricio.corinthianslive"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fabricio.corinthianslive"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "DATA_BASE_URL", "\"${corinthiansDataUrl.trimEnd('/')}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // APK pessoal instalável. Para publicar na Play Store, substitua pela sua chave de produção.
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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

    sourceSets["main"].assets.srcDir("../../data-pipeline/public")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation("androidx.navigation:navigation-compose:2.8.0")

    // <<< ISSO AQUI resolve EmojiEvents / LiveTv / SportsSoccer
    implementation("androidx.compose.material:material-icons-extended")
}
