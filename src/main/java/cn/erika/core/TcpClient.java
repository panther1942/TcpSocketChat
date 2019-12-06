package cn.erika.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 客户端的实现类 作为一个TcpSocket容器 执行各种操作需要通过TcpHandler实现
 */
public class TcpClient {

    private Socket socket;
    private TcpHandler handler;

    /**
     * 默认的构造方法
     *
     * @param handler 处理器
     */
    public TcpClient(TcpHandler handler) throws IOException {
        this.handler = handler;
        socket = new Socket();
        socket.setReuseAddress(true);
    }

    public void connect(InetSocketAddress address) throws IOException {
        try {
            socket.connect(address);
            TcpSocket tcpSocket = new TcpSocket(socket, handler);
            new Thread(tcpSocket).start();
            handler.accept(tcpSocket);
        } catch (IOException e) {
            throw new IOException("无法连接到服务器: " + e.getMessage());
        }
    }
}
