package com.ownwn.server;

import com.ownwn.server.intercept.Intercept;
import com.ownwn.server.intercept.Interceptor;
import com.ownwn.server.java.lang.replacement.*;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AnnotationFinder {
    private static final Map<Class<?>, Object> instances = new HashMap<>();
    private AnnotationFinder() {}

    static void loadAllAnnotatedMethods(String packageName, Map<String, RequestHandler> handleMethods, List<Interceptor> interceptMethods) {

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);

            List<Class<?>> classes = new ArrayList<>();
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();

                if (resource.getProtocol().equals("file")) {
                    File directory = new File(resource.getFile());
                    classes.addAll(findClasses(directory, packageName));
                } else if (resource.getProtocol().equals("jar")) {
                    classes.addAll(findClassesInJar(resource, path, packageName));
                }
            }

            classes.forEach(clazz -> loadMethods(clazz, handleMethods, interceptMethods));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (handleMethods.isEmpty()) {
            System.err.println("No handlers found, your server won't do anything...");
        }

    }

    /** Written by LLM */
    private static List<Class<?>> findClassesInJar(URL resource, String path, String packageName) throws IOException {
        List<Class<?>> classes = new ArrayList<>();
        JarURLConnection connection = (JarURLConnection) resource.openConnection();
        JarFile jarFile = connection.getJarFile();
        Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();

            if (entryName.startsWith(path) && entryName.endsWith(".class")) {
                String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);
                try {
                    classes.add(Class.forName(className));
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        return classes;
    }

    private static List<Class<?>> findClasses(File directory, String packageName) {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }

        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                String className = (packageName.isEmpty() ? "" : packageName + ".") + file.getName().substring(0, file.getName().length() - 6);
                try {
                    classes.add(Class.forName(className));
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        return classes;
    }

    private static <T> void loadMethods(Class<T> clazz, Map<String, RequestHandler> map, List<Interceptor> interceptors) {
        loadInterceptors(clazz, getAnnotatedMethods(clazz, Intercept.class), interceptors);
        loadHandlers(clazz, getAnnotatedMethods(clazz, Handle.class), map);
    }

    private static <T> List<Method> getAnnotatedMethods(Class<T> clazz, Class<? extends Annotation> annotationClass) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(annotationClass))
                .toList(); // todo remove constructor wrap
    }

    @SuppressWarnings("unchecked")
    private static <T> T getInstance(Class<T> clazz) {
        // noinspection all
        T instance = (T) instances.computeIfAbsent(clazz, k -> {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return instance;
    }

    private static <T> void loadHandlers(Class<T> clazz, List<Method> methods, Map<String, RequestHandler> handlerMap) {
        if (methods.isEmpty()) {
            return;
        }

        T instance = getInstance(clazz);

        for (Method method : methods) {
            Handle annotation = method.getAnnotation(Handle.class);
            RequestHandler handler = RequestHandler.from(method, annotation, instance);
            String path = Server.cleanUrl(annotation.value());

            if (handlerMap.put(path, handler) != null) {
                throw new RuntimeException("Duplicate path: " + path);
            }
        }
    }

    private static <T> void loadInterceptors(Class<T> clazz, List<Method> methods, List<Interceptor> interceptors) {
        if (methods.isEmpty()) {
            return;
        }

        T instance = getInstance(clazz);

        for (Method method : methods) {
            Intercept annotation = method.getAnnotation(Intercept.class);
            Interceptor interceptor = Interceptor.from(method, annotation, instance);
            interceptors.add(interceptor);
        }
    }
}