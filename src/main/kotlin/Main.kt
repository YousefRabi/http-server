import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import java.net.ServerSocket
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Paths

fun handleConn(clientConn: Socket, directory: String) {
    println("Inside handleConn")
    val inputStream = clientConn.getInputStream()
    val outputStream = clientConn.getOutputStream()
    val reader = inputStream.bufferedReader()
    val httpRequest = reader.parseHttpRequest()
    val method = httpRequest.method
    val url = httpRequest.url
    val headersMap = httpRequest.headers
    val body = httpRequest.body
    println("method: ${httpRequest.method}\turl: ${httpRequest.url}\theaders: ${httpRequest.headers}")
    println("body: ${httpRequest.body}")

    val httpResponse = route(method, url, headersMap, body, directory)
    println("httpResponse: ${httpResponse.toByteArray()}")

    outputStream.write(httpResponse.toByteArray())
    println("sent response")
    outputStream.close()
}

fun route(method: String, url: String, headersMap: Map<String, String>, body: String, directory: String): HttpResponse {
    val responseHeaders = mutableMapOf<String, String>()

    val httpResponse = when (method) {
        "GET" -> when {
            url == "/" -> HttpResponse(status=HttpStatus.OK)
            url.startsWith("/echo/") -> {
                val echoedStr = url.substringAfter("/echo/")
                responseHeaders["Content-Type"] = "text/plain"
                val gzipEncoded = headersMap.getOrDefault("Accept-Encoding", "").contains("gzip")
                if (gzipEncoded) {
                    responseHeaders["Content-Encoding"] = "gzip"
                } else responseHeaders["Content-Length"] = echoedStr.toByteArray().size.toString()

                println("gzipEncoded: $gzipEncoded")

                HttpResponse(
                    status=HttpStatus.OK,
                    headers=responseHeaders,
                    body=if (gzipEncoded) body.toByteArray() else ByteArray(0),)

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

fun main(args: Array<String>) {
    val parser = ArgParser("server")
    val directory by parser.option(ArgType.String, description = "The directory of content").default("/tmp/")
    parser.parse(args)

    println("serving files at directory: $directory")

    runBlocking {
        val serverSocket = ServerSocket(4221)

        // Since the tester restarts your program quite often, setting SO_REUSEADDR
        // ensures that we don't run into 'Address already in use' errors
        serverSocket.reuseAddress = true

        while (true) {
            val clientConn = serverSocket.accept()
            println("Client connected: $clientConn")

            launch(Dispatchers.IO) {
                println("Handling connection in I/O thread")
                handleConn(clientConn, directory)
            }
        }
    }
}
