package cn.erika.plugins.security;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSA {
    //使用非对称加密算法
    private static final String KEY_ALGORITHM = "RSA";
//    private static final String SIGN_ALGORITHM = "SHA256withRSA";
    private static final String SIGN_ALGORITHM = "SHA384WITHRSA";

    /**
     * 生成RSA密钥对
     *
     * @param keySize 密钥长度
     * @return 返回一个装有私钥和公钥的数组 0为公钥 1为私钥
     * @throws NoSuchAlgorithmException 当前环境不支持KEY_ALGORITHM所标识的加密方法时抛出该异常
     */
    public static byte[][] initKey(int keySize) throws NoSuchAlgorithmException {
        byte[][] keyPair = new byte[2][];
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        generator.initialize(keySize);
        KeyPair pair = generator.generateKeyPair();

        keyPair[0] = pair.getPublic().getEncoded();
        keyPair[1] = pair.getPrivate().getEncoded();
        return keyPair;
    }

    /**
     * 通过byte数组生成原始公钥
     *
     * @param key 由原始私钥生成的byte数组
     * @return 返回原始公钥
     * @throws NoSuchAlgorithmException 当前环境不支持KEY_ALGORITHM所标识的加密方法时抛出该异常
     * @throws InvalidKeySpecException  公钥无效时抛出该异常
     */
    private static PublicKey getPublicKey(byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory factory = KeyFactory.getInstance(KEY_ALGORITHM);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(key);
        return factory.generatePublic(spec);
    }

    /**
     * 通过byte数组生成原始私钥
     *
     * @param key 由原始私钥生成的byte数组
     * @return 返回原始私钥
     * @throws NoSuchAlgorithmException 当前环境不支持KEY_ALGORITHM所标识的加密方法时抛出该异常
     * @throws InvalidKeySpecException  私钥无效时抛出该异常
     */
    private static PrivateKey getPrivateKey(byte[] key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(key);
        KeyFactory factory = KeyFactory.getInstance(KEY_ALGORITHM);
        return factory.generatePrivate(spec);
    }

    /**
     * 通过私钥加密数据
     *
     * @param data 要加密的数据
     * @param key  私钥
     * @return 返回加密后的数据
     * @throws Exception 1、当前环境不支持该加密方法 2、秘钥无效 3、秘钥不匹配
     */
    public static byte[] encryptByPrivateKey(byte[] data, byte[] key) throws IOException {
        try {
            PrivateKey privateKey = getPrivateKey(key);
            Cipher cipher = Cipher.getInstance(privateKey.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new IOException("当前环境不支持该加密方法: " + e.getMessage());
        } catch (InvalidKeySpecException | InvalidKeyException | IllegalBlockSizeException e) {
            throw new IOException("秘钥无效: " + Base64.getEncoder().encodeToString(key));
        } catch (BadPaddingException e) {
            throw new IOException("秘钥不匹配: " + e.getMessage());
        }
    }

    /**
     * 通过公钥加密数据
     *
     * @param data 要加密的数据
     * @param key  公钥
     * @return 返回加密后的数据
     * @throws Exception 1、当前环境不支持该加密方法 2、秘钥无效 3、秘钥不匹配
     */
    public static byte[] encryptByPublicKey(byte[] data, byte[] key) throws IOException {
        try {
            PublicKey publicKey = getPublicKey(key);
            Cipher cipher = Cipher.getInstance(publicKey.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new IOException("当前环境不支持该加密方法: " + e.getMessage());
        } catch (InvalidKeySpecException | InvalidKeyException | IllegalBlockSizeException e) {
            throw new IOException("秘钥无效: " + Base64.getEncoder().encodeToString(key));
        } catch (BadPaddingException e) {
            throw new IOException("秘钥不匹配: " + e.getMessage());
        }
    }

    /**
     * 通过私钥解密数据
     *
     * @param data 要解密的数据
     * @param key  公钥
     * @return 返回解密后的数据
     * @throws Exception 1、当前环境不支持该加密方法 2、秘钥无效 3、秘钥不匹配
     */
    public static byte[] decryptByPrivateKey(byte[] data, byte[] key) throws IOException {
        try {
            PrivateKey privateKey = getPrivateKey(key);
            Cipher cipher = Cipher.getInstance(privateKey.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new IOException("当前环境不支持该加密方法: " + e.getMessage());
        } catch (InvalidKeySpecException | InvalidKeyException | IllegalBlockSizeException e) {
            throw new IOException("秘钥无效: " + Base64.getEncoder().encodeToString(key));
        } catch (BadPaddingException e) {
            throw new IOException("秘钥不匹配: " + e.getMessage());
        }
    }

    /**
     * 通过公钥解密数据
     *
     * @param data 要解密的数据
     * @param key  公钥
     * @return 返回解密后的数据
     * @throws Exception 1、当前环境不支持该加密方法 2、秘钥无效 3、秘钥不匹配
     */
    public static byte[] decryptByPublicKey(byte[] data, byte[] key) throws IOException {
        try {
            PublicKey publicKey = getPublicKey(key);
            Cipher cipher = Cipher.getInstance(publicKey.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new IOException("当前环境不支持该加密方法: " + e.getMessage());
        } catch (InvalidKeySpecException | InvalidKeyException | IllegalBlockSizeException e) {
            throw new IOException("秘钥无效: " + Base64.getEncoder().encodeToString(key));
        } catch (BadPaddingException e) {
            throw new IOException("秘钥不匹配: " + e.getMessage());
        }
    }

    public static byte[] sign(byte[] data, byte[] key) throws IOException {
        try {
            PrivateKey priKey = getPrivateKey(key);
            Signature sign = Signature.getInstance(SIGN_ALGORITHM);
            sign.initSign(priKey);
            sign.update(data);
            return sign.sign();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("当前环境不支持该加密方法: " + e.getMessage());
        } catch (InvalidKeySpecException | InvalidKeyException e) {
            throw new IOException("秘钥无效: " + Base64.getEncoder().encodeToString(key));
        } catch (SignatureException e) {
            throw new IOException("签名对象未正确初始化: " + e.getMessage());
        }
    }

    public static boolean verify(byte[] data, byte[] key, byte[] signData) throws IOException {
        try {
            PublicKey pubKey = getPublicKey(key);
            Signature sign = Signature.getInstance(SIGN_ALGORITHM);
            sign.initVerify(pubKey);
            sign.update(data);
            return sign.verify(signData);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("当前环境不支持该加密方法: " + e.getMessage());
        } catch (InvalidKeySpecException | InvalidKeyException e) {
            throw new IOException("秘钥无效: " + Base64.getEncoder().encodeToString(key));
        } catch (SignatureException e) {
            throw new IOException("签名对象未正确初始化: " + e.getMessage());
        }
    }
}
