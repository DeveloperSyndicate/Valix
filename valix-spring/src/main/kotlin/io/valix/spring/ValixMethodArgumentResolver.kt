package io.valix.spring

import io.valix.localization.resolveMessages
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class ValixMethodArgumentResolver(
    private val delegate: HandlerMethodArgumentResolver
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(ValidValix::class.java) && delegate.supportsParameter(parameter)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Any? {
        val arg = delegate.resolveArgument(parameter, mavContainer, webRequest, binderFactory) ?: return null
        val validValix = parameter.getParameterAnnotation(ValidValix::class.java) ?: return arg
        val groups = validValix.groups
        
        val result = ValixFrameworkValidator.validate(arg, *groups)
        if (!result.valid) {
            val resolvedResult = result.resolveMessages()
            throw ValixValidationException(resolvedResult)
        }
        return arg
    }
}
