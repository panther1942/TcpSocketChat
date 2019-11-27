package cn.erika.core;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;

/**
 * 服务器的实现类
 */
public class TcpServer implements Runnable {
    // 日志
    private Logger log = Logger.getLogger(this.getClass().getName());
    private ServerSocket server;
    // 处理器
    private TcpHandler handler;
    private InetSocketAddress listen;
    // 队列数量 限制并发数 防止内存占用过多
    private int capacity;

    /**
     * 默认的构造方法
     *
     * @param host     服务器监听的主机地址
     * @param port     服务器监听的主机端口
     * @param capacity 队列数量
     * @param handler  处理器
     */
    public TcpServer(String host, int port, int capacity, TcpHandler handler) {
        this.listen = new InetSocketAddress(host, port);
        this.capacity = capacity;
        this.handler = handler;
    }

    /**
     * 调用此方法开始监听并运行
     *
     * @throws IOException 如果未指定Socket管理器或者开启服务器的过程中发生错误
     */
    public void listen() throws IOException {
        log.info("尝试启动服务器 监听: [" +
                listen.getHostName() + ":" + listen.getPort() + "]");

        server = new ServerSocket(
                listen.getPort(),
                capacity,
                listen.getAddress());
        new Thread(this).start();
        this.log.info("启动服务器成功");
    }

    @Override
    public void run() {
        try {
            // 只要服务器没有关闭 将一直循环下去
            while (!server.isClosed()) {
                Socket socket = null;
                try {
                    // SocketServer.accept()方法是阻塞的 也就是说
                    // 在关闭服务器的时候需要建立一个回环连接使循环终止
                    socket = server.accept();
                } catch (IOException e) {
                    this.log.error("处理接入连接时发生错误: " + e.getMessage());
                }
                if (socket != null) {
                    try {
                        TcpSocket tcpSocket = new TcpSocket(socket, handler);
                        new Thread(tcpSocket).start();
                        handler.accept(tcpSocket);
                    } catch (IOException e) {
                        this.log.error("无法获取IO流: " + e.getMessage());
                    }
                }
            }
        } catch (NumberFormatException e) {
            this.log.error("配置文件无效: " + e.getMessage());
        }
    }

    public void shutdown() throws IOException {
        this.server.close();
    }


}
