package com.arianesline.ariane.plugin.speleodb;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class HTTPRequestMultipartBodyTest {
    
    public static void main(String[] args) throws IOException {
        testBasicStringPart();
        testMultipleParts();
        testByteArrayPart();
        testBoundaryGeneration();
        System.out.println("All tests passed!");
    }
    
    static void testBasicStringPart() throws IOException {
        HTTPRequestMultipartBody body = new HTTPRequestMultipartBody.Builder()
            .addPart("field1", "value1")
            .build();
        
        assert body.getContentType().startsWith("multipart/form-data; boundary=");
        assert body.getBoundary() != null && !body.getBoundary().isEmpty();
        
        String bodyStr = new String(body.getBody(), StandardCharsets.UTF_8);
        assert bodyStr.contains("Content-Disposition: form-data; name=\"field1\"");
        assert bodyStr.contains("Content-Type: text/plain");
        assert bodyStr.contains("value1");
        assert bodyStr.contains("--" + body.getBoundary() + "--");
        System.out.println("✓ Basic string part test passed");
    }
    
    static void testMultipleParts() throws IOException {
        HTTPRequestMultipartBody body = new HTTPRequestMultipartBody.Builder()
            .addPart("field1", "value1")
            .addPart("field2", "value2", "text/html")
            .build();
        
        String bodyStr = new String(body.getBody(), StandardCharsets.UTF_8);
        assert bodyStr.contains("name=\"field1\"");
        assert bodyStr.contains("value1");
        assert bodyStr.contains("name=\"field2\"");
        assert bodyStr.contains("value2");
        assert bodyStr.contains("text/html");
        System.out.println("✓ Multiple parts test passed");
    }
    
    static void testByteArrayPart() throws IOException {
        byte[] testData = "binary data".getBytes(StandardCharsets.UTF_8);
        
        HTTPRequestMultipartBody body = new HTTPRequestMultipartBody.Builder()
            .addPart("binaryField", testData, "application/octet-stream", "test.bin")
            .build();
        
        String bodyStr = new String(body.getBody(), StandardCharsets.UTF_8);
        assert bodyStr.contains("name=\"binaryField\"");
        assert bodyStr.contains("filename=\"test.bin\"");
        assert bodyStr.contains("application/octet-stream");
        assert bodyStr.contains("binary data");
        System.out.println("✓ Byte array part test passed");
    }
    
    static void testBoundaryGeneration() throws IOException {
        HTTPRequestMultipartBody body1 = new HTTPRequestMultipartBody.Builder()
            .addPart("test", "value").build();
        HTTPRequestMultipartBody body2 = new HTTPRequestMultipartBody.Builder()
            .addPart("test", "value").build();
        
        assert !body1.getBoundary().equals(body2.getBoundary());
        assert body1.getBoundary().matches("[a-z0-9]+");
        assert body2.getBoundary().matches("[a-z0-9]+");
        System.out.println("✓ Boundary generation test passed");
    }
} 