package com.ownwn.server;

import com.ownwn.server.java.lang.replacement.*;
import com.ownwn.server.java.lang.replacement.stream.InputStream;
import com.ownwn.server.request.GetRequest;
import com.ownwn.server.request.PostRequest;
import com.ownwn.server.request.Request;
import com.ownwn.server.sockets.Client;
import com.ownwn.server.sockets.SocketServer;

import java.io.IOException;
import java.lang.foreign.*;
import java.net.SocketTimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BaseHttpServer {
    private final Pattern getRequestParamPattern = Pattern.compile("[?&]([^?=&]+)=([^?=&]+)"); // todo single responsibility principal?
    private final SocketServer socket;

    public String getAddress() {
        return socket.getHostInetAddress().getHostAddress();
    }

    public static BaseHttpServer create(short port, Consumer<Request> handler) throws Throwable {
        return new BaseHttpServer(port, handler);
    }


    private BaseHttpServer(short port, Consumer<Request> handler) throws Throwable {
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
                            Request request = createRequest(client, clientArena);
                            handler.accept(request);
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

    private Request createRequest(Client client, Arena arena) {
        try {
            InputStream s = client.getInputStream(arena);

            List<String> rawHeaders = getRawHeaders(s);
            if (rawHeaders.isEmpty()) {
                throw new RuntimeException("Bad HTTP request!"); // todo handle better
            }
            String requestMethod = rawHeaders.removeFirst();
            String[] requestParts = requestMethod.split(" ");
            HttpMethod method = HttpMethod.fromString(requestParts[0]);
            if (!requestMethod.endsWith("HTTP/1.1") || requestParts.length != 3 || method == null) {
                // todo malformed response
                return null;
            }

            String requestPath = requestParts[1];
            Headers headers = Headers.fromRawList(rawHeaders);
            Map<String, String> cookies = headers.getAndRemoveCookies();
            StringBuilder newPath = new StringBuilder(requestPath);
            Map<String, String> requestParams = getGetRequestParamsAndMutatePath(newPath);
            requestPath = newPath.toString();



            if (method == HttpMethod.GET) {
                return new GetRequest(client.getInetAddress(), s, headers, client.getOutputStream(arena), requestPath, cookies, requestParams);
            } else if (method == HttpMethod.POST) {
                return new PostRequest(client.getInetAddress(), s, headers, client.getOutputStream(arena), requestPath, cookies, requestParams);
            } else {
                return new GetRequest(client.getInetAddress(), s, headers, client.getOutputStream(arena), requestPath, cookies, requestParams);
                // todo
            }



        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Mutates {@code path} */
    private Map<String, String> getGetRequestParamsAndMutatePath(StringBuilder path) {
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

    private List<String> getRawHeaders(InputStream inputStream) throws IOException {
        List<String> headers = new ArrayList<>();
        LineState state = LineState.NONE;
        int b;
        StringBuilder currentHeaderRaw = new StringBuilder(32);

        while ((b = inputStream.read()) != -1) { // todo limit header length to reasonable strlen?
            if (b == 13) {
                if (state == LineState.NONE) state = LineState.SEEN_CR1;
                else if (state == LineState.SEEN_LF1) state = LineState.SEEN_CR2;
            } else if (b == 10) {
                if (state == LineState.SEEN_CR1) {
                    headers.add(currentHeaderRaw.substring(0, currentHeaderRaw.length()-1));
                    currentHeaderRaw = new StringBuilder(32);
                    state = LineState.SEEN_LF1;
                    continue;
                } else if (state == LineState.SEEN_CR2) {
                    return headers;
                }
            } else {
                state = LineState.NONE;
            }
            currentHeaderRaw.append((char) b);
        }


        return headers;
    }

    private String getRequestMethod(InputStream inputStream) throws IOException {
        LineState state = LineState.NONE;
        int b;
//        byte[] bob = inputStream.readAllBytes();
        StringBuilder method = new StringBuilder(1000); // todo bad?

        while ((b = inputStream.read()) != -1) {
            if (b == 10 && state == LineState.SEEN_CR1) { // new line
                return method.toString();
            }
            if (b == 13) {
                state = LineState.SEEN_CR1;
            }
            method.append((char) b);

            if (method.length() > 50) {
                break;
            }
        }
        return null; // todo malformed;

    }

    enum LineState {
        NONE, SEEN_CR1, SEEN_LF1, SEEN_CR2
    }

    record Pair<A,B>(A a, B b) {}
}
