plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
}

android {
    namespace = "com.flymex"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = false
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

    lint {
        // Resource names are kept identical to Flyme's own (Blue_6, mcLoadingRadius …) on purpose.
        disable += setOf("ResourceName", "UnusedResources", "MissingTranslation")
    }
}

dependencies {
    api(libs.androidx.appcompat)
    api(libs.androidx.core)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.dynamicanimation)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.flymex"
            artifactId = "flymex"
            version = "0.1.0"
            afterEvaluate { from(components["release"]) }
        }
    }
}
