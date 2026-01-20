package dev.cphiri.hyprlang.parser

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

class HyprlangParser : AutoCloseable {

    @PublishedApi
    internal var nativeHandle: Long = 0

    companion object {
        init {
            System.loadLibrary("hyprlang_wrapper")
        }
    }

    init {
        nativeHandle = create()
    }

    enum class ConfigType {
        INT, FLOAT, STRING, VEC2
    }

    private val registeredKeys = mutableSetOf<String>()
    private val registeredSpecialCategories = mutableSetOf<String>()
    private val registeredSpecialValues = mutableSetOf<String>()

    fun addConfigValue(name: String, type: ConfigType, defaultValue: Any? = null) {
        if (registeredKeys.add(name)) {
            addConfigValue(nativeHandle, name, type.name, defaultValue)
        }
    }

    fun addSpecialCategory(name: String, key: String? = null, anonymousKeyBased: Boolean = false) {
        if (registeredSpecialCategories.add(name)) {
            addSpecialCategory(nativeHandle, name, key, anonymousKeyBased)
        }
    }

    fun addSpecialConfigValue(category: String, name: String, type: ConfigType) {
        val fullKey = "$category:$name"
        if (registeredSpecialValues.add(fullKey)) {
            addSpecialConfigValue(nativeHandle, category, name, type.name)
        }
    }

    private val registeredArrayHandlers = mutableSetOf<String>()

    fun registerArrayHandler(name: String) {
        if (registeredArrayHandlers.add(name)) {
            registerArrayHandler(nativeHandle, name)
        }
    }

    fun getArrayValues(name: String): List<String> {
        return getArrayValues(nativeHandle, name)?.toList() ?: emptyList()
    }

    fun parse(input: String): String {
        return parse(nativeHandle, input)
    }

    fun register(block: ConfigBuilder.() -> Unit) {
        val builder = ConfigBuilder(this)
        builder.block()
    }

    class ConfigBuilder(private val parser: HyprlangParser, private val prefix: String = "") {
        
        fun int(name: String, default: Int? = null) {
            registerValue(name, ConfigType.INT, default)
        }

        fun float(name: String, default: Float? = null) {
            registerValue(name, ConfigType.FLOAT, default)
        }

        fun string(name: String, default: String? = null) {
            registerValue(name, ConfigType.STRING, default)
        }
        
        fun vec2(name: String, default: Any? = null) {
            registerValue(name, ConfigType.VEC2, default)
        }

        fun category(name: String, block: ConfigBuilder.() -> Unit) {
            val newPrefix = if (prefix.isEmpty()) "$name:" else "$prefix$name:"
            val subBuilder = ConfigBuilder(parser, newPrefix)
            subBuilder.block()
        }

        fun stringArray(name: String) {
            val fullKey = "$prefix$name"
            parser.registerArrayHandler(fullKey)
        }

        private fun registerValue(name: String, type: ConfigType, default: Any?) {
            val fullKey = "$prefix$name"
            parser.addConfigValue(fullKey, type, default)
        }
    }

    class SpecialCategoryBuilder(
        private val parser: HyprlangParser,
        private val categoryName: String
    ) {
        fun int(name: String) {
            parser.addSpecialConfigValue(categoryName, name, ConfigType.INT)
        }

        fun float(name: String) {
            parser.addSpecialConfigValue(categoryName, name, ConfigType.FLOAT)
        }

        fun string(name: String) {
            parser.addSpecialConfigValue(categoryName, name, ConfigType.STRING)
        }

        fun vec2(name: String) {
            parser.addSpecialConfigValue(categoryName, name, ConfigType.VEC2)
        }
    }

    fun registerSpecialCategory(
        name: String,
        key: String? = null,
        anonymousKeyBased: Boolean = false,
        block: SpecialCategoryBuilder.() -> Unit
    ) {
        addSpecialCategory(name, key, anonymousKeyBased)
        val builder = SpecialCategoryBuilder(this, name)
        builder.block()
    }

    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun <T> convertValue(value: Any?, clazz: Class<T>): T? {
        if (value == null) return null
        return when (clazz) {
            Int::class.java, java.lang.Integer::class.java -> {
                when (value) {
                    is Int -> value as T
                    is Number -> value.toInt() as T
                    else -> null
                }
            }
            Float::class.java, java.lang.Float::class.java -> {
                when (value) {
                    is Float -> value as T
                    is Number -> value.toFloat() as T
                    else -> null
                }
            }
            Double::class.java, java.lang.Double::class.java -> {
                when (value) {
                    is Double -> value as T
                    is Number -> value.toDouble() as T
                    else -> null
                }
            }
            Long::class.java, java.lang.Long::class.java -> {
                when (value) {
                    is Long -> value as T
                    is Number -> value.toLong() as T
                    else -> null
                }
            }
            String::class.java -> value.toString() as T
            Boolean::class.java, java.lang.Boolean::class.java -> {
                when (value) {
                    is Boolean -> value as T
                    is Number -> (value.toInt() != 0) as T
                    is String -> value.equals("true", ignoreCase = true) as T
                    else -> null
                }
            }
            else -> value as? T
        }
    }

    inline fun <reified T> get(name: String): T? {
        val value = getConfigValue(nativeHandle, name) ?: return null
        return convertValue(value, T::class.java)
    }

    fun listKeysForSpecialCategory(category: String): List<String> {
        return listKeysForSpecialCategory(nativeHandle, category)?.toList() ?: emptyList()
    }

    inline fun <reified T> getSpecial(category: String, name: String, key: String? = null): T? {
        val value = getSpecialConfigValue(nativeHandle, category, name, key) ?: return null
        return convertValue(value, T::class.java)
    }

    inline fun <reified T : Any> getSpecialAs(category: String): T? {
        return parseToDataClass(T::class, category, null)
    }

    inline fun <reified T : Any> getSpecialAs(category: String, key: String): T? {
        return parseToDataClass(T::class, category, key)
    }

    inline fun <reified T : Any> getAllSpecialAs(category: String): List<T> {
        val keys = listKeysForSpecialCategory(category)
        return keys.mapNotNull { key ->
            parseToDataClass(T::class, category, key)
        }
    }

    @PublishedApi
    internal fun <T : Any> parseToDataClass(clazz: KClass<T>, category: String, key: String?): T? {
        val constructor = clazz.primaryConstructor ?: return null
        
        val args = mutableMapOf<KParameter, Any?>()
        
        for (param in constructor.parameters) {
            val customAnnotation = param.findAnnotation<HyprName>()
            val lookupName = customAnnotation?.name ?: param.name ?: continue

            val value = getSpecialConfigValue(nativeHandle, category, lookupName, key)
            
            if (value != null) {
                val convertedValue = convertValueForParameter(value, param)
                args[param] = convertedValue
            } else if (!param.isOptional) {
                return null
            }
        }
        
        return try {
            constructor.callBy(args)
        } catch (e: Exception) {
            null
        }
    }

    @PublishedApi
    internal fun convertValueForParameter(value: Any, param: KParameter): Any? {
        val classifier = param.type.classifier as? KClass<*> ?: return value
        
        return when (classifier) {
            Int::class -> when (value) {
                is Int -> value
                is Number -> value.toInt()
                else -> null
            }
            Float::class -> when (value) {
                is Float -> value
                is Number -> value.toFloat()
                else -> null
            }
            Double::class -> when (value) {
                is Double -> value
                is Number -> value.toDouble()
                else -> null
            }
            Long::class -> when (value) {
                is Long -> value
                is Number -> value.toLong()
                else -> null
            }
            String::class -> value.toString()
            Boolean::class -> when (value) {
                is Boolean -> value
                is Number -> value.toInt() != 0
                is String -> value.equals("true", ignoreCase = true)
                else -> null
            }
            else -> value
        }
    }

    override fun close() {
        if (nativeHandle != 0L) {
            destroy(nativeHandle)
            nativeHandle = 0
        }
    }

    private external fun create(): Long
    private external fun destroy(handle: Long)
    private external fun addConfigValue(handle: Long, name: String, type: String, defaultValue: Any?)
    private external fun addSpecialCategory(handle: Long, name: String, key: String?, anonymousKeyBased: Boolean)
    private external fun addSpecialConfigValue(handle: Long, category: String, name: String, type: String)
    private external fun registerArrayHandler(handle: Long, name: String)
    private external fun parse(handle: Long, input: String): String
    @PublishedApi
    internal external fun getConfigValue(handle: Long, name: String): Any?
    private external fun listKeysForSpecialCategory(handle: Long, category: String): Array<String>?
    @PublishedApi
    internal external fun getSpecialConfigValue(handle: Long, category: String, name: String, key: String?): Any?
    private external fun getArrayValues(handle: Long, name: String): Array<String>?
}
