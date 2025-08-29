package com.cars.material.utils;

import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.encoders.Hex;
import java.nio.charset.StandardCharsets;

public class Sm4Util {

    private static final String SECRET_KEY = "1234567890abcdef"; // 16字节密钥

    public static String encrypt(String plainText) {
        try {
            byte[] input = plainText.getBytes(StandardCharsets.UTF_8);
            byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
            byte[] output = sm4EcbProcess(input, keyBytes, true);
            return Hex.toHexString(output);
        } catch (Exception e) {
            throw new RuntimeException("SM4加密失败", e);
        }
    }

    public static String decrypt(String cipherHex) throws Exception {
//        try {
            byte[] cipherBytes = Hex.decode(cipherHex);
            byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
            byte[] output = sm4EcbProcess(cipherBytes, keyBytes, false);
            return new String(output, StandardCharsets.UTF_8);
//        } catch (Exception e) {
//            throw new RuntimeException("SM4解密失败", e);
//        }
    }

    private static byte[] sm4EcbProcess(byte[] input, byte[] key, boolean isEncrypt) throws Exception {
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new SM4Engine(), new PKCS7Padding());
        cipher.init(isEncrypt, new KeyParameter(key));

        byte[] output = new byte[cipher.getOutputSize(input.length)];
        int len1 = cipher.processBytes(input, 0, input.length, output, 0);
        int len2 = cipher.doFinal(output, len1);

        byte[] result = new byte[len1 + len2];
        System.arraycopy(output, 0, result, 0, result.length);
        return result;
    }
}
