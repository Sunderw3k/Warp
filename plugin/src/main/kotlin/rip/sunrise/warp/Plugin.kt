package rip.sunrise.warp

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

@Suppress("unused")
class Plugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.tasks.withType(JavaCompile::class.java).forEach { task ->
            task.doLast {
                transformClassesInDirectory(task.destinationDirectory.get().asFile)
            }
        }

        target.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            target.tasks.withType(KotlinCompile::class.java).forEach { task ->
                task.doLast {
                    transformClassesInDirectory(task.destinationDirectory.get().asFile)
                }
            }
        }
    }

    private fun transformClassesInDirectory(directory: File) {
        directory.walkTopDown().filter { it.extension == "class" }.forEach {
            Transformer.transform(it)
        }
    }
}