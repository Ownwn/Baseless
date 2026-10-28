package com.ownwn.server;

import com.ownwn.server.java.lang.replacement.stream.InputStream;

public record FormAttachment(String type, String fileName, InputStream bytes) {}