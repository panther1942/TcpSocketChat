package cn.erika.handler;

import cn.erika.core.TcpSocket;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;


public class Reader {
    private Logger log = Logger.getLogger(this.getClass().getName());
    private Charset charset;
    private Handler handler;
    private DataHead head;
    private byte[] cache = new byte[0];
    private byte[] cacheTmp = new byte[0];
    private int pos = 0;

    Reader(Charset charset, Handler handler) {
        this.charset = charset;
        this.handler = handler;
    }

    public void read(TcpSocket socket, byte[] data, int len) throws IOException {
        if (cache.length == 0) {
            if (cacheTmp.length == 0) {
                getHead(data, len);
            } else {
                byte[] tmp = new byte[cacheTmp.length + len];
                System.arraycopy(cacheTmp, 0, tmp, 0, cacheTmp.length);
                System.arraycopy(data, 0, tmp, cacheTmp.length, len);
                pos = cacheTmp.length + len;
                getHead(tmp, pos);
                cacheTmp = new byte[0];
            }
        } else {
            if (pos + len > cache.length) {
                int available = cache.length - pos;
                System.arraycopy(data, 0, cache, pos, available);
                pos += available;
                handler.deal(socket, head, cache);
                cache = new byte[0];
                pos = 0;
                head = null;
                cacheTmp = new byte[len - available];
                System.arraycopy(data, available, cacheTmp, 0, len - available);
                if (len - available > DataHead.LEN) {
                    getHead(cacheTmp, len - available);
                    cacheTmp = new byte[0];
                }
            } else {
                System.arraycopy(data, 0, cache, pos, len);
                pos += len;
            }
        }
        if (pos == cache.length && pos > 0 || head.getLen()==0) {
            log.debug("数据完整 开始处理");
            handler.deal(socket, head, cache);
            cache = new byte[0];
            pos = 0;
            head = null;
        }
    }

    private void getHead(byte[] data, int len) {
        log.debug("读取数据头");
        byte[] bHead = new byte[DataHead.LEN];
        System.arraycopy(data, 0, bHead, 0, DataHead.LEN);
        String strHead = new String(bHead, charset);
        head = new DataHead();
        head.setTimestamp(new Date(Long.parseLong(strHead.substring(0, 13))));
        head.setPos(Integer.parseInt(strHead.substring(13, 23)));
        head.setLen(Integer.parseInt(strHead.substring(23, 33)));
        head.setOrder(Integer.parseInt(strHead.substring(33, 37)));
        byte[] sign = new byte[256];
        System.arraycopy(data, 37, sign, 0, 256);
        head.setSign(sign);
        log.debug(head.show());
        cache = new byte[head.getLen()];
        System.arraycopy(data, DataHead.LEN, cache, 0, len - DataHead.LEN);
        pos = len - DataHead.LEN;
        log.debug("数据头处理完成");
    }
}
