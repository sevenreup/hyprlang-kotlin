package com.hyprlang.parser

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HyprlangParserTest {

    @Test
    fun testParseNative() {
        // Just ensuring context is available, though not strictly needed for the pure string parse
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val parser = HyprlangParser()
        
        // This input matches what we currently "support" (hardcoded keys in wrapper)
        val input = """
            general {
                border_size = 10
                gaps_in = 5
            }
        """.trimIndent()

        val result = parser.parseNative(input)
        
        // We expect empty string on success
        assertEquals("Parsing failed", "", result)
    }
}
