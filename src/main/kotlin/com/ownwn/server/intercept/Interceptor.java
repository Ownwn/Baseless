package com.ownwn.server.intercept;

import com.ownwn.server.HttpMethod;
import com.ownwn.server.request.Request;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/** Acts like middleware, used to intercept requests before theyre routed */
public interface Interceptor {

    void handle(Request request, InterceptReceiver interceptReceiver);

    HttpMethod method();


    static Interceptor from(Method m, Intercept handle, Object instance) {
        validateExchangeMethod(m);
        return new Interceptor() {
            @Override
            public void handle(Request request, InterceptReceiver interceptReceiver) {
                try {
                    m.invoke(instance, request, interceptReceiver);
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
        if (m.getParameterCount() != 2 || !Request.class.isAssignableFrom(m.getParameterTypes()[0]) || !InterceptReceiver.class.isAssignableFrom(m.getParameterTypes()[1])) {
            throw new RuntimeException("Invalid handler parameters for method " + m + ". Should be " + Request.class + ", " + InterceptReceiver.class);
        }

        if (!void.class.isAssignableFrom(m.getReturnType())) {
            throw new RuntimeException("Invalid handler return type for method " + m + ". Should be void");
        }
        if (Modifier.isStatic(m.getModifiers())) {
            throw new RuntimeException("Handler " + m + " must not be static");
        }
    }
}
