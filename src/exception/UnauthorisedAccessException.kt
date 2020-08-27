package com.example.exception

import java.lang.RuntimeException

class UnauthorisedAccessException(message: String): RuntimeException(message) {
}