package cn.erika.core;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 服务器的实现类
 */
public class TcpServer implements Runnable {
    // 日志
    private Logger log = Logger.getLogger(getClass().getName());
    private ServerSocket server;
    // 处理器
    private TcpHandler handler;
    private InetSocketAddress address;

    /**
     * 默认的构造方法
     *
     * @param address 服务器监听的主机地址
     * @param handler 处理器
     */
    public TcpServer(InetSocketAddress address, TcpHandler handler) {
        this.address = address;
        this.handler = handler;
    }

    /**
     * 调用此方法开始监听并运行
     *
     * @throws IOException 如果未指定Socket管理器或者开启服务器的过程中发生错误
     */
    public void listen() throws IOException {
        log.info("尝试启动服务器 监听: [" +
                address.getHostName() + ":" + address.getPort() + "]");
        System.out.println("尝试启动服务器 监听: [" +
                address.getHostName() + ":" + address.getPort() + "]");
        server = new ServerSocket();
        server.bind(address);
        new Thread(this).start();
        log.info("启动服务器成功");
        System.out.println("启动服务器成功");
    }

    @Override
    public void run() {
        try {
            // 只要服务器没有关闭 将一直循环下去
            while (!server.isClosed()) {
                Socket socket = null;
                try {
                    socket = server.accept();
                    try {
                        TcpSocket tcpSocket = new TcpSocket(socket, handler);
                        Thread t = new Thread(tcpSocket);
                        t.setDaemon(true);
                        t.start();
                        handler.accept(tcpSocket);
                    } catch (IOException e) {
                        log.error("无法获取IO流", e);
                    }
                } catch (IOException e) {
                    log.warn("连接被迫关闭");
                    log.debug(e);
                }
            }
            log.info("服务器停止运行");
            System.out.println("服务器停止运行");
        } catch (NumberFormatException e) {
            log.error("配置文件无效", e);
            System.err.println("配置文件无效: " + e.getMessage());
        }
    }

    public void shutdown() throws IOException {
        log.info("即将关闭服务器");
        System.out.println("即将关闭服务器");
        server.close();
    }
}
