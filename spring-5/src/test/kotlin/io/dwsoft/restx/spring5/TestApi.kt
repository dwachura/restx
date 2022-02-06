package io.dwsoft.restx.spring5

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
@EnableRestX
class TestApiConfig

@RestController
@RequestMapping("/users")
class UserController {
    private val users = mutableListOf<User>()

    @GetMapping("/health")
    fun health() = ResponseEntity.ok().build<Void>()

    @GetMapping
    fun list() = users.takeIf { it.isNotEmpty() } ?: throw RuntimeException("No users in DB")

    @GetMapping("/{id}")
    fun get(@PathVariable id: Int) = users[id]

    @PostMapping
    fun create(@RequestBody createUserCommand: CreateUserCommand) =
        User(users.size, createUserCommand.login)
            .also { users.add(it) }

    @DeleteMapping("/{id}")
    fun remove(@PathVariable id: Int) = users.removeAt(id).let { ResponseEntity.noContent().build<Void>() }
}

data class CreateUserCommand(val login: String)
data class User(val id: Int, val login: String)
