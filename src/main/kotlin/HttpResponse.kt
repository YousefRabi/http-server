enum class HttpStatus(val code: Int, val msg: String) {
    OK(200, "OK"),
    CREATED(201, "Created"),
    BAD_REQUEST(400, "Bad Request"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(404, "Method Not Allowed"),
}

class HttpResponse(
    val status: HttpStatus,
    val headers: Map<String, String> = emptyMap(),
    val body: ByteArray = byteArrayOf(),
) {
    fun toByteArray(): ByteArray {
        var outString = "HTTP/1.1 ${status.code} ${status.msg}\r\n"
        if (headers.isNotEmpty()) {
            outString += headers.map { "${it.key}: ${it.value}" }.joinToString("\r\n", postfix = "\r\n")
        }
        outString += "\r\n"

        var out = outString.toByteArray()

        if (body.isNotEmpty()) {
            out += body
        }

        return out
    }
}