package com.ownwn.server;

import com.ownwn.server.intercept.InterceptReceiver;
import com.ownwn.server.intercept.Interceptor;
import com.ownwn.server.java.lang.replacement.*;
import com.ownwn.server.request.Request;
import com.ownwn.server.response.Response;
import com.ownwn.server.response.WholeBodyResponse;


public class Server {
    private final Map<String, RequestHandler> handleMethods = new HashMap<>();
    private final List<Interceptor> interceptMethods = new ArrayList<>();
    private final String friendlyAddress;
    private final String basePath;

    public static void create(short port) {
        create("/", port);
    }

    public static void create(String basePath, short port) {
        if (System.getProperty("os.name").contains("Windows")) {
            throw new RuntimeException("Windows is not supported. (glibc required)");
        }

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
        AnnotationFinder.loadAllAnnotatedMethods(packageName, handleMethods, interceptMethods);
        this.basePath = basePath;
        BaseHttpServer httpServer = BaseHttpServer.create(port, this::handle);

        friendlyAddress = httpServer.getAddress() + ":" + port;
    }

    private Response handle(Request request) {
        if (!request.path().startsWith(basePath)) {
            return Response.notFound;
        }
        request.setPath(request.path().substring(basePath.length()));
        for (Interceptor interceptor : interceptMethods) {
            InterceptReceiver rec = new InterceptReceiver();
            interceptor.handle(request, rec);
            if (rec.isClosed()) return rec.getResponse();
        }

        RequestHandler handler = handleMethods.get(cleanUrl(request.path()));
        if (handler == null) return Response.notFound;
        if (handler.method() != request.method()) {
            return WholeBodyResponse.of(405, "Method Not Allowed");
        }
        return handler.handle(request);
    }

    public static String cleanUrl(String url) {
        if (url.startsWith("/")) url = url.substring(1);
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        return url;
    }
}