import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.navigation.safe.args)
    alias(libs.plugins.google.services)
}

// Load local.properties file manually
val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream ->
        localProperties.load(stream)
    }
}

android {
    namespace = "com.example.triptip_yaron_and_alon"
    compileSdk = 36


    defaultConfig {
        applicationId = "com.example.triptip_yaron_and_alon"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // API Keys - Read from local.properties (NEVER commit API keys to git!)
        // Note: Open-Meteo doesn't require an API key (free and open source)
        // Add keys to local.properties file (which is in .gitignore)
        val opentripmapApiKey = localProperties.getProperty("OPENTRIPMAP_API_KEY", "")
        val googlePlacesApiKey = localProperties.getProperty("GOOGLE_PLACES_API_KEY", "")
        buildConfigField("String", "OPENTRIPMAP_API_KEY", "\"$opentripmapApiKey\"")
        buildConfigField("String", "GOOGLE_PLACES_API_KEY", "\"$googlePlacesApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    
    kotlinOptions {
        jvmTarget = "11"
    }
    
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    
    // Fragment & Navigation
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    
    // ViewModel & LiveData
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    
    // RecyclerView & SwipeRefresh
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    
    // ViewPager2 for tabs
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    
    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    
    // Image Loading
    implementation(libs.coil)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    
    // Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.3.0")
    
    // Firebase - Use BOM for version management
    implementation(platform(libs.firebase.bom))
    // Firebase libraries - versions managed by BOM
    // Note: Version constraints help Gradle resolve dependencies correctly with BOM
    implementation("com.google.firebase:firebase-auth-ktx") {
        version {
            strictly("[0,999]") // Allow any version from BOM
        }
    }
    implementation("com.google.firebase:firebase-firestore-ktx") {
        version {
            strictly("[0,999]") // Allow any version from BOM
        }
    }
    implementation("com.google.firebase:firebase-storage-ktx") {
        version {
            strictly("[0,999]") // Allow any version from BOM
        }
    }
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}