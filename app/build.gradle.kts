plugins {
    alias(libs.plugins.android.application)
    id("com.diffplug.spotless")
}

android {
    namespace = "com.example.split_basket"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.split_basket"
        minSdk = 22
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
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_22
        targetCompatibility = JavaVersion.VERSION_22
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(22)
        }
    }
    lint {
        disable += "PropertyEscape"
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    // 添加Gson依赖
    implementation("com.google.code.gson:gson:2.13.2")
    // 添加Room依赖
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

spotless {
    java {
        googleJavaFormat()
    }
}
