package org.dthv.linkforge.app.handler

import jakarta.servlet.http.HttpServletRequest
import org.dthv.linkforge.app.exceptions.LinkStorageException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(LinkStorageException::class, DataAccessException::class)
    fun handleStorageUnavailable(e: LinkStorageException, request: HttpServletRequest): ProblemDetail {
        log.error("Storage failure on ${request.requestURI}", e)
        return problem(HttpStatus.SERVICE_UNAVAILABLE, e, request, e.message)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException, request: HttpServletRequest): ProblemDetail {
        val errors = e.bindingResult?.fieldErrors?.associate { it.field to (it.defaultMessage ?: "invalid") }
        return problem(HttpStatus.BAD_REQUEST, e, request, "Validation failed").apply {
            setProperty("errors", errors)
        }
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception, request: HttpServletRequest): ProblemDetail {
        log.error("Unhandled exception on ${request.requestURI}", e)
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, e, request, "An unexpected error occurred")
    }

    private fun problem(
        status: HttpStatus,
        e: Exception,
        request: HttpServletRequest,
        detail: String? = null,
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail ?: e.message ?: status.reasonPhrase).apply {
            setProperty("path", request.requestURI)
        }
}
