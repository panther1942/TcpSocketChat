package cn.erika.core;

import cn.erika.handler.DataHead;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;

/**
 * 作为Socket的包装类 用于增强Socket的功能
 * 需要一个TcpHandler提供功能支持
 */
public class TcpSocket implements Runnable {
    // 日志
    private Logger log = Logger.getLogger(this.getClass().getName());
    private HashMap attr;

    private Socket socket;
    private TcpHandler handler;
    private InputStream in;
    private OutputStream out;

    private SocketAddress localAddress;

    // 核心属性0x00-0xFF
    public enum Standard implements Attribute {
        CACHE_SIZE(0x0);

        int value;

        Standard(int value) {
            this.value = value;
        }
    }

    /**
     * 默认构造方法 需要一个Socket对象和一个处理器
     *
     * @param socket  Socket对象
     * @param handler 处理器
     * @throws IOException 如果配置无效或者获取IO流失败
     */
    TcpSocket(Socket socket, TcpHandler handler) throws IOException {
        this.handler = handler;
        this.attr = new HashMap();
        this.socket = socket;
        this.socket.setTcpNoDelay(true);
        this.localAddress = this.socket.getRemoteSocketAddress();
        init();
    }

    TcpSocket(InetSocketAddress address, TcpHandler handler) throws IOException {
        this.handler = handler;
        this.attr = new HashMap();
        this.socket = new Socket();
        this.socket.setReuseAddress(true);
        this.socket.setTcpNoDelay(true);
        this.socket.connect(address);
        this.localAddress = this.socket.getRemoteSocketAddress();
        init();
    }

    private void init() throws IOException {
        // 初始化Socket属性
        handler.init(this);
        in = socket.getInputStream();
        out = socket.getOutputStream();
    }

    public void connect(InetSocketAddress address, TcpHandler handler) throws IOException {
        this.socket.close();
        this.handler = handler;
        if (!socket.isBound()) {
            this.socket.bind(localAddress);
        }
        for (int i = 0; i < 10; i++) {
            try {
                this.socket.connect(address);
                log.info("尝试第 " + (i + 1) + " 次连接");
                if (this.socket.isConnected()) {
                    log.info("连接成功");
                    break;
                }
            } catch (IOException e) {
                log.warn("第 " + (i + 1) + " 次连接失败");
            }
        }
        if (!socket.isConnected()) {
            throw new IOException("连接失败");
        }
        init();
    }

    @Override
    public void run() {
        // 缓冲区 如果初始化中未设置缓冲区大小将使用默认值4096 即4k
        int cacheSize = getAttr(Standard.CACHE_SIZE);
        byte[] cache = new byte[cacheSize];
        // 读取到的字节数 如果为-1说明连接中断
        int len;
        try {
            while (!socket.isClosed() && (len = in.read(cache)) > -1) {
                // 向处理器传输缓冲区和有效字节数
                handler.read(this, cache, len);
            }
        } catch (IOException e) {
            log.warn("连接中断: " + socket.getRemoteSocketAddress().toString());
            log.debug(e.getMessage());
            System.out.println("连接中断: " + socket.getRemoteSocketAddress().toString());
        } finally {
            try {
                close();
            } catch (IOException e) {
                log.error("断开连接的过程中发生错误: " + e.getMessage());
            }
        }
    }

    /**
     * 实现了Socket的写方法 并尝试解决2G以上字节流的问题(int最大值为2G)
     *
     * @param data 要发送的数据 是否编码由具体实现决定
     * @param len  发送数据的实际长度
     * @throws IOException 如果传输过程发生错误
     */
    public void write(byte[] data, int len) throws IOException {
        int pos = 0;
        // 这里用pos标记发送数据的长度 每次发送缓冲区大小个字节 直到pos等于数据长度len
        int cacheSize = getAttr(Standard.CACHE_SIZE);
        while (len - pos > cacheSize) {
            out.write(data, pos, cacheSize);
            pos += cacheSize;
        }
        out.write(data, pos, len - pos);
        out.flush();
    }

    public void close() throws IOException {
        if (!socket.isClosed()) {
            socket.close();
        }
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    /**
     * 获取包装的Socket对象 方便直接调用Socket的底层方法
     *
     * @return 包含的Socket对象
     */
    public Socket getSocket() {
        return this.socket;
    }

    @SuppressWarnings("HashMap中存放多种类型的数值 因此忽略强转警告")
    public void setAttr(Attribute k, Object v) {
        this.attr.put(k, v);
    }

    @SuppressWarnings("HashMap中存放多种类型的数值 因此忽略强转警告")
    public <T> T getAttr(Attribute k) {
        return (T) this.attr.get(k);
    }

    public void delAttr(String k) {
        this.attr.remove(k);
    }

    public void setHandler(TcpHandler handler) {
        this.handler = handler;
    }
}
