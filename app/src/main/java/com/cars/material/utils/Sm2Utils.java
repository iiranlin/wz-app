package com.cars.material.utils;

import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.SM2;

public class Sm2Utils {

    // 生成密钥对
    public static void generateKeyPair() {
        SM2 sm2 = SmUtil.sm2();
        System.out.println("私钥: " + sm2.getPrivateKeyBase64());
        System.out.println("公钥: " + sm2.getPublicKeyBase64());
    }

    // 加密(使用公钥)
    public static String encrypt(String publicKey, String plainText) {
        SM2 sm2 = SmUtil.sm2(null, publicKey);
        return sm2.encryptHex(plainText,KeyType.PublicKey);
    }

    // 解密(使用私钥)
    public static String decrypt(String privateKey, String cipherText) {
        SM2 sm2 = SmUtil.sm2(privateKey, null);
        return sm2.decryptStr(cipherText, KeyType.PrivateKey);
    }
}
