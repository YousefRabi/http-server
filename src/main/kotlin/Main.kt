import java.net.ServerSocket

fun main() {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    println("Logs from your program will appear here!")

    // Uncomment this block to pass the first stage
    val serverSocket = ServerSocket(4221)

    // Since the tester restarts your program quite often, setting SO_REUSEADDR
    // ensures that we don't run into 'Address already in use' errors
    serverSocket.reuseAddress = true

    val clientConn = serverSocket.accept() // Wait for connection from client.
    println("accepted new connection")
    val inputStream = clientConn.getInputStream()
    val reader = inputStream.bufferedReader()
    val requestLine = reader.readLine() ?: ""
    val requestTarget = requestLine.split(" ").getOrNull(1) ?: ""
    println("requestTarget: $requestTarget")

    val response = when {
        requestTarget == "/" -> "HTTP/1.1 200 OK\r\n\r\n"
        requestTarget.startsWith("/echo/") -> {
            val echoedStr = requestTarget.substringAfter("/echo/")
            val headers = "Content-Type: text/plain\r\nContent-Length: ${echoedStr.toByteArray().size}\r\n"
            "HTTP/1.1 200 OK\r\n${headers}\r\n$echoedStr"
        }
        else -> "HTTP/1.1 404 Not Found\r\n\r\n"
    }
    println("response: $response")

    clientConn.getOutputStream().write(response.toByteArray())
    println("sent simple response")
    clientConn.close()
}
