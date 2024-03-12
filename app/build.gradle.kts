plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

android {
    namespace = "com.example.livestreamdemo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.livestreamdemo"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    dataBinding {
        enable = true
    }
    packagingOptions {
        resources.excludes.add("META-INF/DEPENDENCIES")
        resources.excludes.add("META-INF/LICENSE")
        resources.excludes.add("META-INF/LICENSE.txt")
        resources.excludes.add("META-INF/license.txt")
        resources.excludes.add("META-INF/NOTICE")
        resources.excludes.add("META-INF/NOTICE.txt")
        resources.excludes.add("META-INF/notice.txt")
        resources.excludes.add("META-INF/ASL2.0")
        resources.excludes.add("META-INF/*.kotlin_module")
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    val intuit_version = "1.1.0"
    implementation("com.intuit.ssp:ssp-android:$intuit_version") //forSDP
    implementation("com.intuit.sdp:sdp-android:$intuit_version") //ForSSP

    val exoplayer_version = "2.19.1"
    implementation("com.google.android.exoplayer:exoplayer-core:$exoplayer_version")
    implementation("com.google.android.exoplayer:exoplayer-hls:$exoplayer_version")
    implementation("com.google.android.exoplayer:exoplayer-dash:$exoplayer_version")
    implementation("com.google.android.exoplayer:exoplayer-ui:$exoplayer_version")

    //Youtube
    //implementation("com.google.api-client:google-api-client:2.2.0")
    implementation("com.google.apis:google-api-services-youtube:v3-rev20240225-2.0.0")
    implementation("com.google.http-client:google-http-client-jackson:1.15.0-rc")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.22.0")
    implementation("com.google.api-client:google-api-client:1.32.1")
    implementation("com.google.android.gms:play-services-location:21.2.0")




    //implementation("com.github.evgenyneu:js-evaluator-for-android:v4.0.0")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.14.2")

    implementation("io.coil-kt:coil:2.6.0")
}