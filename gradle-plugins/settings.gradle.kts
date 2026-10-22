pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
val toolchain = java.util.Properties().apply {
    file("../nbproject/platform.properties").inputStream().use { load(it) }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/dependencies.versions.toml"))
            version("java", toolchain.getProperty("java-version"))
            version("netbeans-ide", toolchain.getProperty("netbeans-plat-version"))
            version("netbeans-api", "RELEASE${toolchain.getProperty("netbeans-plat-version")}0")
        }
    }
}

include("migration")
