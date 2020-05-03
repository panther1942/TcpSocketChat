package cn.erika.handler;

import cn.erika.core.TcpClient;
import cn.erika.core.TcpSocket;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

public class ClientHandler extends DefaultHandler {
    private TcpClient client;
    private TcpSocket socket;
    private Reader reader;

    public ClientHandler() throws IOException {
        reader = new Reader(charset, this);
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
                write(socket, "See you later", DataHead.Order.BYE);
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
    public void read(TcpSocket socket, byte[] data, int len) throws IOException {
        reader.read(socket, data, len);
    }

    @Override
    protected void handler(TcpSocket socket, DataHead head, byte[] data) {
        switch (head.getOrder()) {
            case FILE_RECEIVE_FINISHED:
                log.info("文件传输完成: " + new String(data, charset));
                if (getFlag(Action.CLOSE_AFTER_FINISHED)) {
                    close();
                }
                break;
            default:
                log.warn("未知消息头: " + head.show() + "\n内容: " + new String(data, charset));
        }
    }

    @Override
    protected void display(TcpSocket socket, String message) {
        boolean isEncrypt = socket.getAttr(Extra.ENCRYPT);
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
        write(socket, keyPair[0], DataHead.Order.ENCRYPT);
    }

    public void sendFile(File file) throws IOException {
        sendFileHead(socket, file, file.getAbsolutePath());
    }

    public void sendFile(File file, String filename) throws IOException {
        sendFileHead(socket, file, filename);
    }

    public void registry(String nickname) throws IOException {
        write(socket, nickname, DataHead.Order.REG);
    }

    public void talk(String nickname) throws IOException {
        write(socket, nickname, DataHead.Order.TALK);
    }

    public void find() throws IOException {
        write(socket, "", DataHead.Order.FIND);
    }
}