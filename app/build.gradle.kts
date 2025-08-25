import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.bryanguerra.dynamicislandmusic"
    compileSdk = 36

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.2.0"
    }

    defaultConfig {
        applicationId = "com.bryanguerra.dynamicislandmusic"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {

        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
            freeCompilerArgs.add("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }
}

dependencies {

    // --- Compose BOM (gestiona versiones de to - do Compose) ---
    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))

    // Compose core
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.material3)
    //implementation(libs.androidx.lifecycle.runtime.compose)

    // Navegación (opcional)
    implementation(libs.androidx.navigation.compose)

    // Hilt (DI)
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // DataStore (settings)
    implementation(libs.androidx.datastore.preferences)

    // Coil + GIF
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    // Palette para color dominante de la carátula
    implementation(libs.androidx.palette.ktx)

    // Media (para compat y utilidades)
    implementation(libs.androidx.media3.session)

    implementation(libs.androidx.material.icons.extended)

    implementation(libs.lottie.compose)

    // Necesarios para ComposeView en WindowManager (ViewTree*):
    implementation(libs.androidx.lifecycle.runtime.android)      // ViewTreeLifecycleOwner
    implementation(libs.androidx.lifecycle.viewmodel.android)    // ViewTreeViewModelStoreOwner
    implementation(libs.androidx.savedstate)                     // ViewTreeSavedStateRegistryOwner

    implementation(libs.androidx.ui.graphics)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}