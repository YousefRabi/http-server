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
    val headers = httpRequest.headers
    val body = httpRequest.body
    println("method: ${httpRequest.method}\turl: ${httpRequest.url}\theaders: ${httpRequest.headers}")
    println("body: ${httpRequest.body}")

    val responseBytes = when (method) {
        "GET" -> when {
            url == "/" -> "HTTP/1.1 200 OK\r\n\r\n".toByteArray()
            url.startsWith("/echo/") -> {
                val echoedStr = url.substringAfter("/echo/")
                val headers = "Content-Type: text/plain\r\nContent-Length: ${echoedStr.toByteArray().size}\r\n"
                "HTTP/1.1 200 OK\r\n${headers}\r\n$echoedStr".toByteArray()
            }
            url.endsWith("/user-agent") -> {
                val userAgent = headers["User-Agent"] ?: ""
                val headers = "Content-Type: text/plain\r\nContent-Length: ${userAgent.toByteArray().size}\r\n"
                "HTTP/1.1 200 OK\r\n${headers}\r\n$userAgent".toByteArray()
            }
            url.startsWith("/files/") -> {
                val requestedFile = url.substringAfter("/files/")
                val path = Paths.get(directory).resolve(requestedFile)
                if (Files.exists(path)) {
                    val content = Files.readAllBytes(path)
                    val headers = "Content-Type: application/octet-stream\r\nContent-Length: ${content.size}\r\n"
                    "HTTP/1.1 200 OK\r\n${headers}\r\n".toByteArray() + content
                } else {
                    "HTTP/1.1 404 Not Found\r\n\r\n".toByteArray()
                }
            }
            else -> "HTTP/1.1 404 Not Found\r\n\r\n".toByteArray()
        }
        "POST" -> when {
            url.startsWith("/files/") -> {
                val fileName = url.substringAfter("/files/")
                val path = Paths.get(directory).resolve(fileName)
                if (body == null) {
                    "HTTP/1.1 400 Bad Request\r\n\r\n".toByteArray()
                } else {
                    Files.write(path, body.toByteArray())
                    "HTTP/1.1 201 Created\r\n\r\n".toByteArray()
                }
            }
            else -> "HTTP/1.1 404 Not Found\r\n\r\n".toByteArray()
        }
        else -> "HTTP/1.1 405 Method Not Allowed\r\nAllow: GET, POST\r\n\r\n".toByteArray()
    }

    outputStream.write(responseBytes)
    println("sent response")
    outputStream.close()
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
