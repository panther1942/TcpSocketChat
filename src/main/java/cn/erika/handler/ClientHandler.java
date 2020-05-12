package cn.erika.handler;

import cn.erika.core.TcpClient;
import cn.erika.core.TcpSocket;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.HashMap;

public class ClientHandler extends DefaultHandler {
    private TcpClient client;
    private TcpSocket socket;
    private Reader reader;

    private DatagramSocket udpSocket;

    private HashMap<String, InetSocketAddress> addMap;

    public ClientHandler() throws IOException {
        reader = new Reader(charset, this);
        client = new TcpClient(this);
        udpSocket = new DatagramSocket();
        addMap = new HashMap<>();
    }

    public void connect(InetSocketAddress target) throws IOException {
        log.info("尝试连接服务器 [" + target.getHostName() + ":" + target.getPort() + "]");
        System.out.println("尝试连接服务器 [" + target.getHostName() + ":" + target.getPort() + "]");
        this.client.connect(target);
    }

    @Override
    public void accept(TcpSocket socket) throws IOException {
        log.info("成功连接到服务器");
        System.out.println("成功连接到服务器");
        this.socket = socket;
        Thread udpReceiver = new Thread(() -> {
            byte[] recBuf = new byte[4096];
            DatagramPacket rec = new DatagramPacket(recBuf, recBuf.length);
            while (true) {
                try {
                    udpSocket.receive(rec);
                    String msg = new String(rec.getData(), rec.getOffset(), rec.getLength());
                    System.out.println("UDP: " + msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        udpReceiver.setDaemon(true);
        udpReceiver.start();
    }

    @Override
    public void close(TcpSocket socket) {
        if (socket == null) {
            return;
        }
        String host = socket.getSocket().getInetAddress().getHostAddress();
        int port = socket.getSocket().getPort();
        log.warn("正在关闭连接 To: " + host + ":" + port);
        try {
            if (!socket.isClosed()) {
                write(socket, "See you later", DataHead.Order.BYE);
                socket.close();
            }
        } catch (IOException e) {
            log.warn("连接中断");
            System.out.println("连接中断");
        }
    }

    public void close() {
        log.info("即将关闭客户端");
        System.out.println("即将关闭客户端");
        close(socket);
    }

    @Override
    public void read(TcpSocket socket, byte[] data, int len) throws IOException {
        reader.read(socket, data, len);
    }

    @Override
    protected void handler(TcpSocket socket, DataHead head, byte[] data) throws IOException {
        switch (head.getOrder()) {
            case HELLO_WORLD:
                log.info("服务器要求加密通信");
                System.out.println("服务器要求加密通信");
                encrypt();
                break;
            case FILE_RECEIVE_FINISHED:
                log.info("文件传输完成: " + new String(data, charset));
                System.out.println("文件传输完成: " + new String(data, charset));
                if (getFlag(Action.CLOSE_AFTER_FINISHED)) {
                    close();
                }
                break;
            case REMOTE_ADDRESS:
                log.info("远端地址: " + new String(data, charset));
                String msg = new String(data, charset);
                String dest = msg.split("#")[0];
                String add = msg.split("#")[1].split(":")[0];
                String port = msg.split("#")[1].split(":")[1];
                if (!addMap.containsKey(dest)) {
                    find(dest);
                }
                addMap.put(dest, new InetSocketAddress(add, Integer.parseInt(port)));
                break;
            default:
                log.warn("未知消息头: " + head.show() + "\n内容: " + new String(data, charset));
        }
    }

    public void send(String msg) throws IOException {
        write(socket, msg);
    }

    public void send(String dest, String msg) throws IOException {
        write(socket, dest + ":" + msg, DataHead.Order.TALK);
    }

    public void encrypt() throws IOException {
        log.info("请求加密通信");
        System.out.println("请求加密通信");
        write(socket, keyPair[0], DataHead.Order.ENCRYPT);
    }

    public void sendFile(File file) throws IOException {
        sendFileHead(socket, file, file.getAbsolutePath());
    }

    public void sendFile(File file, String filename) throws IOException {
        sendFileHead(socket, file, filename);
    }

    public void name(String nickname) throws IOException {
        write(socket, nickname, DataHead.Order.NAME);
    }

    public void find() throws IOException {
        write(socket, "", DataHead.Order.FIND);
    }

    public void find(String msg) throws IOException {
        write(socket, msg + "#" + udpSocket.getLocalSocketAddress().toString().split(":")[1], DataHead.Order.DIRECT);
    }

    public void direct(String dest, String msg) throws IOException {
        if (addMap.containsKey(dest)) {
            byte[] data = msg.getBytes("UTF-8");
            DatagramPacket packet = new DatagramPacket(
                    data,
                    data.length,
                    addMap.get(dest)
            );
            udpSocket.send(packet);
        } else {
            System.out.println("未获得目标地址");
        }
    }

    @Override
    protected void display(TcpSocket socket, String message) {
        boolean isEncrypt = socket.getAttr(Extra.ENCRYPT);
        String add = socket.getSocket().getInetAddress().getHostAddress();
        System.out.println((isEncrypt ? "" : "!") + "From: [" + add + ":" + socket.getSocket().getPort() + "]" + message);
    }

    public boolean isClosed() {
        return socket.isClosed();
    }
}