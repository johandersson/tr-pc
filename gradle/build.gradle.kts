plugins {
    java
}

allprojects {
    dependencyLocking {
        lockAllConfigurations()
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(rootProject.libs.versions.java.get())
        }
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        //implementation(platform(rootProject.libs.netbeans.cluster.platform))
        //implementation(platform(rootProject.libs.netbeans.cluster.harness))
        //
        //runtimeOnly(libs.netbeans.cluster.platform)
        //implementation(libs.netbeans.api.core.multitabs)

        runtimeOnly(rootProject.libs.netbeans.modules.projectapi.nb)
        runtimeOnly(rootProject.libs.netbeans.modules.core.multitabs.project)
        runtimeOnly(rootProject.libs.netbeans.modules.core.windows)

        testImplementation(rootProject.libs.junit.jupiter)
        testImplementation(rootProject.libs.junit4)
        testRuntimeOnly(rootProject.libs.junit.vintage.engine)
    }

    tasks.named<Test>("test") {
        useJUnitPlatform()
    }

    tasks.withType<Jar>().configureEach {
        manifest {
            from("${project.projectDir}/manifest.mf")
        }
    }
}


tasks {
    val downloadNetBeans = register<Exec>("downloadNetBeans") {
        description = "Prepares the checksum-verified NetBeans platform using Ant"
        group = "prepare"
        workingDir(rootDir.parentFile)
        commandLine("ant", "-f", "nbproject/platform.xml", "prepare")
    }
    register("unpackNetBeans") {
        description = "Prepares the NetBeans platform using Ant"
        group = "prepare"
        dependsOn(downloadNetBeans)
    }
}
