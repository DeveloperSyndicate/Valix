package io.valix.schema

import io.valix.metadata.ValixModelMetadata
import io.valix.metadata.FieldMetadata
import io.valix.metadata.ConstraintMetadata
import io.valix.metadata.SchemaKeyword

/**
 * Generator producing JSON Schema Draft-07 compliant specification documents from [ValixModelMetadata].
 */
object JsonSchemaGenerator {

    /**
     * Generates a Draft-07 JSON Schema specification string for the target [metadata].
     *
     * @param metadata Model validation metadata.
     * @return Formatted JSON Schema document string.
     */
    fun generate(metadata: ValixModelMetadata): String {
        return buildString {
            append("{\n")
            append("  \"\$schema\": \"http://json-schema.org/draft-07/schema#\",\n")
            append("  \"title\": \"${metadata.modelSimpleName}\",\n")
            append("  \"type\": \"object\",\n")
            append("  \"properties\": {\n")

            val fields = metadata.fields
            for (i in fields.indices) {
                val field = fields[i]
                append("    \"${field.name}\": {\n")
                
                val jsonType = when (field.type) {
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
                append("      \"type\": \"$jsonType\"")

                if (field.description.isNotEmpty()) {
                    append(",\n      \"description\": \"${escapeJson(field.description)}\"")
                }

                for (constraint in field.constraints) {
                    val mapping = mapConstraint(constraint)
                    if (mapping.isNotEmpty()) {
                        append(",\n      $mapping")
                    }
                }

                append("\n    }")
                if (i < fields.size - 1) {
                    append(",")
                }
                append("\n")
            }

            append("  }")

            val requiredFields = fields.filter { it.required }.map { it.name }
            if (requiredFields.isNotEmpty()) {
                append(",\n  \"required\": [")
                append(requiredFields.joinToString(", ") { "\"$it\"" })
                append("]")
            }

            append("\n}")
        }
    }

    private fun mapConstraint(constraint: ConstraintMetadata): String {
        return when (constraint.schemaKeyword) {
            SchemaKeyword.MIN_LENGTH -> {
                val limit = constraint.params["value"] ?: constraint.params["limit"] ?: 0
                "\"minLength\": $limit"
            }
            SchemaKeyword.MAX_LENGTH -> {
                val limit = constraint.params["value"] ?: constraint.params["limit"] ?: 0
                "\"maxLength\": $limit"
            }
            SchemaKeyword.PATTERN -> {
                val regexp = constraint.params["regexp"] ?: ""
                "\"pattern\": \"${escapeJson(regexp.toString())}\""
            }
            SchemaKeyword.FORMAT_EMAIL -> "\"format\": \"email\""
            SchemaKeyword.FORMAT_URI -> "\"format\": \"uri\""
            SchemaKeyword.MINIMUM -> {
                val value = constraint.params["value"] ?: 0
                "\"minimum\": $value"
            }
            SchemaKeyword.MAXIMUM -> {
                val value = constraint.params["value"] ?: 0
                "\"maximum\": $value"
            }
            SchemaKeyword.NOT_EMPTY -> "\"minLength\": 1"
            SchemaKeyword.ENUM_VALUES -> {
                val values = constraint.params["allowed"] ?: constraint.params["value"] ?: emptyList<Any>()
                if (values is Collection<*>) {
                    "\"enum\": [" + values.joinToString(", ") { if (it is Number || it is Boolean) "$it" else "\"${escapeJson(it.toString())}\"" } + "]"
                } else {
                    ""
                }
            }
            else -> ""
        }
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
    }
}
