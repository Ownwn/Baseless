package com.ownwn.servertest;

import com.ownwn.server.*;
import com.ownwn.server.intercept.*;
import com.ownwn.server.request.*;
import com.ownwn.server.response.*;


public class TestServer {
    public static void main(String[] args) {
        Server.create("/api", Short.parseShort(args[0]));
    }

    @Intercept
    public void auth(Request request, InterceptReceiver receiver) {
        if (request.path().equals("/private")) receiver.closeWithResponse(WholeBodyResponse.unauthorised());
        if (request.path().equals("/rejected")) throw new HttpRequestException(413, "fixture rejection");
    }

    @Handle("ok")
    public Response ok(Request request) { return WholeBodyResponse.ok("hello"); }

    @Handle(value = "upload", method = HttpMethod.POST)
    public Response upload(PostRequest request) throws Exception {
        var data = new java.io.ByteArrayOutputStream();
        request.formAttachmentHandler().consumeAttachments(part -> data);
        return WholeBodyResponse.ok(data.toByteArray());
    }

    @Handle("broken")
    public Response broken(Request request) { throw new IllegalStateException("private fixture failure"); }
}
