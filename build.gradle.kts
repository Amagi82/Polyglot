import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
    id("org.jetbrains.compose") version "1.0.0-beta5"
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.animation)
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "15"
        freeCompilerArgs = listOf(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlin.ExperimentalStdlibApi",
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xopt-in=kotlinx.coroutines.FlowPreview",
            "-Xopt-in=kotlinx.coroutines.DelicateCoroutinesApi",
            "-Xopt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-Xopt-in=androidx.compose.foundation.ExperimentalFoundationApi",
        )
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "polyglot"
            packageVersion = "1.0.0"
        }
    }
}

tasks {
    val generateResourceReferences by creating(DefaultTask::class) {
        println("generateResourceReferences")
        val resourceDir = File(projectDir, "src/main/resources")
        val outputDir = File(buildDir, "generated/main").apply(File::mkdirs)
        val outputFile = File(outputDir, "R.kt").apply(File::createNewFile)

        val svgFiles: List<File> = resourceDir.listFiles().orEmpty().filter { it.extension == "svg" }

        fun String.snakeToLowerCamelCase(): String = buildString {
            var capitalize = false
            this@snakeToLowerCamelCase.forEach {
                when {
                    it == '_' -> capitalize = true
                    capitalize -> {
                        capitalize = false
                        append(it.toUpperCase())
                    }
                    else -> append(it)
                }
            }
        }

        outputFile.writer().use { writer ->
            writer.appendLine("object R {")
            if (svgFiles.isNotEmpty()) {
                writer.appendLine("\tobject drawable {")
                svgFiles.forEach {
                    writer.appendLine("\t\tval ${it.nameWithoutExtension.replace("_black_24dp", "").snakeToLowerCamelCase()} = \"${it.name}\"")
                }
                writer.appendLine("\t}")
            }
            writer.appendLine('}')
        }
        sourceSets.main {
            java.srcDir(outputDir)
        }
    }
    build {
        dependsOn(generateResourceReferences)
    }
}
