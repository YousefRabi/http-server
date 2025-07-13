import java.io.BufferedReader

data class HttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap<String, String>(),
    val body: String
)

fun BufferedReader.parseHttpRequest(): HttpRequest {
    val requestLine = readLine() ?: ""
    val (method, url, _) = requestLine.split(" ")

    val headers = lineSequence()
        .takeWhile { it.isNotEmpty() }
        .map { it.split(": ", limit = 2) }
        .filter { it.size == 2 }
        .associate { it[0] to it[1] }

    val body = if (method == "POST") {
        val contentLength = headers["Content-Length"]?.toIntOrNull() ?: 0
        if (contentLength > 0) {
            val charArray = CharArray(contentLength)
            read(charArray, 0, contentLength)
            String(charArray)
        } else ""
    } else ""

    return HttpRequest(method, url, headers, body)
}

