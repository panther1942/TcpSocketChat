package cn.erika.plugins.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

public class AES {
    // 设置当前加密方式为AES加密方式
    private static final String KEY_ALGORITHM = "AES";
    // 设置加密算法
    private static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";//默认的加密算法
    private static Random random = new Random();

    public static String randomPassword(int len) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < len; i++) {
            int idx = random.nextInt(95) + 33;
            buffer.append(String.valueOf((char) idx));
        }
        return buffer.toString();
    }

    /**
     * 调用jdk的AES加密方法加密数据
     *
     * @param data     需要加密的数据
     * @param password 密码
     * @return 加密后的数据
     * @throws IOException 如果密钥无效或者不支持AES加密方式，则抛出该异常
     */
    public static byte[] encrypt(byte[] data, String password) throws IOException {
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(password));
            return cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("无效的秘钥: " + password);
        }
    }

    /**
     * 调用jdk的AES加密方式解密数据
     *
     * @param data     需要解密的数据
     * @param password 密码
     * @return 解密后的数据
     * @throws IOException 如果密钥无效或者不支持AES加密方式，则抛出该异常
     */
    public static byte[] decrypt(byte[] data, String password) throws IOException {
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(password));
            return cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("无效的秘钥: " + password);
        }
    }

    /**
     * 带用jdk的AES加密方式生成加密对象
     *
     * @param password 密码
     * @return 加密对象
     * @throws NoSuchAlgorithmException 如果不支持AES加密方式则抛出该异常
     */
    private static SecretKeySpec getSecretKey(final String password) throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance(KEY_ALGORITHM);
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        secureRandom.setSeed(password.getBytes());
        generator.init(128, secureRandom);
        SecretKey key = generator.generateKey();
        return new SecretKeySpec(key.getEncoded(), KEY_ALGORITHM);
    }
}
