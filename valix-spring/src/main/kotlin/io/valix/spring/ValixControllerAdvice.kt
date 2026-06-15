package io.valix.spring

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ValixControllerAdvice {

    @ExceptionHandler(ValixValidationException::class)
    fun handleValixValidationException(ex: ValixValidationException): ResponseEntity<Map<String, Any>> {
        val body = mapOf(
            "status" to HttpStatus.BAD_REQUEST.value(),
            "error" to "Bad Request",
            "errors" to ex.validationResult.errors.map { error ->
                mapOf(
                    "field" to error.field,
                    "code" to error.code,
                    "message" to error.message,
                    "messageKey" to error.messageKey,
                    "rejectedValue" to error.rejectedValue,
                    "path" to error.path
                )
            }
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }
}
