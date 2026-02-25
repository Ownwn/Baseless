package com.ownwn.server;

import com.ownwn.server.intercept.InterceptReceiver;
import com.ownwn.server.intercept.Interceptor;
import com.ownwn.server.java.lang.replacement.*;
import com.ownwn.server.request.Request;
import com.ownwn.server.response.Response;
import com.ownwn.server.response.TemplateResponse;

import java.io.IOException;

public class Server {
    private final Map<String, RequestHandler> handleMethods = new HashMap<>();
    private final List<Interceptor> interceptMethods = new ArrayList<>();
    private final String friendlyAddress;
    private final String basePath;

    public static void create(short port) {
        create("/", port);
    }

    public static void create(String basePath, short port) {
        try {
            String packageName = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                    .getCallerClass()
                    .getPackageName();
            Server s = new Server(packageName, basePath, port);
            System.out.println("Server started at http://" + s.friendlyAddress + basePath);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


    private Server(String packageName, String basePath, short port) throws Throwable {
        BaseHttpServer httpServer = BaseHttpServer.create(port, request -> {
            try {
                handle(request);
            } catch (Exception e) {
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
            }
        });
        AnnotationFinder.loadAllAnnotatedMethods(packageName, handleMethods, interceptMethods);
        this.basePath = basePath;

        friendlyAddress = httpServer.getAddress() + ":" + port;
    }

    private void handle(Request request) throws IOException {
        if (!request.path().startsWith(basePath)) {
            handle404(request);
        } else {
            request.setPath(request.path().substring(basePath.length()));
        }
        for (Interceptor interceptor : interceptMethods) {
            InterceptReceiver rec = new InterceptReceiver();
            interceptor.handle(request, rec);

            if (rec.isClosed()) {
                request.sendResponse(rec.getResponse().status(), rec.getResponse().headers(), rec.getResponse().body());
                return;
            }
        }

        String url = cleanUrl(request.path());
        RequestHandler handler = handleMethods.get(url);

        if (handler != null) {
            handleRawRequest(handler, request);
        } else {
            handle404(request);
        }
    }

    private void handle404(Request request) throws IOException {
        request.sendResponse404(TemplateResponse.notFound.body());
    }

    private void handleRawRequest(RequestHandler handler, Request request) throws IOException {
        Response response = handler.handle(request);

        request.sendResponse(response.status(), response.headers(), response.body());
    }



    public static String cleanUrl(String url) {
        if (url.startsWith("/")) url = url.substring(1);
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        return url;
    }
}