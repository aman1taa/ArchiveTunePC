package com.vaulttune.potoken

data class PoTokenResult(
    val playerToken: String,
    val sessionToken: String,
    val minterGeneration: Long,
)

/** Recoverable PoToken generation failure (timeout, network, etc.). */
class PoTokenException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/** The embedded browser is fundamentally broken (e.g. JS SyntaxError). */
class BrokenBrowserException(
    message: String,
) : Exception(message)

/** Classifies a JavaScript error string into the appropriate exception type. */
fun classifyJsError(error: String): Exception =
    if (error.contains("SyntaxError")) {
        BrokenBrowserException(error)
    } else {
        PoTokenException(error)
    }
