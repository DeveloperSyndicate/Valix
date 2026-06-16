package io.valix.metadata

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

object MetadataRegistry {
    private val registry = ConcurrentHashMap<String, ValixModelMetadata>()

    fun register(metadata: ValixModelMetadata) {
        registry[metadata.modelFqName] = metadata
    }

    fun get(modelFqName: String): ValixModelMetadata? {
        return registry[modelFqName]
    }

    fun get(clazz: KClass<*>): ValixModelMetadata? {
        return registry[clazz.qualifiedName ?: return null]
    }

    fun getAll(): Collection<ValixModelMetadata> = registry.values
}
