import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import java.net.ServerSocket
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.Socket

fun handleConn(clientConn: Socket) {
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

    val response = when {
        requestTarget == "/" -> "HTTP/1.1 200 OK\r\n\r\n"
        requestTarget.startsWith("/echo/") -> {
            val echoedStr = requestTarget.substringAfter("/echo/")
            val headers = "Content-Type: text/plain\r\nContent-Length: ${echoedStr.toByteArray().size}\r\n"
            "HTTP/1.1 200 OK\r\n${headers}\r\n$echoedStr"
        }
        requestTarget.endsWith("/user-agent") -> {
            val userAgent = requestHeaders["User-Agent"] ?: ""
            val headers = "Content-Type: text/plain\r\nContent-Length: ${userAgent.toByteArray().size}\r\n"
            "HTTP/1.1 200 OK\r\n${headers}\r\n$userAgent"
        }
        else -> "HTTP/1.1 404 Not Found\r\n\r\n"
    }
    println("response: $response")

    clientConn.getOutputStream().write(response.toByteArray())
    println("sent simple response")
    clientConn.close()
}

fun main() {
    runBlocking {
        val serverSocket = ServerSocket(4221)

        // Since the tester restarts your program quite often, setting SO_REUSEADDR
        // ensures that we don't run into 'Address already in use' errors
        serverSocket.reuseAddress = true

        while (true) {
            val clientConn = serverSocket.accept()

            launch(Dispatchers.IO) {
                handleConn(clientConn)
            }
        }
    }
}
