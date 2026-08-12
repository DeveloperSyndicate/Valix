package io.valix.metadata

/**
 * Interface holding static metadata extracted for a validated domain model class.
 */
interface ValixModelMetadata {
    /** Fully qualified class name of the domain model. */
    val modelFqName: String

    /** Simple class name of the domain model. */
    val modelSimpleName: String

    /** Schema version identifier. */
    val schemaVersion: Int

    /** Valix metadata specification version string. */
    val metadataVersion: String

    /** List of field metadata definitions. */
    val fields: List<FieldMetadata>

    /** List of class-level constraint definitions. */
    val classConstraints: List<ConstraintMetadata>

    /** List of validation group names. */
    val groups: List<String>
}

/**
 * Default implementation of [ValixModelMetadata].
 */
data class DefaultValixModelMetadata(
    override val modelFqName: String,
    override val modelSimpleName: String,
    override val schemaVersion: Int,
    override val metadataVersion: String,
    override val fields: List<FieldMetadata>,
    override val classConstraints: List<ConstraintMetadata> = emptyList(),
    override val groups: List<String> = emptyList()
) : ValixModelMetadata

/**
 * Metadata descriptor for a single property or field.
 */
data class FieldMetadata(
    val name: String,
    val type: String,
    val nullable: Boolean,
    val required: Boolean,
    val constraints: List<ConstraintMetadata>,
    val displayName: String = name,
    val description: String = ""
)

/**
 * Metadata descriptor for a single constraint rule attached to a field or class.
 */
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

/**
 * Mapping keyword used for OpenAPI and JSON Schema exports.
 */
enum class SchemaKeyword {
    MIN_LENGTH, MAX_LENGTH, PATTERN, FORMAT_EMAIL, FORMAT_DATE,
    FORMAT_URI, MINIMUM, MAXIMUM, ENUM_VALUES, NOT_EMPTY, REQUIRED,
    CUSTOM, NONE
}
