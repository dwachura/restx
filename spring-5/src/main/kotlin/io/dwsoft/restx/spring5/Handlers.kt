package io.dwsoft.restx.spring5

import io.dwsoft.restx.RestX
import io.dwsoft.restx.SimpleResponseGeneratorBuilder
import io.dwsoft.restx.SimpleResponseGeneratorDsl
import io.dwsoft.restx.SingleErrorResponseGeneratorDsl
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

fun <T : Any> SingleErrorResponseGeneratorDsl<T>.representingHttpStatus(status: HttpStatus) =
    this.apply {
        withCode(status.name)
        withMessage(status.reasonPhrase)
        withStatus(status.value())
    }

@ControllerAdvice
class GlobalControllerAdvice {
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handle(ex: HttpRequestMethodNotSupportedException) = RestX.respondTo<HttpRequestMethodNotSupportedException> {
        asOperationError {
            representingHttpStatus(HttpStatus.METHOD_NOT_ALLOWED)
    } }
        .responseOf(ex)
        .let { ResponseEntity.status(it.status.code).body(it.payload) }

}
