package dev.cphiri.hyprlang.parser

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HyprlangParserTest {

    @Test
    fun testBasicValues() {
        // ### Basic Values
        /*
        count = 42
        opacity = 0.95
        terminal = kitty
        */
        val parser = HyprlangParser()
        parser.addConfigValue("count", "INT", 0)
        parser.addConfigValue("opacity", "FLOAT", 0.0)
        parser.addConfigValue("terminal", "STRING", "")

        val input = """
            count = 42
            opacity = 0.95
            terminal = kitty
        """.trimIndent()

        val result = parser.parse(input)
        assertEquals("Parse should succeed", "", result)

        assertEquals(42, parser.getInt("count"))
        assertEquals(0.95f, parser.getFloat("opacity")!!, 0.001f)
        assertEquals("kitty", parser.getString("terminal"))
        
        parser.close()
    }
    
    @Test
    fun testNestedCategories() {
        // ### Nested Categories
        /*
        general {
            border_size = 2
            gaps {
                inner = 5
                outer = 10
            }
        }
        */
        val parser = HyprlangParser()
        parser.addConfigValue("general:border_size", "INT")
        parser.addConfigValue("general:gaps:inner", "INT")
        parser.addConfigValue("general:gaps:outer", "INT")

        val input = """
            general {
                border_size = 2
                gaps {
                    inner = 5
                    outer = 10
                }
            }
        """.trimIndent()

        val result = parser.parse(input)
        assertEquals("", result)

        assertEquals(2, parser.getInt("general:border_size"))
        assertEquals(5, parser.getInt("general:gaps:inner"))
        assertEquals(10, parser.getInt("general:gaps:outer"))
        
        parser.close()
    }

    @Test
    fun testVariables() {
        // ### Variables
        /*
        $terminal = kitty
        my_term = $terminal
        */
        val parser = HyprlangParser()
        parser.addConfigValue("my_term", "STRING")

        val input = """
            ${'$'}terminal = kitty
            my_term = ${'$'}terminal
        """.trimIndent()

        val result = parser.parse(input)
        assertEquals("", result)

        assertEquals("kitty", parser.getString("my_term"))
        
        parser.close()
    }
}
