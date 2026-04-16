import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.tasks.bundling.Jar

plugins {
    java
}

base {
    archivesName.set("mageswand-spell-pack")
}

val paperApiVersion = providers.gradleProperty("paperApiVersion").orElse("1.21.11-R0.1-SNAPSHOT")
val executorPackage = "com.firstmage.mageswand.wand.executor"

fun parseSelectedExecutors(raw: String): List<String> =
    raw.split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()

fun toExecutorClassName(token: String): String =
    if ('.' in token) token else "$executorPackage.$token"

fun toExecutorClassFilePattern(token: String): String {
    val className = toExecutorClassName(token)
    val classPath = className.replace('.', '/')
    val packagePath = classPath.substringBeforeLast('/')
    val simpleName = classPath.substringAfterLast('/')
    return "$packagePath/${simpleName}*.class"
}

fun availableExecutorClassNames(): List<String> {
    val root = projectDir.resolve("src/main/java/${executorPackage.replace('.', '/')}")
    if (!root.exists()) {
        return emptyList()
    }

    return root.walkTopDown()
        .filter { it.isFile && it.extension == "java" }
        .map { file ->
            "$executorPackage.${file.nameWithoutExtension}"
        }
        .sorted()
        .toList()
}

val selectedExecutorsProvider = providers.gradleProperty("spellExecutors")
    .map(::parseSelectedExecutors)

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(project(":mageswand-core"))
    compileOnly("io.papermc.paper:paper-api:${paperApiVersion.get()}")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.register("listSpellExecutors") {
    group = "help"
    description = "Lists the executor classes available in the spell-pack project."

    doLast {
        val executors = availableExecutorClassNames()
        if (executors.isEmpty()) {
            logger.lifecycle("No spell executors were found in $executorPackage")
        } else {
            logger.lifecycle("Available spell executors:")
            executors.forEach { logger.lifecycle(" - $it") }
        }
    }
}

val generateSelectedSpellPackConfig by tasks.registering {
    group = "build"
    description = "Generates a wand-fuels.json containing only the executors listed in -PspellExecutors."

    val sourceConfig = rootProject.layout.projectDirectory.file("examples/wand-fuels.with-spell-pack.json")
    val outputConfig = layout.buildDirectory.file("generated/selected-spell-pack/wand-fuels.json")

    inputs.file(sourceConfig)
    outputs.file(outputConfig)

    doLast {
        val selectedTokens = selectedExecutorsProvider.orNull.orEmpty()
        if (selectedTokens.isEmpty()) {
            throw GradleException("Provide -PspellExecutors=ExecutorOne,ExecutorTwo or fully-qualified class names.")
        }

        val selectedClasses = selectedTokens.map(::toExecutorClassName).toSet()
        val availableClasses = availableExecutorClassNames().toSet()
        val unknownClasses = selectedClasses - availableClasses
        if (unknownClasses.isNotEmpty()) {
            throw GradleException(
                "Unknown executor(s): ${unknownClasses.joinToString(", ")}. " +
                    "Run :mageswand-spell-pack:listSpellExecutors to see valid names."
            )
        }

        @Suppress("UNCHECKED_CAST")
        val entries = JsonSlurper().parse(sourceConfig.asFile) as List<Map<String, Any?>>
        val filteredEntries = entries.filter { entry ->
            val className = entry["executorClass"]?.toString()
            className in selectedClasses
        }

        if (filteredEntries.size != selectedClasses.size) {
            val present = filteredEntries.mapNotNull { it["executorClass"]?.toString() }.toSet()
            val missing = selectedClasses - present
            throw GradleException(
                "No wand-fuels mapping found for: ${missing.joinToString(", ")}. " +
                    "Update examples/wand-fuels.with-spell-pack.json or choose a mapped executor."
            )
        }

        val target = outputConfig.get().asFile
        target.parentFile.mkdirs()
        target.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(filteredEntries)) + System.lineSeparator())
    }
}

tasks.register<Jar>("selectedSpellPackJar") {
    group = "build"
    description = "Builds a spell-pack jar containing only the executors listed in -PspellExecutors."

    dependsOn(tasks.classes)

    archiveBaseName.set("mageswand-spell-pack-selected")

    val patternProvider = selectedExecutorsProvider
        .map { selected ->
            if (selected.isEmpty()) {
                emptyList()
            } else {
                selected.map(::toExecutorClassFilePattern)
            }
        }
        .orElse(emptyList())

    from(sourceSets.main.get().output) {
        include(patternProvider.get())
    }

    doFirst {
        val selectedTokens = selectedExecutorsProvider.orNull.orEmpty()
        if (selectedTokens.isEmpty()) {
            throw GradleException("Provide -PspellExecutors=ExecutorOne,ExecutorTwo or fully-qualified class names.")
        }

        val selectedClasses = selectedTokens.map(::toExecutorClassName).toSet()
        val availableClasses = availableExecutorClassNames().toSet()
        val unknownClasses = selectedClasses - availableClasses
        if (unknownClasses.isNotEmpty()) {
            throw GradleException(
                "Unknown executor(s): ${unknownClasses.joinToString(", ")}. " +
                    "Run :mageswand-spell-pack:listSpellExecutors to see valid names."
            )
        }

        logger.lifecycle("Packing selected spell executors: ${selectedClasses.joinToString(", ")}")
    }
}
