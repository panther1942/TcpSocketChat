package cn.erika.handler;

import cn.erika.core.TcpSocket;

import java.io.IOException;
import java.nio.charset.Charset;

public class ServerHandler extends DefaultHandler {

    private ClientManager manager = new ClientManager();

    public ServerHandler(Charset charset) throws IOException {
        super(charset);
    }

    public ServerHandler(Charset charset, int cacheSize) {
        super(charset, cacheSize);
    }

    @Override
    public void accept(TcpSocket socket) throws IOException {
        log.info("客户端接入: " +
                socket.getSocket().getRemoteSocketAddress().toString());
        manager.add(socket);
    }

    @Override
    public void close(TcpSocket socket) throws IOException {
        String add = socket.getSocket().getRemoteSocketAddress().toString();
        String id = manager.get(socket);
        log.info("即将关闭连接 User: " + id + " From: " + add);
        socket.getSocket().close();
        log.warn("连接中断 User:" + id + " From: " + add);
        manager.del(socket);
    }

    @Override
    void display(TcpSocket socket, String message) {
        String id = manager.get(socket);
        String add = socket.getSocket().getRemoteSocketAddress().toString();
        System.err.println("User: " + id + " From: " + add + " : " + message);
    }

    public void send(String target, String msg) throws Exception {
        TcpSocket socket = manager.get(target);
        write(socket, msg);
    }

    public void close(String key) throws IOException {
        TcpSocket socket = manager.get(key);
        if (socket != null) {
            manager.del(socket);
            write(socket, "See you later", DataHead.Type.BYE);
            try {
                Thread.sleep(15000);
                if (!socket.getSocket().isClosed()) {
                    log.warn("等待超时 强制关闭连接");
                    close(socket);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void display() {
        log.info("当前接入数量: " + manager.size());
        for (String link : manager.linksInfo()) {
            log.info(link);
        }
    }

    public void encrypt(String key) throws IOException {
        log.info("请求加密通信");
        TcpSocket socket = manager.get(key);
        write(socket, "Hello World", DataHead.Type.ENCRYPT);
    }
}
