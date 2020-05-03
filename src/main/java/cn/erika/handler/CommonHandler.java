package cn.erika.handler;

import cn.erika.core.TcpHandler;
import cn.erika.plugins.security.RSA;
import org.apache.log4j.Logger;

import java.io.IOException;

public abstract class CommonHandler implements TcpHandler {
    // 日志
    protected Logger log = Logger.getLogger(this.getClass().getName());

    protected void verify(byte[] data, byte[] key, byte[] sign) throws IOException {
        if (RSA.verify(data, key, sign)) {
            log.info("验签成功");
        } else {
            log.warn("验签失败 中断传输");
            throw new IOException("验签失败");
        }
    }
}
