package io.valix.schema

import io.valix.metadata.ValixModelMetadata
import io.valix.metadata.FieldMetadata
import io.valix.metadata.ConstraintMetadata
import io.valix.metadata.SchemaKeyword

object OpenApiSchemaGenerator {

    fun generateComponent(metadata: ValixModelMetadata): String {
        val sb = StringBuilder()
        sb.append("${metadata.modelSimpleName}:\n")
        sb.append("  type: object\n")
        sb.append("  properties:\n")

        val fields = metadata.fields
        for (field in fields) {
            sb.append("    ${field.name}:\n")
            val openApiType = when (field.type) {
                "kotlin.String" -> "string"
                "kotlin.Int", "kotlin.Long", "kotlin.Short", "kotlin.Byte" -> "integer"
                "kotlin.Double", "kotlin.Float" -> "number"
                "kotlin.Boolean" -> "boolean"
                else -> {
                    if (field.type.startsWith("kotlin.collections.List") || 
                        field.type.startsWith("kotlin.collections.Set") ||
                        field.type.startsWith("kotlin.collections.Collection") ||
                        field.type.contains("List") || field.type.contains("Set")) {
                        "array"
                    } else {
                        "object"
                    }
                }
            }
            sb.append("      type: $openApiType\n")

            if (field.description.isNotEmpty()) {
                sb.append("      description: \"${escapeYaml(field.description)}\"\n")
            }

            for (constraint in field.constraints) {
                val mapping = mapConstraintToYaml(constraint)
                if (mapping.isNotEmpty()) {
                    sb.append("      $mapping\n")
                }
            }
        }

        val requiredFields = fields.filter { it.required }.map { it.name }
        if (requiredFields.isNotEmpty()) {
            sb.append("  required:\n")
            for (req in requiredFields) {
                sb.append("    - $req\n")
            }
        }

        return sb.toString()
    }

    private fun mapConstraintToYaml(constraint: ConstraintMetadata): String {
        return when (constraint.schemaKeyword) {
            SchemaKeyword.MIN_LENGTH -> {
                val limit = constraint.params["value"] ?: constraint.params["limit"] ?: 0
                "minLength: $limit"
            }
            SchemaKeyword.MAX_LENGTH -> {
                val limit = constraint.params["value"] ?: constraint.params["limit"] ?: 0
                "maxLength: $limit"
            }
            SchemaKeyword.PATTERN -> {
                val regexp = constraint.params["regexp"] ?: ""
                "pattern: \"${escapeYaml(regexp.toString())}\""
            }
            SchemaKeyword.FORMAT_EMAIL -> "format: email"
            SchemaKeyword.FORMAT_URI -> "format: uri"
            SchemaKeyword.MINIMUM -> {
                val value = constraint.params["value"] ?: 0
                "minimum: $value"
            }
            SchemaKeyword.MAXIMUM -> {
                val value = constraint.params["value"] ?: 0
                "maximum: $value"
            }
            SchemaKeyword.NOT_EMPTY -> "minLength: 1"
            SchemaKeyword.ENUM_VALUES -> {
                val values = constraint.params["allowed"] ?: constraint.params["value"] ?: emptyList<Any>()
                if (values is Collection<*>) {
                    "enum:\n        " + values.joinToString("\n        ") { if (it is Number || it is Boolean) "- $it" else "- \"${escapeYaml(it.toString())}\"" }
                } else {
                    ""
                }
            }
            else -> ""
        }
    }

    private fun escapeYaml(str: String): String {
        return str.replace("\"", "\\\"").replace("\n", " ")
    }
}
