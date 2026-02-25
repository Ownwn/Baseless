package com.ownwn.server;

import com.ownwn.server.java.lang.replacement.*;
import com.ownwn.server.java.lang.replacement.stream.InputStream;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormByteParser {
    private static final Pattern dispositionPattern = Pattern.compile("Content-Disposition: form-data; name=\"([^\"]+)\"(?:; filename=\"([^\"]+)\")?");

    private byte[] bytes;
    private final String boundary;
    private int boundaryIndex = 0;
    private List<Chunk> chunks;

    private record Chunk(String disposition, byte[] bytes) {
        @Override
        public String toString() {
            return disposition + "||||EndOfDeposition||||"+ new String(bytes);
        }
    }


    public FormByteParser(InputStream body, String boundary, int contentLength) {
        this.boundary = boundary;
        try {
            this.bytes = new byte[contentLength];
            if (body.readNBytes(this.bytes, 0, contentLength) < contentLength) {
                throw new IOException("didn't read all the bytes"); // code smell? definitely
            }
        } catch (IOException e) {
            e.printStackTrace();
            chunks = List.of();
            return;
        }


        List<byte[]> rawParts;
        try {
            rawParts = readParts();
        } catch (IOException e) {
            chunks = List.of();
            return;
        }

        chunks = rawParts.stream()
                .filter((byte[] arr) -> arr.length - boundary.length() > 0)
                .map(arr ->{

                    int index = indexOf(arr, new byte[]{13, 10,13,10}); // CRLF CRLF
                    if (index == -1) {
                        return null;
                    }
                    return new Chunk(new String(Arrays.copyOfRange(arr, 0, index)), Arrays.copyOfRange(arr, index+4, arr.length-2-boundary.length()));
                } )
                .toList();
    }

    public Map<String, List<FormAttachment>> getTypeGroups() {
        return chunks.stream().collect(HashMap::new, (map, chunk) -> {

            String[] contentHeaders = chunk.disposition.trim().split("\r\n");
            Matcher m = dispositionPattern.matcher(contentHeaders[0]);
            if (!m.find()) {
                return; // malformed request
            }

            FormAttachment attachment = new FormAttachment(m.group(1), m.groupCount() >= 2 ? m.group(2) : "text", chunk.bytes());

            map.computeIfAbsent(m.group(1), g -> new ArrayList<>()).add(attachment);

        }, (p1, p2) -> {throw new Error("cant parallel");});
    }

    private int indexOf(byte[] haystack, byte[] needle) {
        for (int i = 0; i <= haystack.length-needle.length; i++) {
            boolean f = true;
            for (int k = 0; k < needle.length; k++) {
                if (haystack[i+k] != needle[k]) {
                    f = false;
                    break;
                }
            }
            if (f) return i;

        }
        return -1;
    }

    public List<byte[]> readParts() throws IOException {
        List<Integer> endIndices = new ArrayList<>(32);

        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] != boundary.charAt(boundaryIndex++)) {
                boundaryIndex = 0;
                if (bytes[i] == boundary.charAt(0)) boundaryIndex = 1;
            }
            if (boundaryIndex >= boundary.length()) {
                endIndices.add(i+1);
                boundaryIndex = 0;
            }

        }
        List<byte[]> parts = new ArrayList<>(endIndices.size());

        int prev = 0;
        for (int end : endIndices) {
            parts.add(Arrays.copyOfRange(bytes, prev, end));
            prev = end;

        }

        return parts;
    }

    private static byte[] makeArray(List<Byte> bytes) {
        byte[] res = new byte[bytes.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = bytes.get(i);
        }
        return res;
    }
}
