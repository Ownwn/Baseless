package com.ownwn.server.sockets;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.net.InetAddress;
import com.ownwn.server.java.lang.replacement.List;

import static java.lang.foreign.ValueLayout.*;

/** I'm aware that the byte sizes are quite hard coded, I will improve them in the future to hopefully support more archs */
public class SocketServer {
    private static final int AF_INET = 2; // todo use this


    private final FFIHelper ffiHelper;
    private final Arena arena;
    private int socketHandle;
    private InetAddress inetAddress;

    public SocketServer(short port, Arena arena) throws Throwable {

        ffiHelper = FFIHelper.of();
        this.arena = arena;

        socketHandle = (int) ffiHelper.callIntFunction("socket", JAVA_INT, List.of(2,1,0));

        if (socketHandle < 0) throw new RuntimeException("Bad socket");

        MemorySegment opt = arena.allocateFrom(JAVA_INT, 1);


        ffiHelper.callFunction("setsockopt", JAVA_INT,
                List.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, JAVA_LONG),
                List.of(socketHandle, 1, 2, opt, opt.byteSize()));

        MemorySegment sockaddr_in = arena.allocate(16L, 1);

        // family 2, port 2, addr 4, zerof 8. Total struct size == 16 bytes
        sockaddr_in.set(JAVA_SHORT, 0, (short) 2);
        sockaddr_in.set(JAVA_SHORT, 2, (Short) ffiHelper.callShortFunction("htons", JAVA_SHORT, List.of(port)));
        sockaddr_in.set(JAVA_INT, 4, 0);
        sockaddr_in.set(JAVA_LONG, 8, 0);

        if ((int) ffiHelper.callFunction("bind", JAVA_INT, List.of(JAVA_INT, ADDRESS, JAVA_LONG), List.of(socketHandle, sockaddr_in, 16L)) < 0) {
            throw new RuntimeException("Failed to invoke bind");
        }


        if ((int) ffiHelper.callFunction("listen", JAVA_INT, List.of(JAVA_INT, JAVA_INT), List.of(socketHandle, 16)) < 0) {
            throw new RuntimeException("Failed to invoke listen");
        }


    }

    public InetAddress getHostInetAddress() {
        if (inetAddress != null) return inetAddress;

        MemorySegment sockaddr_in_local = arena.allocate(16, 2);
        MemorySegment sock_addr_size = arena.allocate(JAVA_INT, (int) sockaddr_in_local.byteSize());


        try {
            int sockNameRes = (int) ffiHelper.callFunction("getsockname", JAVA_INT, List.of(JAVA_INT, ADDRESS, ADDRESS), List.of(socketHandle, sockaddr_in_local, sock_addr_size));
            if (sockNameRes == 0L) {
                MemorySegment ipString = arena.allocate(16);
                ffiHelper.callFunction("inet_ntop", ADDRESS, List.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS), List.of(AF_INET, sockaddr_in_local.asSlice(4), ipString, arena.allocate(JAVA_INT, 16)));
                return inetAddress = InetAddress.ofLiteral(ipString.getString(0));
            } else {
                ffiHelper.callFunction("perror", ADDRESS, List.of(ADDRESS) ,List.of(arena.allocateFrom("getsockname")));
                throw new RuntimeException("Error getting host IP, reason above");
            }

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

    }

    public Client accept() throws Throwable {

        // struct sockaddr_storage peer;
        MemorySegment sockAddrStoragePeer = arena.allocate(128); // todo
        MemorySegment addrLength = arena.allocate(128);
        addrLength.set(JAVA_INT, 0, (int) sockAddrStoragePeer.byteSize());


        int c = (int) ffiHelper.callFunction("accept", JAVA_INT, List.of(JAVA_INT, ADDRESS, ADDRESS), List.of(socketHandle, sockAddrStoragePeer, addrLength));
        if (c < 0) {
            // print out what went wrong
            ffiHelper.callFunction("perror", ADDRESS, List.of(ADDRESS) ,List.of(arena.allocateFrom("accept")));
            throw new RuntimeException("Failed to invoke accept with error (hopefully) above");
        }

        return Client.fromFileSocketDescriptor(c);

    }
}
