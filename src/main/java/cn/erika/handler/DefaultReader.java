package cn.erika.handler;

import cn.erika.core.Reader;
import cn.erika.core.TcpHandler;
import cn.erika.core.TcpSocket;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;

// 根据自定协议实现的一个处理数据的类
public class DefaultReader implements Reader {
    private Logger log = Logger.getLogger(this.getClass().getName());
    // 持有的TcpSocket
    private TcpSocket socket;
    private Charset charset;
    private TcpHandler handler;
    private DataHead head;
    // 缓冲区
    private byte[] cache = new byte[0];
    private byte[] cacheTmp = new byte[0];
    // 缓冲区写入偏移量
    private int pos = 0;

    DefaultReader(Charset charset, TcpSocket socket, TcpHandler handler) {
        this.charset = charset;
        this.socket = socket;
        this.handler = handler;
    }

    @Override
    public void process(byte[] data, int len) throws IOException {
        // 如果缓冲区长度为0 则需要检查缓冲区的临时部分是否也为0
        if (cache.length == 0) {
            getHead(data, len);
        } else {
            // 如果缓冲区写入偏移量加上本次传来的数据大于缓冲区长度
            // 说明这次传来的数据包含两轮数据的一部分
            if (pos + len > cache.length) {
                // 先把缓冲区装满再说
                // 装满之后 剩下没装的就是下一轮数据的请求头加数据部分
                int available = cache.length - pos;
                System.arraycopy(data, 0, cache, pos, available);
                pos += available;
                // 将缓冲区交给处理器处理
                handler.deal(socket, head, cache);
                // 清空缓冲区 写入偏移量归零 数据头置空
                cache = new byte[0];
                pos = 0;
                head = null;
                // 把剩下的数据塞进缓冲区临时部分
                cacheTmp = new byte[len - available];
                System.arraycopy(data, available, cacheTmp, 0, len - available);
                // 如果这部分数据的长度比数据头长度长
                // 则说明这一轮的数据头就在这里面 解析数据头
                // 解析完之后会将临时区的数据部分放进缓冲区
                // 最后清空缓冲区的临时部分 等待下一次接收数据
                if (len - available > DataHead.LEN) {
                    getHead(cacheTmp, len - available);
                    cacheTmp = new byte[0];
                }
            } else {
                // 如果缓冲区能装下的话 就塞进缓冲区
                System.arraycopy(data, 0, cache, pos, len);
                pos += len;
            }
        }
        // 检查数据 如果写入偏移量刚好等于缓冲区容量 而且都不是0的话
        // 或者请求头中数据长度就是0 说明这一轮的数据都拿到了 开始处理并清空缓冲区,偏移量归零,数据头置空
        if (pos == cache.length && pos > 0 || head.getLen() == 0) {
            log.debug("数据完整 开始处理");
            handler.deal(socket, head, cache);
            cache = new byte[0];
            pos = 0;
            head = null;
        }
    }

    // 获取数据头 并将剩余的部分丢回缓冲区
    private void getHead(byte[] data, int len) {
        // 把数据头提取出来
        log.debug("读取数据头");
        byte[] bHead = new byte[DataHead.LEN];
        System.arraycopy(data, 0, bHead, 0, DataHead.LEN);
        String strHead = new String(bHead, charset);
        head = new DataHead();
        // 时间戳 13字节
        head.setTimestamp(new Date(Long.parseLong(strHead.substring(0, 13))));
        // 本次传输偏移量 10字节
        head.setPos(Long.parseLong(strHead.substring(13, 23)));
        // 本次传输长度 10字节
        head.setLen(Integer.parseInt(strHead.substring(23, 33)));
        // 指令部分 4字节
        head.setOrder(Integer.parseInt(strHead.substring(33, 37)));
        // 这是签名部分 长度256字节
        byte[] sign = new byte[256];
        System.arraycopy(data, 37, sign, 0, 256);
        head.setSign(sign);
        log.debug("数据头处理完成");
        log.debug(head.show());
        // 新建缓冲区 长度为数据头中的本次传输长度
        // 把剩下的数据拷贝进去
        cache = new byte[head.getLen()];
        System.arraycopy(data, DataHead.LEN, cache, 0, len - DataHead.LEN);
        pos = len - DataHead.LEN;
    }
}
