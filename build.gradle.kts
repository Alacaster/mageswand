import xyz.jpenilla.runpaper.task.RunServer

plugins {
    base
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

val pluginGroup = providers.gradleProperty("group").get()
val pluginVersion = providers.gradleProperty("version").get()
val paperRunVersion = providers.gradleProperty("paperRunVersion").orElse("1.21.11")
val runJvmMin = providers.gradleProperty("runJvmMin").orElse("2G")
val runJvmMax = providers.gradleProperty("runJvmMax").orElse("2G")

allprojects {
    group = pluginGroup
    version = pluginVersion
}

subprojects {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

runPaper {
    disablePluginJarDetection()
}

val coreProject = project(":mageswand-core")
val spellPackProject = project(":mageswand-spell-pack")

val coreRuntimeJar = coreProject.layout.buildDirectory.file("libs/mageswand-${project.version}.jar")
val fullSpellPackJar = spellPackProject.layout.buildDirectory.file("libs/mageswand-spell-pack-${project.version}.jar")
val selectedSpellPackJar = spellPackProject.layout.buildDirectory.file("libs/mageswand-spell-pack-selected-${project.version}.jar")
val selectedSpellPackConfig = spellPackProject.layout.buildDirectory.file("generated/selected-spell-pack/wand-fuels.json")

val prepareRunFullSpellPack by tasks.registering {
    group = "run paper"
    description = "Copies the full spell-pack jar into run/plugins/mageswand/spells."

    dependsOn(":mageswand-spell-pack:jar")
    outputs.file(layout.projectDirectory.file("run/plugins/mageswand/spells/${fullSpellPackJar.get().asFile.name}"))

    doLast {
        val source = fullSpellPackJar.get().asFile
        if (!source.exists()) {
            throw GradleException("Full spell-pack jar was not built: ${source.absolutePath}")
        }

        val spellsDir = layout.projectDirectory.dir("run/plugins/mageswand/spells").asFile
        spellsDir.mkdirs()
        spellsDir.listFiles { file -> file.isFile && file.extension == "jar" }?.forEach { it.delete() }
        source.copyTo(spellsDir.resolve(source.name), overwrite = true)
    }
}

val prepareRunSelectedSpellPack by tasks.registering {
    group = "run paper"
    description = "Copies the selected spell-pack jar into run-selected/plugins/mageswand/spells."

    dependsOn(":mageswand-spell-pack:selectedSpellPackJar")
    outputs.file(layout.projectDirectory.file("run-selected/plugins/mageswand/spells/${selectedSpellPackJar.get().asFile.name}"))

    doLast {
        val source = selectedSpellPackJar.get().asFile
        if (!source.exists()) {
            throw GradleException("Selected spell-pack jar was not built: ${source.absolutePath}")
        }

        val spellsDir = layout.projectDirectory.dir("run-selected/plugins/mageswand/spells").asFile
        spellsDir.mkdirs()
        spellsDir.listFiles { file -> file.isFile && file.extension == "jar" }?.forEach { it.delete() }
        source.copyTo(spellsDir.resolve(source.name), overwrite = true)
    }
}

val clearRunCoreOnlySpellPack by tasks.registering {
    group = "run paper"
    description = "Removes any staged spell-pack jars from the core-only run directory."

    doLast {
        val spellsDir = layout.projectDirectory.dir("run-core-only/plugins/mageswand/spells").asFile
        if (spellsDir.exists()) {
            spellsDir.listFiles { file -> file.isFile && file.extension == "jar" }?.forEach { it.delete() }
        }
    }
}


val prepareRunCoreOnlyConfig by tasks.registering {
    group = "run paper"
    description = "Writes an empty wand-fuels.json for the core-only run directory."

    val outputFile = layout.projectDirectory.file("run-core-only/plugins/mageswand/wand-fuels.json")
    outputs.file(outputFile)

    doLast {
        val target = outputFile.asFile
        target.parentFile.mkdirs()
        target.writeText("{\n  \"mappings\": []\n}\n")
    }
}

val prepareRunFullConfig by tasks.registering {
    group = "run paper"
    description = "Copies the example full spell-pack config into the default run directory."

    val inputFile = layout.projectDirectory.file("examples/wand-fuels.with-spell-pack.json")
    val outputFile = layout.projectDirectory.file("run/plugins/mageswand/wand-fuels.json")

    inputs.file(inputFile)
    outputs.file(outputFile)

    doLast {
        val target = outputFile.asFile
        target.parentFile.mkdirs()
        inputFile.asFile.copyTo(target, overwrite = true)
    }
}

val prepareRunSelectedConfig by tasks.registering {
    group = "run paper"
    description = "Copies the generated selected-spell-pack config into the selected run directory."

    dependsOn(":mageswand-spell-pack:generateSelectedSpellPackConfig")

    val outputFile = layout.projectDirectory.file("run-selected/plugins/mageswand/wand-fuels.json")
    outputs.file(outputFile)

    doLast {
        val generated = selectedSpellPackConfig.get().asFile
        if (!generated.exists()) {
            throw GradleException("Selected spell-pack config was not generated. Run with -PspellExecutors=ExecutorOne,ExecutorTwo")
        }

        val target = outputFile.asFile
        target.parentFile.mkdirs()
        generated.copyTo(target, overwrite = true)
    }
}

tasks.named("assemble") {
    dependsOn(":mageswand-core:assemble", ":mageswand-spell-pack:assemble")
}

tasks.named("build") {
    dependsOn(":mageswand-core:build", ":mageswand-spell-pack:build")
}

tasks.register("compileCore") {
    group = "build"
    description = "Compiles only the core plugin classes."
    dependsOn(":mageswand-core:classes")
}

tasks.register("compileSpellPack") {
    group = "build"
    description = "Compiles only the spell-pack classes."
    dependsOn(":mageswand-spell-pack:classes")
}

tasks.register("compileAll") {
    group = "build"
    description = "Compiles the core plugin and the spell pack."
    dependsOn("compileCore", "compileSpellPack")
}

tasks.register("buildCoreJar") {
    group = "build"
    description = "Builds the Paper-runtime core plugin jar."
    dependsOn(":mageswand-core:assemble")
}

tasks.register("buildSpellPackJar") {
    group = "build"
    description = "Builds the full spell-pack jar."
    dependsOn(":mageswand-spell-pack:jar")
}

tasks.register("buildSelectedSpellPackJar") {
    group = "build"
    description = "Builds a spell-pack jar containing only the executors listed in -PspellExecutors."
    dependsOn(":mageswand-spell-pack:selectedSpellPackJar", ":mageswand-spell-pack:generateSelectedSpellPackConfig")
}

tasks.register("buildRuntimeArtifacts") {
    group = "build"
    description = "Builds the core runtime jar and the full spell-pack jar."
    dependsOn("buildCoreJar", "buildSpellPackJar")
}

tasks.named<RunServer>("runServer") {
    group = "run paper"
    description = "Runs Paper with the core plugin and the full spell pack."
    dependsOn(":mageswand-core:assemble", prepareRunFullConfig, prepareRunFullSpellPack)

    minecraftVersion(paperRunVersion.get())
    runDirectory.set(file("run"))
    jvmArgs(
        "-Dcom.mojang.eula.agree=true",
        "-Xms${runJvmMin.get()}",
        "-Xmx${runJvmMax.get()}"
    )

    pluginJars(coreRuntimeJar)
}

tasks.register<RunServer>("runServerCoreOnly") {
    group = "run paper"
    description = "Runs Paper with only the core plugin and an empty wand-fuels.json."
    dependsOn(":mageswand-core:assemble", prepareRunCoreOnlyConfig, clearRunCoreOnlySpellPack)

    minecraftVersion(paperRunVersion.get())
    runDirectory.set(file("run-core-only"))
    jvmArgs(
        "-Dcom.mojang.eula.agree=true",
        "-Xms${runJvmMin.get()}",
        "-Xmx${runJvmMax.get()}"
    )

    pluginJars(coreRuntimeJar)
}

tasks.register<RunServer>("runServerSelected") {
    group = "run paper"
    description = "Runs Paper with the core plugin and a selected-only spell-pack jar. Requires -PspellExecutors."
    dependsOn(":mageswand-core:assemble", prepareRunSelectedConfig, prepareRunSelectedSpellPack)

    minecraftVersion(paperRunVersion.get())
    runDirectory.set(file("run-selected"))
    jvmArgs(
        "-Dcom.mojang.eula.agree=true",
        "-Xms${runJvmMin.get()}",
        "-Xmx${runJvmMax.get()}"
    )

    pluginJars(coreRuntimeJar)
}
