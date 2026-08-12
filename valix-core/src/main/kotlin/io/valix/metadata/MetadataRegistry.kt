package io.valix.metadata

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Thread-safe global registry storing generated model validation metadata at runtime.
 */
object MetadataRegistry {
    private val registry = ConcurrentHashMap<String, ValixModelMetadata>()

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
        return registry[clazz.qualifiedName ?: return null]
    }

    /**
     * Returns all registered model metadata definitions.
     */
    fun getAll(): Collection<ValixModelMetadata> = registry.values
}
