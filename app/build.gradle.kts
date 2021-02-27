plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-android-extensions")
    kotlin("plugin.serialization") version "1.4.30"
}

android {
    compileSdkVersion(29)
    buildToolsVersion("29.0.3")

    defaultConfig {
        applicationId = "com.lt.retrofit.sca.test"
        minSdkVersion(21)
        targetSdkVersion(29)
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    //启用java8
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
}

dependencies {
    //引入jar和aar
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    //网络请求工具类
//    implementation("com.github.ltttttttttttt:retrofit:1.2.5")
    //网络请求
//    implementation("com.squareup.okhttp3:okhttp:3.14.9")
    //kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.4.30")
    //kotlin反射
    implementation(kotlin("reflect"))
    //协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.1")
    //gson序列化适配器
    implementation("com.squareup.retrofit2:converter-gson:2.7.0") {
        exclude("com.squareup.retrofit2", "retrofit")//使其不自动依赖retrofit // exclude module: 'retrofit'
    }
    implementation(project(":SocketCallAdapter"))

}