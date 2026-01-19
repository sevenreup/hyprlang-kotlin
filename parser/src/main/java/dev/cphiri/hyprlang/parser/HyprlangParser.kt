package dev.cphiri.hyprlang.parser

class HyprlangParser : AutoCloseable {

    private var nativeHandle: Long = 0

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

    fun addConfigValue(name: String, type: ConfigType, defaultValue: Any? = null) {
        if (registeredKeys.add(name)) {
            addConfigValue(nativeHandle, name, type.name, defaultValue)
        }
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

        private fun registerValue(name: String, type: ConfigType, default: Any?) {
            val fullKey = "$prefix$name"
            parser.addConfigValue(fullKey, type, default)
        }
    }

    
    fun getInt(name: String): Int? {
        val value = getConfigValue(nativeHandle, name) ?: return null
        return if (value is Int) value else (value as? Number)?.toInt()
    }
    
    fun getFloat(name: String): Float? {
        val value = getConfigValue(nativeHandle, name) ?: return null
        return if (value is Float) value else (value as? Number)?.toFloat()
    }
    
    fun getString(name: String): String? {
        val value = getConfigValue(nativeHandle, name)
        return value?.toString()
    }
    
    fun getConfigValue(name: String): Any? {
        return getConfigValue(nativeHandle, name)
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
    private external fun parse(handle: Long, input: String): String
    private external fun getConfigValue(handle: Long, name: String): Any?
}

