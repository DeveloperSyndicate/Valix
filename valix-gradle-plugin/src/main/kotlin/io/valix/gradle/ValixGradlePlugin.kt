package io.valix.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

open class ValixExtension {
    var outputDir: String = "build/valix"
}

class ValixGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("valix", ValixExtension::class.java)

        val schemasTask = project.tasks.register("generateValixSchemas", GenerateValixSchemasTask::class.java) { task ->
            task.group = "valix"
            task.description = "Generates JSON schemas for Valix validated models."
        }

        val openApiTask = project.tasks.register("generateValixOpenApi", GenerateValixOpenApiTask::class.java) { task ->
            task.group = "valix"
            task.description = "Generates OpenAPI 3.1 YAML component blocks for Valix validated models."
        }

        val docsTask = project.tasks.register("generateValixDocs", GenerateValixDocsTask::class.java) { task ->
            task.group = "valix"
            task.description = "Generates validation Markdown documentation for Valix validated models."
        }

        val metadataTask = project.tasks.register("generateValixMetadata", GenerateValixMetadataTask::class.java) { task ->
            task.group = "valix"
            task.description = "Generates a consolidated JSON metadata dump of all Valix validated models."
        }

        project.afterEvaluate {
            val output = extension.outputDir

            schemasTask.configure { it.outputDirectoryPath = output }
            openApiTask.configure { it.outputDirectoryPath = output }
            docsTask.configure { it.outputDirectoryPath = output }
            metadataTask.configure { it.outputDirectoryPath = output }

            project.tasks.withType(BaseValixTask::class.java).configureEach { task ->
                val compileKotlin = project.tasks.findByName("compileKotlin")
                if (compileKotlin != null) {
                    task.dependsOn(compileKotlin)
                } else {
                    val classes = project.tasks.findByName("classes")
                    if (classes != null) {
                        task.dependsOn(classes)
                    }
                }
            }
        }
    }
}
