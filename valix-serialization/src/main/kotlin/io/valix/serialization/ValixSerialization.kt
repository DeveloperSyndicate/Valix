package io.valix.serialization

import io.valix.metadata.ValixModelMetadata
import io.valix.metadata.FieldMetadata
import io.valix.metadata.ConstraintMetadata
import kotlinx.serialization.descriptors.SerialDescriptor

/**
 * Serializes a [ValixModelMetadata] descriptor into a formatted JSON string representation.
 *
 * @return JSON string encoding of the model metadata.
 */
fun ValixModelMetadata.toJson(): String = buildString {
    append("{\n")
    append("  \"modelFqName\": \"$modelFqName\",\n")
    append("  \"modelSimpleName\": \"$modelSimpleName\",\n")
    append("  \"schemaVersion\": $schemaVersion,\n")
    append("  \"metadataVersion\": \"$metadataVersion\",\n")
    append("  \"fields\": [\n")
    for (i in fields.indices) {
        val field = fields[i]
        append("    {\n")
        append("      \"name\": \"${field.name}\",\n")
        append("      \"type\": \"${field.type}\",\n")
        append("      \"nullable\": ${field.nullable},\n")
        append("      \"required\": ${field.required},\n")
        append("      \"displayName\": \"${escapeJson(field.displayName)}\",\n")
        append("      \"description\": \"${escapeJson(field.description)}\",\n")
        append("      \"constraints\": [\n")
        for (j in field.constraints.indices) {
            appendConstraintJson(field.constraints[j], "        ")
            if (j < field.constraints.size - 1) append(",")
            append("\n")
        }
        append("      ]\n")
        append("    }")
        if (i < fields.size - 1) append(",")
        append("\n")
    }
    append("  ],\n")
    append("  \"classConstraints\": [\n")
    for (i in classConstraints.indices) {
        appendConstraintJson(classConstraints[i], "    ")
        if (i < classConstraints.size - 1) append(",")
        append("\n")
    }
    append("  ],\n")
    append("  \"groups\": [" + groups.joinToString(", ") { "\"$it\"" } + "]\n")
    append("}")
}

private fun StringBuilder.appendConstraintJson(constraint: ConstraintMetadata, indent: String) {
    append("$indent{\n")
    append("$indent  \"annotationFqName\": \"${constraint.annotationFqName}\",\n")
    append("$indent  \"constraintCode\": \"${constraint.constraintCode}\",\n")
    append("$indent  \"messageKey\": \"${constraint.messageKey}\",\n")
    append("$indent  \"defaultMessage\": \"${escapeJson(constraint.defaultMessage)}\",\n")
    append("$indent  \"params\": {")
    val paramEntries = constraint.params.entries.toList()
    for (k in paramEntries.indices) {
        val entry = paramEntries[k]
        val v = entry.value
        val valExpr = if (v is Number || v is Boolean) "$v" else "\"${escapeJson(v.toString())}\""
        append("\"${entry.key}\": $valExpr")
        if (k < paramEntries.size - 1) append(", ")
    }
    append("},\n")
    append("$indent  \"groups\": [" + constraint.groups.joinToString(", ") { "\"$it\"" } + "],\n")
    append("$indent  \"isCustom\": ${constraint.isCustom},\n")
    append("$indent  \"schemaKeyword\": \"${constraint.schemaKeyword.name}\"\n")
    append("$indent}")
}

private fun escapeJson(str: String): String {
    return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
}

/**
 * Wrapper for [SerialDescriptor] attaching Valix model metadata.
 *
 * @property original Underlying kotlinx.serialization [SerialDescriptor].
 * @property metadata Associated [ValixModelMetadata].
 */
class EnrichedDescriptor(
    val original: SerialDescriptor,
    val metadata: ValixModelMetadata
) : SerialDescriptor by original {
    
    /** Returns field metadata matching property [name], or `null`. */
    fun getFieldMetadata(name: String): FieldMetadata? {
        return metadata.fields.find { it.name == name }
    }
}

/**
 * Extension wrapping a kotlinx.serialization [SerialDescriptor] with Valix [metadata].
 */
fun SerialDescriptor.mergeValixMetadata(metadata: ValixModelMetadata): EnrichedDescriptor {
    return EnrichedDescriptor(this, metadata)
}
