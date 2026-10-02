import org.gradle.kotlin.dsl.support.serviceOf

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.aks"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.aks"
        minSdk = 26
        targetSdk = 35
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
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    //FireBase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)


    implementation (libs.play.services.maps)
    implementation (libs.dexter)
    implementation (libs.play.services.location)

    implementation(libs.picasso)

    implementation ("com.google.android.material:material:1.9.0")

    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))

    // Add the dependency for the Firebase Authentication library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-auth")

    // Also add the dependency for the Google Play services library and specify its version
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    implementation ("com.google.firebase:firebase-storage:20.0.0")

    implementation("com.android.volley:volley:1.2.1")

    implementation ("com.google.firebase:firebase-messaging:23.1.1")

    implementation("com.google.android.libraries.places:places:2.7.0")

    implementation("com.google.android.material:material:1.11.0")// or latest version

    implementation ("de.hdodenhof:circleimageview:3.1.0")

    implementation ("com.google.android.gms:play-services-maps:18.1.0")


}