package com.arianesline.ariane.plugin.speleodb;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class HTTPRequestMultipartBody {

    private final byte[] bytes;

    public String getBoundary() {
        return boundary;
    }

    public void setBoundary(String boundary) {
        this.boundary = boundary;
    }

    private String boundary;
    private HTTPRequestMultipartBody(byte[] bytes, String boundary) {
        this.bytes = bytes;
        this.boundary = boundary;
    }

    public String getContentType() {
        return "multipart/form-data; boundary=" + this.getBoundary();
    }
    public byte[] getBody() {
        return this.bytes;
    }

    private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] QUOTE_CRLF = "\"\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CONTENT_TYPE_PREFIX = "Content-Type: ".getBytes(StandardCharsets.UTF_8);
    private static final byte[] DOUBLE_CRLF = "\r\n\r\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] OCTET_STREAM_HEADER = "Content-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8);

    public static class Builder {
        private final String DEFAULT_MIMETYPE = "text/plain";

        public static class MultiPartRecord {
            private String fieldName;
            private String filename;
            private String contentType;
            private Object content;

            public String getFieldName() {
                return fieldName;
            }

            public void setFieldName(String fieldName) {
                this.fieldName = fieldName;
            }

            public String getFilename() {
                return filename;
            }

            public void setFilename(String filename) {
                this.filename = filename;
            }

            public String getContentType() {
                return contentType;
            }

            public void setContentType(String contentType) {
                this.contentType = contentType;
            }

            public Object getContent() {
                return content;
            }

            public void setContent(Object content) {
                this.content = content;
            }
        }

        List<MultiPartRecord> parts;

        public Builder() {
            this.parts = new ArrayList<>();
        }

        public Builder addPart(String fieldName, String fieldValue) {
            MultiPartRecord part = new MultiPartRecord();
            part.setFieldName(fieldName);
            part.setContent(fieldValue);
            part.setContentType(DEFAULT_MIMETYPE);
            this.parts.add(part);
            return this;
        }

        public Builder addPart(String fieldName, String fieldValue, String contentType) {
            MultiPartRecord part = new MultiPartRecord();
            part.setFieldName(fieldName);
            part.setContent(fieldValue);
            part.setContentType(contentType);
            this.parts.add(part);
            return this;
        }

        public Builder addPart(String fieldName, Object fieldValue, String contentType, String fileName) {
            MultiPartRecord part = new MultiPartRecord();
            part.setFieldName(fieldName);
            part.setContent(fieldValue);
            part.setContentType(contentType);
            part.setFilename(fileName);
            this.parts.add(part);
            return this;
        }

        @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
        public HTTPRequestMultipartBody build() throws IOException {
            String boundary = generateBoundary();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                for (MultiPartRecord record : parts) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("--").append(boundary).append("\r\n")
                      .append("Content-Disposition: form-data; name=\"").append(record.getFieldName());
                    if (record.getFilename() != null) {
                        sb.append("\"; filename=\"").append(record.getFilename());
                    }
                    out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                    out.write(QUOTE_CRLF);
                    Object content = record.getContent();
                    switch (content) {
                        case String string -> {
                            if (record.getContentType() != null) {
                                out.write(CONTENT_TYPE_PREFIX);
                                out.write(record.getContentType().getBytes(StandardCharsets.UTF_8));
                                out.write(CRLF);
                            }
                            out.write(DOUBLE_CRLF);
                            out.write(string.getBytes(StandardCharsets.UTF_8));
                        }
                        case byte[] bs -> {
                            out.write(OCTET_STREAM_HEADER);
                            out.write(bs);
                        }
                        case File file -> {
                            out.write(OCTET_STREAM_HEADER);
                            try {
                                Files.copy(file.toPath(), out);
                            } catch (IOException copyException) {
                                throw new IOException("Error copying file content: " + file.getName(), copyException);
                            }
                        }
                        default -> {
                            out.write(OCTET_STREAM_HEADER);
                            out.write(content.toString().getBytes(StandardCharsets.UTF_8));
                        }
                    }
                    out.write(CRLF);
                }
                out.write(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
                out.write(CRLF);
            } catch (IOException e) {
                throw new IOException("Error building HTTP request multipart body", e);
            }
            return new HTTPRequestMultipartBody(out.toByteArray(), boundary);
        }

        private static String generateBoundary() {
            return new BigInteger(256, new SecureRandom()).toString(36); // Base-36 uses only alphanumeric
        }
    }
}
