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
    val requestLine = inputStream.bufferedReader().readLine()
    val (httpMethod, requestTarget, httpVersion) = requestLine.split(' ')
    println("httpMethod: $httpMethod\trequestTarget: $requestTarget\thttpVersion: $httpVersion")

    val response = if (requestTarget == "/") {
        "HTTP/1.1 200 OK\r\n\r\n"
    } else {
        "HTTP/1.1 404 Not Found\r\n\r\n"
    }

    val outputStream = clientConn.getOutputStream()
    outputStream.write(response.toByteArray())
    println("sent simple response")
    clientConn.close()
}
