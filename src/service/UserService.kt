package com.example.service

import com.example.dto.AuthenticationResponseDto
import com.example.dto.PasswordChangeRequestDto
import com.example.dto.RegistrationRequestDto
import com.example.dto.UserResponseDto
import com.example.exception.InvalidPasswordException
import com.example.exception.PasswordChangeException
import com.example.model.UserModel
import com.example.repository.UserRepository
import io.ktor.features.*
import org.springframework.security.crypto.password.PasswordEncoder

class UserService(
    private val repo: UserRepository,
    private val tokenService: JWTTokenService,
    private val passwordEncoder: PasswordEncoder
) {
    suspend fun getModelById(id: Long): UserModel? {
        return repo.getById(id)
    }

    suspend fun getById(id: Long): UserResponseDto {
        val model = repo.getById(id) ?: throw NotFoundException()
        return UserResponseDto.fromModel(model)
    }

    suspend fun changePassword(id: Long, input: PasswordChangeRequestDto) {
        val model = repo.getById(id) ?: throw NotFoundException()
        if (!passwordEncoder.matches(input.old, model.password)) {
            throw PasswordChangeException("Wrong password!")
        }
        val copy = model.copy(password = passwordEncoder.encode(input.new))
        repo.save(copy)
    }

    suspend fun authenticate(input: RegistrationRequestDto): AuthenticationResponseDto {
        val model = repo.getByUsername(input.username) ?: throw NotFoundException()
        if (!passwordEncoder.matches(input.password, model.password)) {
            throw InvalidPasswordException("Wrong password!")
        }

        val token = tokenService.generate(model.id)
        return AuthenticationResponseDto(token)
    }

    suspend fun save(username: String, password: String) {
        if (repo.getByUsername(username) == null) {
            repo.save(UserModel(username = username, password = passwordEncoder.encode(password)))
        }
        return
    }
}