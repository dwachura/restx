package io.dwsoft.restx.spring5

import io.kotest.core.extensions.Extension
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.contain
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@WebMvcTest(UserController::class)
@ContextConfiguration(classes = [TestApiConfig::class])
class Tests : FunSpec() {
    override fun extensions(): List<Extension> = listOf(SpringExtension)
    override fun testCaseOrder(): TestCaseOrder = TestCaseOrder.Sequential

    @Autowired private lateinit var controller: UserController
    @Autowired private lateinit var mockMvc: MockMvc

    init {
        test("test spring app boots correctly") {
            mockMvc.get("/users/health").andExpect { status { isOk() } }
        }

        test("method not supported exception handled") {
            mockMvc.post("/users/health")
                .andDo { print() }
                .andExpect {
                    status { isMethodNotAllowed() }
                    content {
                        with(HttpStatus.METHOD_NOT_ALLOWED) {
                            jsonPath("$.code") { value(name) }
                            jsonPath("$.message") { value(reasonPhrase) }
                        }
                    }
                }
        }
    }
}
