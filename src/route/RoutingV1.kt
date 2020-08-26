package com.example.route

import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.features.ParameterConversionException
import io.ktor.http.content.*
import io.ktor.request.receive
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.routing.*
import com.example.dto.AuthenticationRequestDto
import com.example.dto.PostRequestDto
import com.example.dto.UserResponseDto
import com.example.model.UserModel
import com.example.service.FileService
import com.example.service.PostService
import com.example.service.UserService

class RoutingV1(
    private val staticPath: String,
    private val postService: PostService,
    private val fileService: FileService,
    private val userService: UserService
) {
    fun setup(configuration: Routing) {
        with(configuration) {
            route("/api/v1/") {
                static("/static") {
                    files(staticPath)
                }

                route("/") {
                    post("/registration") {
                        TODO()
                    }

                    post("/authentication") {
                        val input = call.receive<AuthenticationRequestDto>()
                        val response = userService.authenticate(input)
                        call.respond(response)
                    }
                }

                authenticate {
                    route("/me") {
                        get {
                            val me = call.authentication.principal<UserModel>()
                            call.respond(UserResponseDto.fromModel(me!!))
                        }
                    }

                    route("/posts") {
                        get {
                            val response = postService.getAll()
                            call.respond(response)
                        }
                        get("/{id}") {
                            val id = call.parameters["id"]?.toLongOrNull() ?: throw ParameterConversionException(
                                "id",
                                "Long"
                            )
                            val response = postService.getById(id)
                            call.respond(response)
                        }
                        post {
                            val input = call.receive<PostRequestDto>()
                            val response = postService.save(input)
                            call.respond(response)
                        }
                        delete("/{id}") {
                            val me = call.authentication.principal<UserModel>()!!
                            // в me - информация о текущем пользователе
                            TODO()
                        }
                    }
                }

                route("/media") {
                    post {
                        val multipart = call.receiveMultipart()
                        val response = fileService.save(multipart)
                        call.respond(response)
                    }
                }
            }
        }
    }
}