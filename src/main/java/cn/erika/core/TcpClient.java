package cn.erika.core;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 客户端的实现类 作为一个TcpSocket容器 执行各种操作需要通过TcpHandler实现
 */
public class TcpClient {
    // 日志
    private Logger log = Logger.getLogger(this.getClass().getName());
    private TcpSocket socket;
    private TcpHandler handler;
    private InetSocketAddress listen;

    /**
     * 默认的构造方法
     *
     * @param host 服务器地址
     * @param port 服务器端口
     * @param handler 处理器
     */
    public TcpClient(String host, int port,TcpHandler handler) {
        this.listen = new InetSocketAddress(host, port);
        this.handler = handler;
    }

    public void connect() throws IOException {
        log.info("尝试连接服务器 [" + listen.getHostName() + ":" + listen.getPort() + "]");
        try {
            socket = new TcpSocket(new Socket(listen.getAddress(), listen.getPort()), handler);
            log.info("成功连接到服务器");
            new Thread(socket).start();
            handler.accept(socket);
        } catch (IOException e) {
            throw new IOException("无法连接到服务器: " + e.getMessage());
        }
    }

    public void shutdown() {
        log.info("尝试与服务器断开连接");
        try {
            handler.close(this.socket);
            log.info("与服务器连接中断");
        } catch (IOException e) {
            log.error("断开连接的过程中发生错误: " + e.getMessage());
        }
    }

    public boolean isClosed(){
        return this.socket.getSocket().isClosed();
    }
}
