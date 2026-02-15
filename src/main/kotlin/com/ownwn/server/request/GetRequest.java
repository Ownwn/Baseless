package com.ownwn.server.request;


import com.ownwn.server.Headers;
import com.ownwn.server.HttpMethod;
import com.ownwn.server.java.lang.replacement.*;
import com.ownwn.server.java.lang.replacement.stream.InputStream;
import com.ownwn.server.java.lang.replacement.stream.OutputStream;


public class GetRequest extends Request {


    public GetRequest(SimpleAddress remoteAddress, InputStream requestBody, Headers requestHeaders, OutputStream responseBody, String path, Map<String, String> cookies, Map<String, String> queryParameters) {
        super(remoteAddress, requestBody, requestHeaders, responseBody, path, cookies, queryParameters);
    }

    @Override
    public HttpMethod method() {
        return HttpMethod.GET;
    }


}
