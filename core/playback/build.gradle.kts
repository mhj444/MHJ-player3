plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mhj.player.core.playback"
    compileSdk = 35

    defaultConfig {
        minSdk = 31
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
    implementation(project(":core:model"))
    implementation(project(":core:data"))

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.media:media:1.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    // Room types leak through MhjDatabase.get() — need on classpath for service restore path.
    implementation("androidx.room:room-runtime:2.6.1")

    val media3 = "1.5.1"
    api("androidx.media3:media3-exoplayer:$media3")
    api("androidx.media3:media3-exoplayer-hls:$media3")
    api("androidx.media3:media3-session:$media3")
    api("androidx.media3:media3-ui:$media3")
    api("androidx.media3:media3-common:$media3")
    // FFmpeg audio decoders (AC3, E-AC3, DTS, TrueHD, ALAC, WMA… that many phones can't play).
    implementation("org.jellyfin.media3:media3-ffmpeg-decoder:1.5.0+1")

}
