package com.polino.app.testcenter

import org.junit.Assert.assertTrue
import org.junit.Test

class PolinoSelfTestsTest {
    @Test
    fun selfTests_allPass() {
        val results = PolinoSelfTests.runAll()
        assertTrue("مرکز تست باید دست‌کم یک مورد داشته باشد", results.isNotEmpty())
        results.forEach { result ->
            assertTrue("${result.title}: ${result.details}", result.passed)
        }
    }

    @Test
    fun selfTests_rerunIsStable() {
        val first = PolinoSelfTests.runAll()
        val second = PolinoSelfTests.runAll()
        assertTrue(
            "اجرای دوباره باید همان نتیجه را بدهد",
            first.map { it.passed } == second.map { it.passed }
        )
    }
}
