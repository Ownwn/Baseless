package com.ownwn.server;

import com.ownwn.server.request.Request;
import com.ownwn.server.response.Response;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

interface RequestHandler {
    Response handle(Request request);

    HttpMethod method();


    static RequestHandler from(Method m, Handle handle, Object instance) {
        validateExchangeMethod(m);
        return new RequestHandler() {
            @Override
            public Response handle(Request request) {
                try {
                    Object res = m.invoke(instance, request);
                    return (Response) res;
                } catch (InvocationTargetException e) {
                    if (e.getCause() instanceof RuntimeException cause) throw cause;
                    if (e.getCause() instanceof Error cause) throw cause;
                    throw new RuntimeException(e.getCause());
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public HttpMethod method() {
                return handle.method();
            }
        };

    }

    private static void validateExchangeMethod(Method m) {
        if (m.getParameterCount() != 1 || !Request.class.isAssignableFrom(m.getParameterTypes()[0])) {
            throw new RuntimeException("Invalid handler parameters for method " + m + ". Should be " + Request.class);
        }

        if (!Response.class.isAssignableFrom(m.getReturnType())) {
            throw new RuntimeException("Invalid handler return type for method " + m + ". Should be " + Response.class);
        }
        if (Modifier.isStatic(m.getModifiers())) {
            throw new RuntimeException("Handler " + m + " must not be static");
        }
    }
}
