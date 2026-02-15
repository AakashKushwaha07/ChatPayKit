package com.chatpaykit.util;

import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class RazorpaySignatureUtil {

    public static boolean verify(String payload, String receivedSignature, String secret) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(keySpec);
            byte[] hash = sha256Hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = bytesToHex(hash);
            return computed.equals(receivedSignature);
        } catch (Exception e) {
            return false;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
