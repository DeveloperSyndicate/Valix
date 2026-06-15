package io.valix.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.Internal
import java.io.File
import java.net.URLClassLoader

abstract class BaseValixTask : DefaultTask() {

    @get:InputFiles
    val classpathFiles: FileCollection by lazy {
        val sourceSets = project.extensions.findByType(SourceSetContainer::class.java)
        sourceSets?.getByName("main")?.runtimeClasspath ?: project.files()
    }

    @get:Internal
    var outputDirectoryPath: String = "build/valix"

    protected fun executeWithMetadata(block: (ClassLoader, List<Any>) -> Unit) {
        val files = classpathFiles.files
        val urls = files.map { it.toURI().toURL() }.toTypedArray()
        val parentLoader = ClassLoader.getSystemClassLoader().parent ?: ClassLoader.getSystemClassLoader()
        val classLoader = URLClassLoader(urls, parentLoader)
        
        try {
            Class.forName("io.valix.generated.ValixRegistry", true, classLoader)
        } catch (e: ClassNotFoundException) {
            logger.warn("ValixRegistry not found on classpath. Ensure KSP has run and generated code.")
            return
        }

        val registryClass = Class.forName("io.valix.metadata.MetadataRegistry", true, classLoader)
        val instance = registryClass.getField("INSTANCE").get(null)
        val getAllMethod = registryClass.getMethod("getAll")
        val metadataList = getAllMethod.invoke(instance) as Collection<*>

        block(classLoader, metadataList.toList().filterNotNull())
    }
}

open class GenerateValixSchemasTask : BaseValixTask() {

    @get:OutputDirectory
    val outputDir: File
        get() = project.file("$outputDirectoryPath/schemas")

    @TaskAction
    fun run() {
        val dir = outputDir
        if (!dir.exists()) dir.mkdirs()

        executeWithMetadata { classLoader, metadataList ->
            val modelMetadataClass = classLoader.loadClass("io.valix.metadata.ValixModelMetadata")
            val generatorClass = classLoader.loadClass("io.valix.schema.JsonSchemaGenerator")
            val generatorInstance = generatorClass.getField("INSTANCE").get(null)
            val generateMethod = generatorClass.getMethod("generate", modelMetadataClass)

            for (metadata in metadataList) {
                val modelSimpleName = metadata.javaClass.getMethod("getModelSimpleName").invoke(metadata) as String
                val schema = generateMethod.invoke(generatorInstance, metadata) as String
                val schemaFile = File(dir, "$modelSimpleName.schema.json")
                schemaFile.writeText(schema)
                logger.lifecycle("Generated JSON schema: ${schemaFile.absolutePath}")
            }
        }
    }
}

open class GenerateValixOpenApiTask : BaseValixTask() {

    @get:OutputFile
    val outputFile: File
        get() = project.file("$outputDirectoryPath/openapi-components.yaml")

    @TaskAction
    fun run() {
        val file = outputFile
        val parent = file.parentFile
        if (!parent.exists()) parent.mkdirs()

        executeWithMetadata { classLoader, metadataList ->
            val modelMetadataClass = classLoader.loadClass("io.valix.metadata.ValixModelMetadata")
            val generatorClass = classLoader.loadClass("io.valix.schema.OpenApiSchemaGenerator")
            val generatorInstance = generatorClass.getField("INSTANCE").get(null)
            val generateMethod = generatorClass.getMethod("generateComponent", modelMetadataClass)

            val sb = StringBuilder()
            sb.append("components:\n")
            sb.append("  schemas:\n")

            for (metadata in metadataList) {
                val yaml = generateMethod.invoke(generatorInstance, metadata) as String
                val indentedYaml = yaml.lines().joinToString("\n") { "    $it" }
                sb.append(indentedYaml).append("\n")
            }

            file.writeText(sb.toString())
            logger.lifecycle("Generated OpenAPI YAML components: ${file.absolutePath}")
        }
    }
}

open class GenerateValixDocsTask : BaseValixTask() {

    @get:OutputDirectory
    val outputDir: File
        get() = project.file("$outputDirectoryPath/docs")

    @TaskAction
    fun run() {
        val dir = outputDir
        if (!dir.exists()) dir.mkdirs()

        executeWithMetadata { classLoader, metadataList ->
            for (metadata in metadataList) {
                val modelSimpleName = metadata.javaClass.getMethod("getModelSimpleName").invoke(metadata) as String
                val doc = generateMarkdownDoc(metadata, classLoader)
                val docFile = File(dir, "${modelSimpleName}ValidationDocs.md")
                docFile.writeText(doc)
                logger.lifecycle("Generated validation docs: ${docFile.absolutePath}")
            }
        }
    }

    private fun generateMarkdownDoc(metadata: Any, classLoader: ClassLoader): String {
        val modelFqName = metadata.javaClass.getMethod("getModelFqName").invoke(metadata) as String
        val modelSimpleName = metadata.javaClass.getMethod("getModelSimpleName").invoke(metadata) as String
        val fields = metadata.javaClass.getMethod("getFields").invoke(metadata) as List<*>
        val classConstraints = metadata.javaClass.getMethod("getClassConstraints").invoke(metadata) as List<*>

        val sb = java.lang.StringBuilder()
        sb.append("# Validation Documentation: $modelSimpleName\n\n")
        sb.append("- **Class FQN**: `$modelFqName`\n\n")

        if (fields.isNotEmpty()) {
            sb.append("## Field Constraints\n\n")
            sb.append("| Field | Type | Required | Display Name | Description | Constraints |\n")
            sb.append("| --- | --- | --- | --- | --- | --- |\n")
            for (field in fields.filterNotNull()) {
                val name = field.javaClass.getMethod("getName").invoke(field) as String
                val type = field.javaClass.getMethod("getType").invoke(field) as String
                val required = field.javaClass.getMethod("getRequired").invoke(field) as Boolean
                val displayName = field.javaClass.getMethod("getDisplayName").invoke(field) as String
                val description = field.javaClass.getMethod("getDescription").invoke(field) as String
                val constraints = field.javaClass.getMethod("getConstraints").invoke(field) as List<*>

                val constraintsStr = constraints.filterNotNull().joinToString("<br>") { constraint ->
                    val code = constraint.javaClass.getMethod("getConstraintCode").invoke(constraint) as String
                    val params = constraint.javaClass.getMethod("getParams").invoke(constraint) as Map<*, *>
                    val msg = constraint.javaClass.getMethod("getDefaultMessage").invoke(constraint) as String
                    val groups = constraint.javaClass.getMethod("getGroups").invoke(constraint) as List<*>
                    
                    var str = "**$code**"
                    if (params.isNotEmpty()) {
                        str += " (${params.entries.joinToString(", ") { "${it.key}=${it.value}" }})"
                    }
                    str += " - *\"$msg\"*"
                    if (groups.isNotEmpty()) {
                        str += " (groups: ${groups.joinToString(", ")})"
                    }
                    str
                }

                sb.append("| `$name` | `$type` | ${if (required) "Yes" else "No"} | $displayName | ${description.ifEmpty { "-" }} | ${constraintsStr.ifEmpty { "-" }} |\n")
            }
            sb.append("\n")
        }

        if (classConstraints.isNotEmpty()) {
            sb.append("## Class Level Constraints\n\n")
            for (constraint in classConstraints.filterNotNull()) {
                val code = constraint.javaClass.getMethod("getConstraintCode").invoke(constraint) as String
                val params = constraint.javaClass.getMethod("getParams").invoke(constraint) as Map<*, *>
                val msg = constraint.javaClass.getMethod("getDefaultMessage").invoke(constraint) as String
                val groups = constraint.javaClass.getMethod("getGroups").invoke(constraint) as List<*>

                sb.append("- **$code**")
                if (params.isNotEmpty()) {
                    sb.append(" (${params.entries.joinToString(", ") { "${it.key}=${it.value}" }})")
                }
                sb.append(" - *\"$msg\"*")
                if (groups.isNotEmpty()) {
                    sb.append(" (groups: ${groups.joinToString(", ")})")
                }
                sb.append("\n")
            }
        }

        return sb.toString()
    }
}

open class GenerateValixMetadataTask : BaseValixTask() {

    @get:OutputFile
    val outputFile: File
        get() = project.file("$outputDirectoryPath/valix-metadata.json")

    @TaskAction
    fun run() {
        val file = outputFile
        val parent = file.parentFile
        if (!parent.exists()) parent.mkdirs()

        executeWithMetadata { classLoader, metadataList ->
            val serializationKtClass = classLoader.loadClass("io.valix.serialization.ValixSerializationKt")
            val modelMetadataClass = classLoader.loadClass("io.valix.metadata.ValixModelMetadata")
            val toJsonMethod = serializationKtClass.getMethod("toJson", modelMetadataClass)

            val sb = StringBuilder()
            sb.append("[\n")
            for (i in metadataList.indices) {
                val metadata = metadataList[i]
                val json = toJsonMethod.invoke(null, metadata) as String
                val indentedJson = json.lines().joinToString("\n") { "  $it" }
                sb.append(indentedJson)
                if (i < metadataList.size - 1) sb.append(",")
                sb.append("\n")
            }
            sb.append("]")

            file.writeText(sb.toString())
            logger.lifecycle("Generated combined Valix metadata JSON: ${file.absolutePath}")
        }
    }
}
