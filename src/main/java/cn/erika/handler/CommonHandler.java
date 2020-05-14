package cn.erika.handler;

import cn.erika.core.Attribute;
import cn.erika.core.TcpHandler;
import cn.erika.plugins.security.RSA;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.charset.Charset;

public abstract class CommonHandler implements TcpHandler {
    // 日志
    protected Logger log = Logger.getLogger(this.getClass().getName());
    // 字符编码 默认UTF-8编码
    Charset charset = Charset.forName("UTF-8");

    /**
     * TcpSocket的额外属性
     */
    public enum Extra implements Attribute {
        ENCRYPT(0x100), // 启用加密
        PUBLIC_KEY(0x101), // 服务器公钥
        PASSWORD(0x102),// AES秘钥
        NICKNAME(0x200),// 用户昵称(默认为用户id)
        HIDE(0x201); // 用户是否隐身

        int value;

        Extra(int value) {
            this.value = value;
        }
    }

    /**
     * 用于加密通信的验签
     *
     * @param data 需要检验的数据
     * @param key RSA私钥
     * @param sign 数据签名
     * @throws IOException 如果数据非法或者签名无效则抛出该异常
     */
    protected void verify(byte[] data, byte[] key, byte[] sign) throws IOException {
        if (RSA.verify(data, key, sign)) {
            log.info("验签成功");
        } else {
            log.warn("验签失败 中断传输");
            throw new IOException("验签失败");
        }
    }
}
