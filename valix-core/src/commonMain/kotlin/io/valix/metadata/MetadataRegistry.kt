package io.valix.metadata

import kotlin.reflect.KClass

/**
 * Global registry storing generated model validation metadata at runtime.
 */
object MetadataRegistry {
    private val registry = mutableMapOf<String, ValixModelMetadata>()

    /**
     * Registers model metadata.
     */
    fun register(metadata: ValixModelMetadata) {
        registry[metadata.modelFqName] = metadata
    }

    /**
     * Retrieves metadata by fully qualified model name.
     */
    fun get(modelFqName: String): ValixModelMetadata? {
        return registry[modelFqName]
    }

    /**
     * Retrieves metadata for a target Kotlin class.
     */
    fun get(clazz: KClass<*>): ValixModelMetadata? {
        val name = clazz.simpleName ?: return null
        return registry[name] ?: registry.values.firstOrNull { it.modelFqName.endsWith(name) }
    }

    /**
     * Returns all registered model metadata definitions.
     */
    fun getAll(): Collection<ValixModelMetadata> = registry.values
}
