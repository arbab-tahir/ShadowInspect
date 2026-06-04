import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}
val vtApiKey: String = localProperties.getProperty("VIRUSTOTAL_API_KEY", "")
val geminiApiKey: String = localProperties.getProperty("GEMINI_API_KEY", "")

// Module 7 API Keys
val abstractApiKey: String = localProperties.getProperty("ABSTRACT_API_KEY", "")
val numverifyApiKey: String = localProperties.getProperty("NUMVERIFY_API_KEY", "")
val ipqualityApiKey: String = localProperties.getProperty("IPQUALITY_KEY", "")
val veriphoneApiKey: String = localProperties.getProperty("VERIPHONE_KEY", "")
val urlscanApiKey: String = localProperties.getProperty("URLSCAN_API_KEY", "")


android {
    namespace = "com.shadowinspect.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.shadowinspect.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "3.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        buildConfigField("String", "VIRUSTOTAL_API_KEY", "\"$vtApiKey\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        
        // Module 7 Build Config Fields
        buildConfigField("String", "ABSTRACT_API_KEY", "\"$abstractApiKey\"")
        buildConfigField("String", "NUMVERIFY_API_KEY", "\"$numverifyApiKey\"")
        buildConfigField("String", "IPQUALITY_KEY", "\"$ipqualityApiKey\"")
        buildConfigField("String", "VERIPHONE_KEY", "\"$veriphoneApiKey\"")
        buildConfigField("String", "URLSCAN_API_KEY", "\"$urlscanApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }



    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/INDEX.LIST",
                "META-INF/jpms.args",
                "META-INF/io.netty.versions.properties",
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                "META-INF/versions/9/module-info.class"
            )
        }
        pickFirsts += setOf(
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE",
            "META-INF/NOTICE",
            "META-INF/license.txt",
            "META-INF/notice.txt",
            "META-INF/ASL-2.0.txt",
            "META-INF/LGPL-3.0.txt",
            "META-INF/apache-license-2.0.txt"
        )
        jniLibs {
            useLegacyPackaging = true
        }
    }
    
    // Prevent compressing TFLite models
    androidResources {
        noCompress("tflite")
    }
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")




    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // DocumentFile
    implementation("androidx.documentfile:documentfile:1.0.1")

    // PDF Generation (iText7)
    implementation("com.itextpdf:itext7-core:7.2.5") {
        exclude(group = "javax.xml.stream", module = "stax-api")
        exclude(group = "org.bouncycastle")
    }

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Vico Charts (Analytics)
    val vicoVersion = "1.13.1"
    implementation("com.patrykandpatrick.vico:compose:$vicoVersion")
    implementation("com.patrykandpatrick.vico:compose-m3:$vicoVersion")
    implementation("com.patrykandpatrick.vico:core:$vicoVersion")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.04.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // PDF Processing (Apache PDFBox for Android)
    implementation("com.tom-roush:pdfbox-android:2.0.27.0") {
        exclude(group = "org.bouncycastle")
    }

    // Office Document Processing (Apache POI)
    implementation("org.apache.poi:poi:5.2.3") {
        exclude(group = "stax", module = "stax-api")
        exclude(group = "commons-logging", module = "commons-logging")
        exclude(group = "org.bouncycastle")
    }
    implementation("org.apache.poi:poi-ooxml:5.2.3") {
        exclude(group = "stax", module = "stax-api")
        exclude(group = "commons-logging", module = "commons-logging")
        exclude(group = "org.bouncycastle")
    }
    implementation("org.apache.poi:poi-scratchpad:5.2.3") {
        exclude(group = "stax", module = "stax-api")
        exclude(group = "commons-logging", module = "commons-logging")
        exclude(group = "org.bouncycastle")
    }

    // XML processing (required by POI-OOXML)
    implementation("org.apache.xmlbeans:xmlbeans:5.1.1") {
        exclude(group = "stax", module = "stax-api")
    }

    // Single BouncyCastle version (shared by iText7 & PDFBox)
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
    
    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
}

configurations.all {
    exclude(group = "stax", module = "stax-api")
    exclude(group = "xml-apis", module = "xml-apis")
    exclude(group = "javax.xml.stream", module = "stax-api")
    exclude(group = "commons-logging", module = "commons-logging")
}

kapt {
    correctErrorTypes = true
    arguments {
        arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
    }
}

hilt {
    enableAggregatingTask = true
}



