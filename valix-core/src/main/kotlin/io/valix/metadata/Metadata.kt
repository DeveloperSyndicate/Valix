package io.valix.metadata

interface ValixModelMetadata {
    val modelFqName: String
    val modelSimpleName: String
    val schemaVersion: Int
    val metadataVersion: String
    val fields: List<FieldMetadata>
    val classConstraints: List<ConstraintMetadata>
    val groups: List<String>
}

data class DefaultValixModelMetadata(
    override val modelFqName: String,
    override val modelSimpleName: String,
    override val schemaVersion: Int,
    override val metadataVersion: String,
    override val fields: List<FieldMetadata>,
    override val classConstraints: List<ConstraintMetadata> = emptyList(),
    override val groups: List<String> = emptyList()
) : ValixModelMetadata

data class FieldMetadata(
    val name: String,
    val type: String,
    val nullable: Boolean,
    val required: Boolean,
    val constraints: List<ConstraintMetadata>,
    val displayName: String = name,
    val description: String = ""
)

data class ConstraintMetadata(
    val annotationFqName: String,
    val constraintCode: String,
    val messageKey: String,
    val defaultMessage: String,
    val params: Map<String, Any>,
    val groups: List<String>,
    val isCustom: Boolean,
    val schemaKeyword: SchemaKeyword
)

enum class SchemaKeyword {
    MIN_LENGTH, MAX_LENGTH, PATTERN, FORMAT_EMAIL, FORMAT_DATE,
    FORMAT_URI, MINIMUM, MAXIMUM, ENUM_VALUES, NOT_EMPTY, REQUIRED,
    CUSTOM, NONE
}
