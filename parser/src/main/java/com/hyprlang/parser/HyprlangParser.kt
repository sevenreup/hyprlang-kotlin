package com.hyprlang.parser

class HyprlangParser {

    companion object {
        init {
            System.loadLibrary("hyprlang_wrapper")
        }
    }

    /**
     * Parses the given input string using the native hyprlang library.
     * 
     * @param input The configuration string to parse.
     * @return An empty string if successful, or an error message if parsing failed.
     */
    external fun parseNative(input: String): String
}
