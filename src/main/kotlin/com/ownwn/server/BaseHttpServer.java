package com.ownwn.server;

import com.ownwn.server.java.lang.replacement.*;
import com.ownwn.server.java.lang.replacement.function.Function;
import com.ownwn.server.java.lang.replacement.stream.OutputStream;
import com.ownwn.server.request.DeleteRequest;
import com.ownwn.server.request.GetRequest;
import com.ownwn.server.request.PostRequest;
import com.ownwn.server.response.Response;
import com.ownwn.server.response.ResponseWriter;
import com.ownwn.server.response.WholeBodyResponse;
import com.ownwn.server.java.lang.replacement.stream.InputStream;
import com.ownwn.server.request.Request;
import com.ownwn.server.sockets.Client;
import com.ownwn.server.sockets.SocketServer;

import java.lang.foreign.*;
import java.net.SocketTimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BaseHttpServer {
    private static final Pattern getRequestParamPattern = Pattern.compile("[?&]([^?=&]+)=([^?=&]+)");
    private final SocketServer socket;

    public String getAddress() {
        return socket.getHostAddress().ip();
    }

    public static BaseHttpServer create(short port, Function<Request, Response> handler) throws Throwable {
        return new BaseHttpServer(port, handler);
    }


    private BaseHttpServer(short port, Function<Request, Response> handler) throws Throwable {
        Arena arena = Arena.ofShared();
        socket = new SocketServer(port, arena);

        Thread baseServerHandlerThread = new Thread(() -> {
            try (arena) {
                while (true) {
                    Client client;
                    try {
                        client = socket.accept();
                    } catch (SocketTimeoutException socketTimeoutException) {
                        continue;
                    }

                    Thread clientRequestHandlerThread = new Thread(() -> {
                        try (Arena clientArena = Arena.ofConfined()) {
                            processClient(client, clientArena, handler);
                        }
                    });
                    clientRequestHandlerThread.start();
                }
            } catch (Throwable e) {
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
            }
        });

        baseServerHandlerThread.start();

    }

    // this is the only function that closes the socket
    static void processClient(Client client, Arena arena, Function<Request, Response> handler) {
        OutputStream output = client.getOutputStream(arena);
        ResponseWriter writer = new ResponseWriter(output);
        try {
            Request request = createRequest(client, arena, output);
            writer = request.responseWriter();
            Response response = handler.apply(request);
            if (!writer.hasStarted()) writer.send(response);
        } catch (Exception e) {
            if (e instanceof HttpRequestException rejection) {
                System.err.println("HTTP " + rejection.status() + ": " + rejection.getMessage());
            } else {
                e.printStackTrace();
            }
            if (!writer.hasStarted()) {
                int status = e instanceof HttpRequestException rejection ? rejection.status() : 500;
                String message = e instanceof HttpRequestException ? e.getMessage() : "Internal Server Error";
                try {
                    writer.send(WholeBodyResponse.of(status, message));
                } catch (Exception writeError) {
                    writeError.printStackTrace();
                }
            }
        } finally {
            try { output.close(); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private static Request createRequest(Client client, Arena arena, OutputStream output) throws IOException {
        InputStream input = client.getInputStream(arena);
        List<String> rawHeaders = getRawHeaders(input);
        if (rawHeaders.isEmpty()) throw new HttpRequestException(400, "Missing HTTP request line");
        String[] parts = rawHeaders.removeFirst().split(" ", -1);
        if (parts.length != 3 || parts[1].isEmpty()) {
            throw new HttpRequestException(400, "Malformed HTTP request line");
        }
        HttpMethod method = HttpMethod.fromString(parts[0]);
        if (method == null) throw new HttpRequestException(400, "Invalid HTTP method");
        if (!parts[2].equals("HTTP/1.1") && !parts[2].equals("HTTP/1.0")) {
            throw new HttpRequestException(505, "HTTP version not supported");
        }
        Headers headers = Headers.fromRawList(rawHeaders);
        Map<String, String> cookies = headers.getAndRemoveCookies();
        StringBuilder path = new StringBuilder(parts[1]);
        Map<String, String> params = getGetRequestParamsAndMutatePath(path);
        if (method == HttpMethod.POST) {
            return new PostRequest(client.getInetAddress(), input, headers, output, path.toString(), cookies, params);
        } else if (method == HttpMethod.DELETE) {
            return new DeleteRequest(client.getInetAddress(), input, headers, output, path.toString(), cookies, params);
        } else if (method == HttpMethod.GET) {
            return new GetRequest(client.getInetAddress(), input, headers, output, path.toString(), cookies, params);
        } else {
            throw new HttpRequestException(405, "Unknown Method Type");
        }
    }

    /** Mutates {@code path} */
    private static Map<String, String> getGetRequestParamsAndMutatePath(StringBuilder path) {
        if (path.isEmpty() || path.toString().equals("/")) return Map.of();
        Map<String, String> params = new HashMap<>();

        int endOfPath = -1;
        Matcher m = getRequestParamPattern.matcher(path);
        while (m.find()) {
            if (endOfPath == -1) endOfPath = m.start();
            params.put(m.group(1), m.group(2));
        }
        if (endOfPath != -1) {
            path.delete(endOfPath, path.length());
        }
        return params;
    }

    private static List<String> getRawHeaders(InputStream input) throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        int total = 0;
        while (true) {
            int value = input.read();
            if (value == -1) throw new HttpRequestException(400, "Incomplete HTTP headers");
            if (++total > 16384) throw new HttpRequestException(431, "HTTP headers exceed 16 KB");
            if (value == '\r') {
                if (input.read() != '\n') throw new HttpRequestException(400, "Expected CRLF in HTTP headers");
                if (++total > 16384) throw new HttpRequestException(431, "HTTP headers exceed 16 KB");
                if (line.isEmpty()) return lines;
                lines.add(line.toString());
                line.setLength(0);
            } else {
                if (value == '\n') throw new HttpRequestException(400, "Expected CRLF in HTTP headers");
                line.append((char) value);
            }
        }
    }
}
