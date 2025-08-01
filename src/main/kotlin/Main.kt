import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import java.net.ServerSocket
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.GZIPOutputStream

val supportedEncodings = setOf("gzip")

fun handleConn(clientConn: Socket, directory: String) {
    val inputStream = clientConn.getInputStream()
    val outputStream = clientConn.getOutputStream()
    val reader = inputStream.bufferedReader()

    while (true) {
        val httpRequest = reader.parseHttpRequest()

        if (httpRequest == null) break

        val method = httpRequest.method
        val url = httpRequest.url
        val headersMap = httpRequest.headers
        val body = httpRequest.body

        val httpResponse = route(method, url, headersMap, body, directory)
        val responseBytes = httpResponse.toByteArray()

        if (responseBytes.isNotEmpty()) {
            outputStream.write(httpResponse.toByteArray())
            outputStream.flush()
        }

        if (headersMap["Connection"] == "close") break
    }

    outputStream.close()
}

fun route(method: String, url: String, headersMap: Map<String, String>, body: String, directory: String): HttpResponse {
    val responseHeaders = mutableMapOf<String, String>()

    if (headersMap["Connection"] == "close") {
        responseHeaders["Connection"] = "close"
    }

    val httpResponse = when (method) {
        "GET" -> when {
            url == "/" -> HttpResponse(status=HttpStatus.OK, headers=responseHeaders)
            url.startsWith("/echo/") -> {
                val echoedStr = url.substringAfter("/echo/")
                responseHeaders["Content-Type"] = "text/plain"
                val acceptedSupportedEncodings = headersMap["Accept-Encoding"]?.split(", ")?.toSet()?.intersect(supportedEncodings) ?: emptySet()
                var responseBody: ByteArray
                if (acceptedSupportedEncodings.contains("gzip")) {
                    responseBody = gzip(echoedStr)
                    responseHeaders["Content-Encoding"] = "gzip"
                } else {
                    responseBody = echoedStr.toByteArray()
                }

                responseHeaders["Content-Length"] = responseBody.size.toString()
                HttpResponse(
                    status=HttpStatus.OK,
                    headers=responseHeaders,
                    body=responseBody,
                )

            }
            url.endsWith("/user-agent") -> {
                val userAgent = (headersMap["User-Agent"] ?: "").toByteArray()
                responseHeaders["Content-Type"] = "text/plain"
                responseHeaders["Content-Length"] = userAgent.size.toString()

                HttpResponse(
                    status=HttpStatus.OK,
                    headers=responseHeaders,
                    body=userAgent,
                )
            }
            url.startsWith("/files/") -> {
                val requestedFile = url.substringAfter("/files/")
                val path = Paths.get(directory).resolve(requestedFile)
                if (Files.exists(path)) {
                    val content = Files.readAllBytes(path)
                    responseHeaders["Content-Type"] = "application/octet-stream"
                    responseHeaders["Content-Length"] = content.size.toString()
                    HttpResponse(
                        status=HttpStatus.OK,
                        headers=responseHeaders,
                        body=content,
                    )
                } else {
                    HttpResponse(
                        status=HttpStatus.NOT_FOUND,
                    )
                }
            }
            else -> HttpResponse(status=HttpStatus.NOT_FOUND,)
        }
        "POST" -> when {
            url.startsWith("/files/") -> {
                val fileName = url.substringAfter("/files/")
                val path = Paths.get(directory).resolve(fileName)
                if (body.isEmpty()) {
                    HttpResponse(status=HttpStatus.BAD_REQUEST,)
                } else {
                    Files.write(path, body.toByteArray())
                    HttpResponse(
                        status=HttpStatus.CREATED,
                    )
                }
            }
            else -> HttpResponse(status=HttpStatus.NOT_FOUND,)
        }
        else -> {
            responseHeaders["Allow"] = "GET, POST"
            HttpResponse(
                status=HttpStatus.METHOD_NOT_ALLOWED,
                headers=responseHeaders,
            )
        }
    }

    return httpResponse
}

fun gzip(content: String): ByteArray {
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).bufferedWriter(Charsets.UTF_8).use { it.write(content) }
    return bos.toByteArray()
}

fun main(args: Array<String>) {
    val parser = ArgParser("server")
    val directory by parser.option(ArgType.String, description = "The directory of content").default("/tmp/")
    parser.parse(args)

    runBlocking {
        val serverSocket = ServerSocket(4221)

        serverSocket.reuseAddress = true

        while (true) {
            val clientConn = serverSocket.accept()

            launch(Dispatchers.IO) {
                try {
                    handleConn(clientConn, directory)
                } catch (e: Exception) {
                    println("Caught exception: ${e::class.simpleName}: ${e.message}")
                    e.printStackTrace()
                } finally {
                    clientConn.close()
                }
            }
        }
    }
}