package io.valix.spring

import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter

/**
 * Spring Boot auto-configuration registering Valix controllers advice, message resolver, and argument resolver post-processor.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(RequestMappingHandlerAdapter::class)
class ValixSpringAutoConfiguration(
    private val messageSource: org.springframework.context.MessageSource
) {

    init {
        io.valix.metadata.ValixConfig.messageResolver = SpringMessageResolver(messageSource)
    }

    /** Post-processor registering [ValixMethodArgumentResolver] on Spring MVC handlers. */
    @Bean
    fun valixRequestMappingHandlerAdapterPostProcessor(): BeanPostProcessor {
        return object : BeanPostProcessor {
            override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
                if (bean is RequestMappingHandlerAdapter) {
                    val resolvers = bean.argumentResolvers
                    if (resolvers != null) {
                        val newResolvers = resolvers.map { resolver ->
                            if (resolver is ValixMethodArgumentResolver) resolver
                            else ValixMethodArgumentResolver(resolver)
                        }
                        bean.argumentResolvers = newResolvers
                    }
                }
                return bean
            }
        }
    }

    /** Controller advice returning standard 400 Bad Request responses for [ValixValidationException]. */
    @Bean
    fun valixControllerAdvice(): ValixControllerAdvice {
        return ValixControllerAdvice()
    }
}
