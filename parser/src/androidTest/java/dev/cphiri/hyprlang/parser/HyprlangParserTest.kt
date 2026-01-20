package dev.cphiri.hyprlang.parser

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HyprlangParserTest {

    @Test
    fun testDSL() {
        val parser = HyprlangParser()
        
        parser.register {
            int("count")
            float("opacity")
            string("terminal")
            
            category("general") {
                int("border_size")
                category("gaps") {
                    int("inner")
                    int("outer")
                }
            }
        }

        val input = """
            count = 42
            opacity = 0.95
            terminal = kitty
            general {
                border_size = 2
                gaps {
                    inner = 5
                    outer = 10
                }
            }
        """.trimIndent()

        val result = parser.parse(input)
        assertEquals("Parse should succeed", "", result)

        assertEquals(42, parser.get<Int>("count"))
        assertEquals(0.95f, parser.get<Float>("opacity")!!, 0.001f)
        assertEquals("kitty", parser.get<String>("terminal"))
        assertEquals(2, parser.get<Int>("general:border_size"))
        assertEquals(5, parser.get<Int>("general:gaps:inner"))
        assertEquals(10, parser.get<Int>("general:gaps:outer"))
        
        parser.close()
    }

    @Test
    fun testSpecialCategoryWithKey() {
        val parser = HyprlangParser()

        parser.registerSpecialCategory("window", key = "class") {
            int("opacity")
            string("title")
        }

        val input = """
            window {
                class = firefox
                opacity = 100
                title = Browser
            }
            window {
                class = kitty
                opacity = 95
                title = Terminal
            }
        """.trimIndent()

        val result = parser.parse(input)
        assertEquals("Parse should succeed", "", result)

        val keys = parser.listKeysForSpecialCategory("window")
        assertEquals(2, keys.size)
        assertTrue(keys.contains("firefox"))
        assertTrue(keys.contains("kitty"))

        assertEquals(100, parser.getSpecial<Int>("window", "opacity", "firefox"))
        assertEquals("Browser", parser.getSpecial<String>("window", "title", "firefox"))
        assertEquals(95, parser.getSpecial<Int>("window", "opacity", "kitty"))
        assertEquals("Terminal", parser.getSpecial<String>("window", "title", "kitty"))

        parser.close()
    }

    @Test
    fun testAnonymousSpecialCategory() {
        val parser = HyprlangParser()

        parser.registerSpecialCategory("bind", anonymousKeyBased = true) {
            string("key")
            string("action")
        }

        val input = """
            bind {
                key = SUPER+Q
                action = killactive
            }
            bind {
                key = SUPER+Return
                action = exec kitty
            }
        """.trimIndent()

        val result = parser.parse(input)
        assertEquals("Parse should succeed", "", result)

        val keys = parser.listKeysForSpecialCategory("bind")
        assertEquals(2, keys.size)

        parser.close()
    }

    @Test
    fun testSpecialCategoryWithFloatValues() {
        val parser = HyprlangParser()

        parser.registerSpecialCategory("decoration", key = "name") {
            float("rounding")
            float("blur")
            int("shadow")
        }

        val input = """
            decoration {
                name = default
                rounding = 10.5
                blur = 0.8
                shadow = 1
            }
        """.trimIndent()

        val result = parser.parse(input)
        assertEquals("Parse should succeed", "", result)

        val keys = parser.listKeysForSpecialCategory("decoration")
        assertEquals(1, keys.size)
        assertEquals("default", keys[0])

        assertEquals(10.5f, parser.getSpecial<Float>("decoration", "rounding", "default")!!, 0.001f)
        assertEquals(0.8f, parser.getSpecial<Float>("decoration", "blur", "default")!!, 0.001f)
        assertEquals(1, parser.getSpecial<Int>("decoration", "shadow", "default"))

        parser.close()
    }

    @Test
    fun testArrayHandler() {
        val parser = HyprlangParser()

        parser.register {
            category("testCategory") {
                stringArray("categoryKeyword")
                int("testValueInt")
            }
        }

        val input = """
            testCategory {
                testValueInt = 123
                categoryKeyword = first value
                categoryKeyword = second value
                categoryKeyword = third value
            }
        """.trimIndent()

        val result = parser.parse(input)
        assertEquals("Parse should succeed", "", result)

        val keywords = parser.getArrayValues("testCategory:categoryKeyword")
        assertEquals(3, keywords.size)
        assertEquals("first value", keywords[0])
        assertEquals("second value", keywords[1])
        assertEquals("third value", keywords[2])

        assertEquals(123, parser.get<Int>("testCategory:testValueInt"))

        parser.close()
    }

    @Test
    fun testArrayHandlerTopLevel() {
        val parser = HyprlangParser()

        parser.register {
            stringArray("tags")
            string("name")
        }

        val input = """
            name = MyConfig
            tags = important
            tags = production
            tags = v2
        """.trimIndent()

        val result = parser.parse(input)
        assertEquals("Parse should succeed", "", result)

        val tags = parser.getArrayValues("tags")
        assertEquals(3, tags.size)
        assertEquals("important", tags[0])
        assertEquals("production", tags[1])
        assertEquals("v2", tags[2])

        assertEquals("MyConfig", parser.get<String>("name"))

        parser.close()
    }

    @Test
    fun testArrayHandlerEmpty() {
        val parser = HyprlangParser()

        parser.register {
            stringArray("items")
        }

        val input = """
            # no items defined
        """.trimIndent()

        val result = parser.parse(input)
        assertEquals("Parse should succeed", "", result)

        val items = parser.getArrayValues("items")
        assertEquals(0, items.size)

        parser.close()
    }

    data class WindowConfig(
        val opacity: Int,
        val title: String
    )

    @Test
    fun testParseToDataClass() {
        val parser = HyprlangParser()

        parser.registerSpecialCategory("window", key = "class") {
            int("opacity")
            string("title")
        }

        val input = """
            window {
                class = firefox
                opacity = 100
                title = Browser
            }
            window {
                class = kitty
                opacity = 95
                title = Terminal
            }
        """.trimIndent()

        val result = parser.parse(input)
        assertEquals("Parse should succeed", "", result)

        val firefox = parser.getSpecialAs<WindowConfig>("window", "firefox")
        assertNotNull(firefox)
        assertEquals(100, firefox!!.opacity)
        assertEquals("Browser", firefox.title)

        val kitty = parser.getSpecialAs<WindowConfig>("window", "kitty")
        assertNotNull(kitty)
        assertEquals(95, kitty!!.opacity)
        assertEquals("Terminal", kitty.title)

        parser.close()
    }

    @Test
    fun testGetAllAsDataClass() {
        val parser = HyprlangParser()

        parser.registerSpecialCategory("window", key = "class") {
            int("opacity")
            string("title")
        }

        val input = """
            window {
                class = firefox
                opacity = 100
                title = Browser
            }
            window {
                class = kitty
                opacity = 95
                title = Terminal
            }
            window {
                class = vscode
                opacity = 100
                title = Editor
            }
        """.trimIndent()

        val result = parser.parse(input)
        assertEquals("Parse should succeed", "", result)

        val windows = parser.getAllSpecialAs<WindowConfig>("window")
        assertEquals(3, windows.size)

        val titles = windows.map { it.title }.toSet()
        assertTrue(titles.contains("Browser"))
        assertTrue(titles.contains("Terminal"))
        assertTrue(titles.contains("Editor"))

        parser.close()
    }

    data class DecorationConfig(
        val rounding: Float,
        val blur: Float,
        val shadow: Int
    )

    @Test
    fun testParseToDataClassWithFloats() {
        val parser = HyprlangParser()

        parser.registerSpecialCategory("decoration", key = "name") {
            float("rounding")
            float("blur")
            int("shadow")
        }

        val input = """
            decoration {
                name = default
                rounding = 10.5
                blur = 0.8
                shadow = 1
            }
        """.trimIndent()

        val result = parser.parse(input)
        assertEquals("Parse should succeed", "", result)

        val decoration = parser.getSpecialAs<DecorationConfig>("decoration", "default")
        assertNotNull(decoration)
        assertEquals(10.5f, decoration!!.rounding, 0.001f)
        assertEquals(0.8f, decoration.blur, 0.001f)
        assertEquals(1, decoration.shadow)

        parser.close()
    }

    data class GeneralConfig(
        val border_size: Int,
        val gaps_in: Int,
        val gaps_out: Int,
        val layout: String
    )

    @Test
    fun testParseToDataClassWithoutKey() {
        val parser = HyprlangParser()

        parser.registerSpecialCategory("general") {
            int("border_size")
            int("gaps_in")
            int("gaps_out")
            string("layout")
        }

        val input = """
            general {
                border_size = 5
                gaps_in = 5
                gaps_out = 10
                layout = dwindle
            }
        """.trimIndent()

        val result = parser.parse(input)
        assertEquals("Parse should succeed", "", result)

        val generalConfig = parser.getSpecialAs<GeneralConfig>("general")
        assertNotNull(generalConfig)
        assertEquals(5, generalConfig!!.border_size)
        assertEquals(5, generalConfig.gaps_in)
        assertEquals(10, generalConfig.gaps_out)
        assertEquals("dwindle", generalConfig.layout)

        parser.close()
    }
}
