package com.ownwn.server.sockets;

import java.lang.foreign.*;

import com.ownwn.server.java.lang.replacement.ArrayList;
import com.ownwn.server.java.lang.replacement.List;
import com.ownwn.server.java.lang.replacement.stream.IntStream;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

public class FFIHelper {
    private Linker linker;
    private SymbolLookup stdLib;

    /** You must close the arena yourself! */
    public FFIHelper() {
        linker = Linker.nativeLinker();
        stdLib = linker.defaultLookup();
    }

    public static FFIHelper of() {
        return new FFIHelper();
    }

    /** adapted from https://dev.java/learn/ffm/native/ */
    public <T extends MemoryLayout> Object callFunction(String name, T returnType, List<T> types, List<Object> args) throws Throwable {
        if (types.size() != args.size()) {
            throw new IllegalArgumentException("Mismatch of number of args and types!");
        }

        MemorySegment function_addr = stdLib.find(name).orElseThrow(() -> new RuntimeException("Can't find function of name " + name));

        FunctionDescriptor fd = FunctionDescriptor.of(returnType, types.toArray(new MemoryLayout[types.size()]));

        var mh = linker.downcallHandle(function_addr, fd);
        return mh.invokeWithArguments(args);
    }

    public <T extends MemoryLayout> Object callIntFunction(String name, T returnType, List<Integer> args) throws Throwable { // todo unnecessary copy constructor
        return callFunction(name, returnType, new ArrayList<>(IntStream.range(0, args.size()).mapToObj(ignored -> (MemoryLayout) JAVA_INT).toList()), new ArrayList<>(args.stream().map(i -> (Object) i).toList()));
    }

    public <T extends MemoryLayout> Object callShortFunction(String name, T returnType, List<Short> args) throws Throwable {
        return callFunction(name, returnType, new ArrayList<>(IntStream.range(0, args.size()).mapToObj(ignored -> (MemoryLayout) JAVA_SHORT).toList()), new ArrayList<>(args.stream().map(i -> (Object) i).toList()));
    }
}
