buildscript {
    val kotlin_version by extra("1.4.21")
    repositories {
        maven("http://maven.aliyun.com/nexus/content/groups/public/")
        google()
        maven("https://dl.bintray.com/umsdk/release")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.1.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.20")
    }
}

allprojects {
    repositories {
        maven("http://maven.aliyun.com/nexus/content/groups/public/")
        google()
        jcenter()
        maven("https://jitpack.io")
        maven("https://dl.bintray.com/umsdk/release")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}