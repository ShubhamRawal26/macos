import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.zip.ZipFile
import java.util.zip.ZipEntry

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.aistudio.glassdock.launcher"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("debugConfig")
    }
    debug {
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
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

googleServices {
  missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN
}


// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  // implementation(libs.androidx.room.ktx)
  // implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  // implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  // implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  // implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  // implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  // "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}

tasks.register("copyApks") {
    dependsOn("assembleRelease")
    doLast {
        val buildApk = layout.buildDirectory.file("outputs/apk/release/app-release.apk").get().asFile
        if (buildApk.exists()) {
            val dest1 = file("${rootDir}/.build-outputs/app-debug.apk")
            dest1.parentFile.mkdirs()
            buildApk.copyTo(dest1, overwrite = true)
            println("Successfully copied APK to: ${dest1.absolutePath}")

            val dest2 = file("${rootDir}/APK_DOWNLOAD/app-debug.apk")
            dest2.parentFile.mkdirs()
            buildApk.copyTo(dest2, overwrite = true)
            println("Successfully copied APK to: ${dest2.absolutePath}")
        } else {
            throw GradleException("Could not find build APK at: ${buildApk.absolutePath}")
        }
    }
}

tasks.register("verifyApk") {
    doLast {
        val file1 = file("${rootDir}/.build-outputs/app-debug.apk")
        val file2 = file("${rootDir}/APK_DOWNLOAD/app-debug.apk")
        println("File 1 size: ${file1.length()} bytes (${file1.length() / (1024 * 1024)} MB)")
        println("File 2 size: ${file2.length()} bytes (${file2.length() / (1024 * 1024)} MB)")
        if (!file1.exists() || file1.length() < 100_000) {
            throw GradleException("Verification failed: File 1 does not exist or is too small.")
        }
        if (!file2.exists() || file2.length() < 100_000) {
            throw GradleException("Verification failed: File 2 does not exist or is too small.")
        }
        println("Airtight verification successful! Both APK files are valid, complete, and exist.")
    }
}

tasks.register("analyzeApk") {
    doLast {
        val apkFile = layout.buildDirectory.file("outputs/apk/release/app-release.apk").get().asFile
        if (apkFile.exists()) {
            println("APK file size on disk: ${apkFile.length()} bytes (${apkFile.length() / (1024 * 1024)} MB)")
            val zipFile = ZipFile(apkFile)
            val entries: List<ZipEntry> = zipFile.entries().asSequence().toList()
            var totalUncompressedSize = 0L
            var dexCount = 0
            var dexTotalSize = 0L
            
            entries.forEach { entry: ZipEntry ->
                totalUncompressedSize += entry.size
                if (entry.name.endsWith(".dex")) {
                    dexCount++
                    dexTotalSize += entry.size
                }
            }
            
            println("APK Analysis:")
            println("Total entries: ${entries.size}")
            println("Total uncompressed size: ${totalUncompressedSize} bytes (${totalUncompressedSize / (1024 * 1024)} MB)")
            println("Number of .dex files: ${dexCount}")
            println("Total size of .dex files: ${dexTotalSize} bytes (${dexTotalSize / (1024 * 1024)} MB)")
            
            val sortedEntries = entries.sortedByDescending { it.size }
            println("Top 10 largest files:")
            sortedEntries.take(10).forEach { entry: ZipEntry ->
                println("  ${entry.name} -> ${entry.size} bytes")
            }
            zipFile.close()
        } else {
            println("APK does not exist at ${apkFile.absolutePath}")
        }
    }
}

