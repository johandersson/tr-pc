val buildTask = tasks.register("buildPlugins")

allprojects {
    dependencyLocking {
        lockAllConfigurations()
    }
}

subprojects {
    buildTask.configure { dependsOn(tasks.named("build")) }
}
