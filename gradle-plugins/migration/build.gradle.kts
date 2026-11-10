import org.jetbrains.kotlin.gradle.dsl.JvmTarget

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

dependencies {
    implementation(libs.plugin.kotlin)
}

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.java.get())
    targetCompatibility = sourceCompatibility
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(libs.versions.java.get().toInt())
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.java.get()))
    }
}

gradlePlugin {
    plugins {
        create("antPathSourcSetPlugin") {
            id = "au.com.trgtd.migration.antpathsourceset"
            implementationClass = "au.com.trgtd.migration.AntPathSourceSetPlugin"
        }
    }
}