package io.dwsoft.restx

import io.mockk.MockKVerificationScope
import io.mockk.verify

inline fun <reified T> dummy(): T = mock()

inline fun <reified T> mock(stubbing: T.() -> Unit = {}): T {
    val mock: T = io.mockk.mockk(relaxed = true)
    mock.stubbing()
    return mock
}

fun verifyNotCalled(verifyBlock: MockKVerificationScope.() -> Unit) =
    verify(exactly = 0, verifyBlock = verifyBlock)