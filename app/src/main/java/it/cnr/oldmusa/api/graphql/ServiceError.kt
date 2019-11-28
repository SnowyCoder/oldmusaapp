package it.cnr.oldmusa.api.graphql

import com.apollographql.apollo.api.Error
import java.lang.RuntimeException


open class ServiceGraphQlException(operationName: String, message: String)
    : RuntimeException("$message on $operationName") {

    companion object {
        fun fromErrors(operationName: String, errors: List<Error>): ServiceGraphQlException {
            if (errors.size != 1) {
                return ServiceGraphQlException(
                    operationName,
                    errors.joinToString(", ") { it.message() ?: "Unknown" }
                )
            }
            val error = errors[0]

            return error.customAttributes()["type"]?.let {
                when (it) {
                    "INTERNAL_SERVER_ERROR" -> InternalServerErrorGraphQlException(operationName, error.customAttributes()["info"]?.toString() ?: "Unknown")
                    "BAD_REQUEST" -> BadRequestGraphQlException(operationName, error.message() ?: "Unknown")
                    "NOT_FOUND" -> NotFoundGraphQlException(operationName, error.message() ?: "Unknown")
                    "UNAUTHORIZED" -> UnauthorizedraphQlException(operationName)
                    "WRONG_PASSWORD" -> WrongPasswordGraphQlException(operationName)
                    "LOGIN_REQUIRED" -> LoginRequiredGraphQlException(operationName)
                    "ALREADY_PRESENT" -> AlreadyPresentGraphQlException(operationName, error.message() ?: "Unknown")
                    else -> null
                }
            } ?: ServiceGraphQlException(operationName, error.message() ?: "Unknown")
        }

        fun fromHttpCode(operationName: String, code: Int, messageBody: String?): ServiceGraphQlException {
            return when {
                code / 100 == 5 -> InternalServerErrorGraphQlException(operationName, messageBody ?: "Unknown")
                code == 400 -> BadRequestGraphQlException(operationName, messageBody ?: "Unknown")
                code == 401 -> LoginRequiredGraphQlException(operationName)
                code == 403 -> UnauthorizedraphQlException(operationName)
                code == 404 -> NotFoundGraphQlException(operationName, messageBody ?: "Unknown")
                else -> ServiceGraphQlException(operationName, messageBody ?: "Unknonwn")
            }
        }
    }
}

class InternalServerErrorGraphQlException(operationName: String, message: String)
    : ServiceGraphQlException(operationName, message)

class BadRequestGraphQlException(operationName: String, message: String)
    : ServiceGraphQlException(operationName, message)

class NotFoundGraphQlException(operationName: String, message: String)
    : ServiceGraphQlException(operationName, message)

class UnauthorizedraphQlException(operationName: String)
    : ServiceGraphQlException(operationName, "Unauthorized")

class WrongPasswordGraphQlException(operationName: String)
    : ServiceGraphQlException(operationName, "Wrong Password")

class LoginRequiredGraphQlException(operationName: String)
    : ServiceGraphQlException(operationName, "Login Required")

class AlreadyPresentGraphQlException(message: String, operationName: String)
    : ServiceGraphQlException(operationName, message)
