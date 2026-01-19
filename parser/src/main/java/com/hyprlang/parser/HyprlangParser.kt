package com.hyprlang.parser

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

    fun addConfigValue(name: String, type: String, defaultValue: Any? = null) {
        addConfigValue(nativeHandle, name, type, defaultValue)
    }

    fun parse(input: String): String {
        return parse(nativeHandle, input)
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
        return getConfigValue(nativeHandle, name) as? String
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
    
    // Deprecated old method
    fun parseNative(input: String): String {
        // Fallback or just re-implement using new methods if possible, 
        // but for now let's keep it unsupported or remove it since we are refactoring.
        // Or create a temporary instance.
        val instance = HyprlangParser()
        instance.addConfigValue("general:border_size", "INT", 0)
        instance.addConfigValue("general:gaps_in", "INT", 0)
        instance.addConfigValue("general:gaps_out", "INT", 0)
        val result = instance.parse(input)
        instance.close()
        return result
    }

    private external fun create(): Long
    private external fun destroy(handle: Long)
    private external fun addConfigValue(handle: Long, name: String, type: String, defaultValue: Any?)
    private external fun parse(handle: Long, input: String): String
    private external fun getConfigValue(handle: Long, name: String): Any?
}
