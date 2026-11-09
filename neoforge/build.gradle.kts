import org.slf4j.event.Level

plugins {
    id("idea")
    id("net.neoforged.moddev") version "2.0.28-beta"
}

apply(plugin = "net.neoforged.moddev")

dependencies {
    val minecraftVersion = rootProject.properties["minecraft_version"].toString()
    val geckoLibVersion = rootProject.properties["geckolib_version"].toString()
    implementation("software.bernie.geckolib:geckolib-neoforge-$minecraftVersion:$geckoLibVersion")
    implementation("de.javagl:obj:0.4.0")
    jarJar("de.javagl:obj:0.4.0")
    additionalRuntimeClasspath("de.javagl:obj:0.4.0")

    implementation(project.project(":common").sourceSets.getByName("main").output)
}

neoForge {
    version = rootProject.properties["neoforge_version"].toString()

    parchment {
        minecraftVersion = rootProject.properties["minecraft_version"].toString()
        mappingsVersion = rootProject.properties["parchment_release"].toString()
    }

    runs {
        create("Client") {
            client()
            gameDirectory.set(file("run-client"))
        }
        create("Server") {
            server()
            gameDirectory.set(file("run-server"))
            programArguments.add("--nogui")
        }
        create("Datagen") {
            data()

            programArguments.addAll(
                "--mod", rootProject.properties["archives_base_name"].toString(),
                "--all",
                "--output", file("src/generated/resources/").getAbsolutePath(),
                "--existing", file("src/main/resources/").getAbsolutePath())
        }

        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = Level.DEBUG
            if (project.hasProperty("vvQa")) systemProperty("vv.qa", "true")
            if (project.hasProperty("vvClientQa")) systemProperty("vv.clientQa", "true")
            if (project.hasProperty("vvReloadQa")) systemProperty("vv.reloadQa", "true")
        }
    }

    mods {
        create(rootProject.properties["archives_base_name"].toString()) {
            sourceSet(sourceSets.main.get())
        }
    }

    accessTransformers {
        file("neoforge/src/main/resources/META-INF/accesstransformer.cfg")
    }
}

if (project.hasProperty("vvQa") || project.hasProperty("vvClientQa") || project.hasProperty("vvReloadQa")) {
    sourceSets.main.get().java.srcDir("src/qa/java")
}

sourceSets.main.get().resources {
    srcDir("src/generated/resources")
}

tasks {
    jar {
        val main = project.project(":common").sourceSets.main.get()
        from(main.output.classesDirs)
        from(main.output.resourcesDir)
        from(rootProject.file("ASSET-RIGHTS.md"))
    }

    named("compileTestJava").configure {
        enabled = false
    }

    val notNeoTask: (Task) -> Boolean = { !it.name.startsWith("neo") && !it.name.startsWith("compileService") }

    withType<JavaCompile>().configureEach {
        source(project(":common").sourceSets.main.get().allSource)
    }

    withType<Javadoc>().matching(notNeoTask).configureEach {
        source(project(":common").sourceSets.main.get().allSource)
    }

    withType<ProcessResources>().matching(notNeoTask).configureEach {
        from(project(":common").sourceSets.main.get().resources)

        filesMatching("META-INF/neoforge.mods.toml") {
            expand(mapOf("version" to rootProject.properties["mod_version"]))
        }
    }
}
