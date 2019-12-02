package cn.erika.handler;

import cn.erika.core.TcpSocket;
import cn.erika.plugins.security.AES;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;

public class ClientHandler extends DefaultHandler {
    private TcpSocket socket;

    public ClientHandler(Charset charset) {
        super(charset);
    }

    public ClientHandler(Charset charset, int cacheSize) {
        super(charset, cacheSize);
    }

    @Override
    public void accept(TcpSocket socket) throws IOException {
        this.socket = socket;
    }

    @Override
    public void close(TcpSocket socket) throws IOException {
        String add = socket.getSocket().getRemoteSocketAddress().toString();
        log.info("即将关闭连接 From: " + add);
        write(socket, "See you later", DataHead.BYE);
        socket.getSocket().close();
        log.warn("连接中断 From: " + add);
    }

    @Override
    void display(TcpSocket socket, String message) {
        String add = socket.getSocket().getRemoteSocketAddress().toString();
        log.info("From: " + add + " : " + message);
    }

    public void send(String msg) throws IOException {
        write(this.socket, msg);
    }

    public void encrypt() throws IOException {
        this.encrypt(socket);
    }

    public void encrypt(TcpSocket socket) throws IOException {
        log.info("请求加密通信");
        write(socket, "Hello World", DataHead.ENCRYPT);
    }

    public void sendFile(File file) throws IOException {
        sendFileHead(this.socket, file);
    }
}