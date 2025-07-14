# HTTP Server in Kotlin

A lightweight HTTP server implementation built from scratch in Kotlin, featuring concurrent request handling and file operations.

## Features

- **HTTP/1.1 Protocol Support**: Handles GET and POST requests
- **Concurrent Request Handling**: Uses Kotlin coroutines for handling multiple clients
- **GZIP Compression**: Supports gzip encoding for response compression
- **File Operations**: Read and write files through HTTP endpoints
- **Persistent Connections**: Supports keep-alive connections

## Supported Endpoints

### GET Endpoints
- `GET /` - Returns 200 OK
- `GET /echo/{message}` - Echoes back the message with optional gzip compression
- `GET /user-agent` - Returns the client's User-Agent header
- `GET /files/{filename}` - Serves files from the configured directory

### POST Endpoints
- `POST /files/{filename}` - Saves request body to a file in the configured directory

## Quick Start

### Prerequisites
- Java 8 or higher
- Maven

### Building the Project
```bash
mvn compile
mvn package
```

### Running the Server
```bash
java -jar target/build-your-own-http-server.jar --directory /path/to/files
```

The server runs on port 4221 by default.

### Usage Examples

```bash
# Basic health check
curl http://localhost:4221/

# Echo a message
curl http://localhost:4221/echo/hello

# Echo with gzip compression
curl -H "Accept-Encoding: gzip" http://localhost:4221/echo/hello

# Get user agent
curl http://localhost:4221/user-agent

# Upload a file
echo "Hello World" | curl -X POST http://localhost:4221/files/test.txt --data-binary @-

# Download a file
curl http://localhost:4221/files/test.txt
```

## Architecture

- **Main.kt**: Server initialization and connection handling
- **HttpRequest.kt**: HTTP request parsing and data structures
- **HttpResponse.kt**: HTTP response formatting and status codes
- **Routing**: URL-based request routing with method support

### Key Components

- **Coroutines**: Each client connection is handled in a separate coroutine for concurrency
- **Socket Programming**: Raw socket handling for low-level HTTP protocol implementation
- **HTTP Parsing**: Custom HTTP request parser that handles headers and body
- **File I/O**: Direct file system operations for file serving and storage

## Technical Details

- **Language**: Kotlin
- **Concurrency**: Kotlinx Coroutines
- **Build Tool**: Maven
- **JVM Target**: Java 8
- **Dependencies**: 
  - Kotlin Standard Library
  - Kotlinx Coroutines
  - Kotlinx CLI