import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.ani.dailyspacenews"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ani.dailyspacenews"
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = "1.9"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val nasaKey1 = localProperties.getProperty("NASA_API_KEY_1", "")
        val nasaKey2 = localProperties.getProperty("NASA_API_KEY_2", "")
        val nasaKey3 = localProperties.getProperty("NASA_API_KEY_3", "")
        val lpAppKey = localProperties.getProperty("LEVELPLAY_APP_KEY", "")
        val lpNativeId = localProperties.getProperty("LP_NATIVE_ID", "")
        val lpInterstitialId = localProperties.getProperty("LP_INTERSTITIAL_ID", "")
        val lpRewardedId = localProperties.getProperty("LP_REWARDED_ID", "")
        val qonversionKey = localProperties.getProperty("QONVERSION_PROJECT_KEY", "")

        buildConfigField("String", "NASA_API_KEY_1", "\"$nasaKey1\"")
        buildConfigField("String", "NASA_API_KEY_2", "\"$nasaKey2\"")
        buildConfigField("String", "NASA_API_KEY_3", "\"$nasaKey3\"")
        
        buildConfigField("String", "LEVELPLAY_APP_KEY", "\"$lpAppKey\"")
        buildConfigField("String", "LP_NATIVE_ID", "\"$lpNativeId\"")
        buildConfigField("String", "LP_INTERSTITIAL_ID", "\"$lpInterstitialId\"")
        buildConfigField("String", "LP_REWARDED_ID", "\"$lpRewardedId\"")
        buildConfigField("String", "QONVERSION_PROJECT_KEY", "\"$qonversionKey\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.material3)
    implementation(libs.material)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.browser)

    implementation(libs.play.review)
    implementation(libs.play.review.ktx)

    implementation(libs.billing.ktx)
    implementation(libs.user.messaging.platform)
    implementation(libs.qonversion)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.coil.compose)

    // LevelPlay / ironSource SDK
    implementation(libs.ironsource.mediation)
    implementation(libs.unityads.adapter)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
