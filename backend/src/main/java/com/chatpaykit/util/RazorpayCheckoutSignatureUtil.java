package com.chatpaykit.util;

import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class RazorpayCheckoutSignatureUtil {

    private RazorpayCheckoutSignatureUtil() {}

    // For checkout success signature:
    // expected = HMAC_SHA256(order_id + "|" + payment_id, keySecret)
    public static boolean verify(String orderId, String paymentId, String providedSignature, String keySecret) {
        if (orderId == null || paymentId == null || providedSignature == null || keySecret == null) return false;

        String payload = orderId + "|" + paymentId;
        String expected = hmacSha256Hex(payload, keySecret);
        return constantTimeEquals(expected, providedSignature);
    }

    private static String hmacSha256Hex(String data, String secret) {
        try {
            Mac sha256 = Mac.getInstance("HmacSHA256");
            sha256.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = sha256.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC calculation failed", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int res = 0;
        for (int i = 0; i < a.length(); i++) res |= a.charAt(i) ^ b.charAt(i);
        return res == 0;
    }
}
