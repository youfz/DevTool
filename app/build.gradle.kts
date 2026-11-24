plugins { alias(libs.plugins.android.application) }
android {
 namespace = "com.test.tool"
 compileSdk = 36
 defaultConfig { applicationId = "com.test.tool"; minSdk = 26; targetSdk = 36; versionCode = 1; versionName = "1.0"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
 buildTypes { release { isMinifyEnabled = false } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
 buildFeatures { viewBinding = true }
}
dependencies {
 implementation("androidx.appcompat:appcompat:1.7.1")
 implementation("com.google.android.material:material:1.12.0")
 implementation("androidx.activity:activity-ktx:1.10.1")
 implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
 implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
 implementation("androidx.datastore:datastore-preferences:1.1.1")
 implementation("com.google.code.gson:gson:2.11.0")
 implementation("com.squareup.okhttp3:okhttp:4.12.0")
 testImplementation("junit:junit:4.13.2")
 testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
 androidTestImplementation(libs.androidx.espresso.core)
 androidTestImplementation(libs.androidx.junit)
}
