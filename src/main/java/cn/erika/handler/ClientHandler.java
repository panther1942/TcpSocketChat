package cn.erika.handler;

import cn.erika.core.TcpClient;
import cn.erika.core.TcpSocket;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

public class ClientHandler extends DefaultHandler {
    private TcpClient client;
    private TcpSocket socket;
    private Cache cache;

    public ClientHandler() throws IOException {
        cache = new Cache(charset, this);
        client = new TcpClient(this);
    }

    @Override
    public void accept(TcpSocket socket) throws IOException {
        log.info("成功连接到服务器");
        this.socket = socket;
    }

    @Override
    public void close(TcpSocket socket) {
        String host = socket.getSocket().getInetAddress().getHostAddress();
        int port = socket.getSocket().getPort();
        log.warn("正在关闭连接 To: " + host + ":" + port);
        try {
            if (!socket.isClosed()) {
                write(socket, "See you later", DataHead.BYE);
                socket.close();
            }
        } catch (IOException e) {
            log.warn("连接已经断开");
        }
    }

    public void close() {
        log.info("正在关闭客户端");
        close(socket);
    }

    @Override
    public void deal(TcpSocket socket, byte[] data, int len) throws IOException {
        cache.read(socket, data, len);
    }

    @Override
    protected void handler(TcpSocket socket, DataHead head, byte[] data) {
        switch (head.getOrder()) {
            default:
                log.warn("未知消息头: " + head.show() + "\n内容: " + new String(data, charset));
        }
    }

    @Override
    protected void display(TcpSocket socket, String message) {
        boolean isEncrypt = socket.getAttr(ENCRYPT);
        String add = socket.getSocket().getInetAddress().getHostAddress();
        System.out.println((isEncrypt ? "" : "!") + "From: [" + add + ":" + socket.getSocket().getPort() + "]" + message);
    }

    public void connect(InetSocketAddress target) throws IOException {
        log.info("尝试连接服务器 [" + target.getHostName() + ":" + target.getPort() + "]");
        this.client.connect(target);
    }

    public void send(String msg) throws IOException {
        write(socket, msg);
    }

    public void encrypt() throws IOException {
        log.info("请求加密通信");
        write(socket, "Hello World", DataHead.ENCRYPT);
    }

    public void sendFile(File file) throws IOException {
        sendFileHead(socket, file);
    }

    public void registry(String nickname) throws IOException {
        write(socket, nickname, DataHead.REG);
    }

    public void talk(String nickname) throws IOException {
        write(socket, nickname, DataHead.TALK);
    }

    public void find() throws IOException {
        write(socket, null, DataHead.FIND);
    }
}