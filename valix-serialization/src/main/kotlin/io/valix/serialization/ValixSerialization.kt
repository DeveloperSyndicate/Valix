package io.valix.serialization

import io.valix.metadata.ValixModelMetadata
import io.valix.metadata.FieldMetadata
import io.valix.metadata.ConstraintMetadata
import kotlinx.serialization.descriptors.SerialDescriptor

fun ValixModelMetadata.toJson(): String {
    val sb = StringBuilder()
    sb.append("{\n")
    sb.append("  \"modelFqName\": \"$modelFqName\",\n")
    sb.append("  \"modelSimpleName\": \"$modelSimpleName\",\n")
    sb.append("  \"schemaVersion\": $schemaVersion,\n")
    sb.append("  \"metadataVersion\": \"$metadataVersion\",\n")
    sb.append("  \"fields\": [\n")
    for (i in fields.indices) {
        val field = fields[i]
        sb.append("    {\n")
        sb.append("      \"name\": \"${field.name}\",\n")
        sb.append("      \"type\": \"${field.type}\",\n")
        sb.append("      \"nullable\": ${field.nullable},\n")
        sb.append("      \"required\": ${field.required},\n")
        sb.append("      \"displayName\": \"${escapeJson(field.displayName)}\",\n")
        sb.append("      \"description\": \"${escapeJson(field.description)}\",\n")
        sb.append("      \"constraints\": [\n")
        for (j in field.constraints.indices) {
            val constraint = field.constraints[j]
            sb.append("        {\n")
            sb.append("          \"annotationFqName\": \"${constraint.annotationFqName}\",\n")
            sb.append("          \"constraintCode\": \"${constraint.constraintCode}\",\n")
            sb.append("          \"messageKey\": \"${constraint.messageKey}\",\n")
            sb.append("          \"defaultMessage\": \"${escapeJson(constraint.defaultMessage)}\",\n")
            sb.append("          \"params\": {")
            val paramEntries = constraint.params.entries.toList()
            for (k in paramEntries.indices) {
                val entry = paramEntries[k]
                val v = entry.value
                val valExpr = if (v is Number || v is Boolean) "$v" else "\"${escapeJson(v.toString())}\""
                sb.append("\"${entry.key}\": $valExpr")
                if (k < paramEntries.size - 1) sb.append(", ")
            }
            sb.append("},\n")
            sb.append("          \"groups\": [" + constraint.groups.joinToString(", ") { "\"$it\"" } + "],\n")
            sb.append("          \"isCustom\": ${constraint.isCustom},\n")
            sb.append("          \"schemaKeyword\": \"${constraint.schemaKeyword.name}\"\n")
            sb.append("        }")
            if (j < field.constraints.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("      ]\n")
        sb.append("    }")
        if (i < fields.size - 1) sb.append(",")
        sb.append("\n")
    }
    sb.append("  ],\n")
    sb.append("  \"classConstraints\": [\n")
    for (i in classConstraints.indices) {
        val constraint = classConstraints[i]
        sb.append("    {\n")
        sb.append("      \"annotationFqName\": \"${constraint.annotationFqName}\",\n")
        sb.append("      \"constraintCode\": \"${constraint.constraintCode}\",\n")
        sb.append("      \"messageKey\": \"${constraint.messageKey}\",\n")
        sb.append("      \"defaultMessage\": \"${escapeJson(constraint.defaultMessage)}\",\n")
        sb.append("      \"params\": {")
        val paramEntries = constraint.params.entries.toList()
        for (k in paramEntries.indices) {
            val entry = paramEntries[k]
            val v = entry.value
            val valExpr = if (v is Number || v is Boolean) "$v" else "\"${escapeJson(v.toString())}\""
            sb.append("\"${entry.key}\": $valExpr")
            if (k < paramEntries.size - 1) sb.append(", ")
        }
        sb.append("},\n")
        sb.append("      \"groups\": [" + constraint.groups.joinToString(", ") { "\"$it\"" } + "],\n")
        sb.append("      \"isCustom\": ${constraint.isCustom},\n")
        sb.append("      \"schemaKeyword\": \"${constraint.schemaKeyword.name}\"\n")
        sb.append("    }")
        if (i < classConstraints.size - 1) sb.append(",")
        sb.append("\n")
    }
    sb.append("  ],\n")
    sb.append("  \"groups\": [" + groups.joinToString(", ") { "\"$it\"" } + "]\n")
    sb.append("}")
    return sb.toString()
}

private fun escapeJson(str: String): String {
    return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
}

class EnrichedDescriptor(
    val original: SerialDescriptor,
    val metadata: ValixModelMetadata
) : SerialDescriptor by original {
    
    fun getFieldMetadata(name: String): FieldMetadata? {
        return metadata.fields.find { it.name == name }
    }
}

fun SerialDescriptor.mergeValixMetadata(metadata: ValixModelMetadata): EnrichedDescriptor {
    return EnrichedDescriptor(this, metadata)
}
