package com.example

import com.example.exception.UnauthorisedAccessException
import com.example.repository.PostRepository
import com.example.repository.PostRepositoryInMemoryWithMutexImpl
import com.example.repository.UserRepository
import com.example.repository.UserRepositoryInMemoryWithMutexImpl
import com.example.route.RoutingV1
import com.example.service.FileService
import com.example.service.JWTTokenService
import com.example.service.PostService
import com.example.service.UserService
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import kotlinx.coroutines.runBlocking
import org.kodein.di.generic.bind
import org.kodein.di.generic.eagerSingleton
import org.kodein.di.generic.instance
import org.kodein.di.generic.with
import org.kodein.di.ktor.KodeinFeature
import org.kodein.di.ktor.kodein
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            serializeNulls()
        }
    }

    install(StatusPages) {
        exception<NotImplementedError> {
            call.respond(HttpStatusCode.NotImplemented)
        }
        exception<ParameterConversionException> {
            call.respond(HttpStatusCode.BadRequest)
        }
        exception<Throwable> {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    install(KodeinFeature) {
        constant(tag = "upload-dir") with (environment.config.propertyOrNull("example.upload.dir")?.getString()
            ?: throw UnauthorisedAccessException("Upload dir is not specified"))
        bind<PasswordEncoder>() with eagerSingleton { BCryptPasswordEncoder() }
        bind<JWTTokenService>() with eagerSingleton { JWTTokenService() }
        bind<PostRepository>() with eagerSingleton { PostRepositoryInMemoryWithMutexImpl() }
        bind<PostService>() with eagerSingleton { PostService(instance()) }
        bind<FileService>() with eagerSingleton { FileService(instance(tag = "upload-dir")) }
        bind<UserRepository>() with eagerSingleton { UserRepositoryInMemoryWithMutexImpl() }
        bind<UserService>() with eagerSingleton {
            UserService(instance(), instance(), instance()).apply {
                runBlocking {
                    this@apply.save("vasya", "password")
                }
            }
        }
        bind<RoutingV1>() with eagerSingleton {
            RoutingV1(
                instance(tag = "upload-dir"),
                instance(),
                instance(),
                instance()
            )
        }
    }

    install(Authentication) {
        jwt {
            val jwtService by kodein().instance<JWTTokenService>()
            verifier(jwtService.verifier)
            val userService by kodein().instance<UserService>()

            validate {
                challenge {defaultScheme, realm ->
                    call.request.call.respond(HttpStatusCode.Forbidden)
                }
                val id = it.payload.getClaim("id").asLong()
                userService.getModelById(id)
            }
        }
    }

    install(Routing) {
        val routingV1 by kodein().instance<RoutingV1>()
        routingV1.setup(this)
    }
}

