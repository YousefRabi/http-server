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
    val reader = inputStream.bufferedReader()
    val requestLine = reader.readLine() ?: ""
    val requestTarget = requestLine.split(" ").getOrNull(1) ?: ""
    println("requestTarget: $requestTarget")

    val requestHeaders = reader.lineSequence()
        .takeWhile { it.isNotEmpty() }
        .map { it.split(": ", limit = 2) }
        .filter { it.size == 2 }
        .associate { it[0] to it[1] }

    println("requestHeaders: $requestHeaders")

    val responseBytes = when {
        requestTarget == "/" -> "HTTP/1.1 200 OK\r\n\r\n".toByteArray()
        requestTarget.startsWith("/echo/") -> {
            val echoedStr = requestTarget.substringAfter("/echo/")
            val headers = "Content-Type: text/plain\r\nContent-Length: ${echoedStr.toByteArray().size}\r\n"
            "HTTP/1.1 200 OK\r\n${headers}\r\n$echoedStr".toByteArray()
        }
        requestTarget.endsWith("/user-agent") -> {
            val userAgent = requestHeaders["User-Agent"] ?: ""
            val headers = "Content-Type: text/plain\r\nContent-Length: ${userAgent.toByteArray().size}\r\n"
            "HTTP/1.1 200 OK\r\n${headers}\r\n$userAgent".toByteArray()
        }
        requestTarget.startsWith("/files/") -> {
            val requestedFile = requestTarget.substringAfter("/files/")
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
    println("responseBytes: $responseBytes")

    clientConn.getOutputStream().write(responseBytes)
    println("sent simple response")
    clientConn.close()
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
