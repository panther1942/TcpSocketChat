package cn.erika.handler;

import cn.erika.core.TcpServer;
import cn.erika.core.TcpSocket;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;

public class ServerHandler extends DefaultHandler {
    public static final int NICKNAME = 0x200;
    public static final int HIDE = 0x201;

    private TcpServer server;
    private SocketManager manager = new SocketManager();

    public void listen(InetSocketAddress address) throws IOException {
        server = new TcpServer(address, this);
        server.listen();
    }

    @Override
    public void accept(TcpSocket socket) throws IOException {
        log.info("客户端接入: " +
                socket.getSocket().getRemoteSocketAddress().toString());
        String id = manager.add(socket, new Cache(charset, this));
        socket.setAttr(NICKNAME, id);
        socket.setAttr(HIDE, false);
    }

    @Override
    public void close(TcpSocket socket) {
        String add = socket.getSocket().getRemoteSocketAddress().toString();
        String id = manager.get(socket);
        try {
            if (!socket.isClosed()) {
                log.info("正在关闭连接 User: " + id + " From: " + add);
                socket.close();
            }
        } catch (IOException e) {
            log.warn("连接已经中断");
        } finally {
            log.warn("连接中断 User:" + id + " From: " + add);
        }

        manager.del(socket);
    }

    @Override
    public void deal(TcpSocket socket, byte[] data, int len) throws IOException {
        manager.getCache(socket).read(socket, data, len);
    }

    @Override
    protected void handler(TcpSocket socket, DataHead head, byte[] data) throws IOException {
        switch (head.getOrder()) {
            case DataHead.REG:
                // 注册
                socket.setAttr(NICKNAME, new String(data, charset));
                write(socket, "Registry Successful");
                break;
            case DataHead.FIND:
                // 查找
                Set<String> list = manager.linksInfo();
                StringBuilder buffer = new StringBuilder();
                for (String name : list) {
                    boolean hide = manager.get(name).getAttr(HIDE);
                    if (!hide) {
                        buffer.append(name).append(":");
                    }
                }
                buffer.deleteCharAt(buffer.length() - 1);
                write(socket, buffer.toString());
                break;
            case DataHead.TALK:
                // 聊天
                String tmp = new String(data, charset);
                String nickname = tmp.split(":")[0];
                String message = tmp.substring(nickname.length() + 1);
                TcpSocket dest = manager.getByNickname(nickname);
                if (dest != null) {
                    String from = socket.getAttr(NICKNAME);
                    write(dest, from + ":" + message);
                } else {
                    write(socket, "未找到昵称: " + nickname);
                }
                break;
            case DataHead.HIDE:
                // 不允许查找
                socket.setAttr(HIDE, true);
                write(socket, "已禁止查找");
                break;
            case DataHead.SEEK:
                // 允许查找
                socket.setAttr(HIDE, false);
                write(socket, "已允许查找");
                break;
            default:
                log.warn("未知消息头: " + head.show() + "\n内容: " + new String(data, charset));
        }
    }

    @Override
    protected void display(TcpSocket socket, String message) {
        boolean isEncrypt = socket.getAttr(ENCRYPT);
        String id = manager.get(socket);
        String add = socket.getSocket().getInetAddress().getHostAddress();
        System.out.println((isEncrypt ? "" : "!") + "User: " + id + " From: [" + add + ":" + socket.getSocket().getPort() + "]" + message);
    }

    public void send(String target, String msg) throws IOException, IllegalArgumentException {
        TcpSocket socket = manager.get(target);
        if (socket == null) {
            throw new IllegalArgumentException("连接不存在");
        }
        write(socket, msg);
    }

    public void close() throws IOException {
        server.shutdown();
    }

    public void close(String key) throws IOException {
        TcpSocket socket = manager.get(key);
        if (socket != null) {
            manager.del(socket);
            write(socket, "See you later", DataHead.BYE);
            new Thread(() -> {
                try {
                    Thread.sleep(15000);
                    if (!socket.isClosed()) {
                        log.warn("等待超时 强制关闭连接");
                        socket.close();
                    }
                } catch (InterruptedException e) {
                    log.error("线程被关闭");
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }).start();

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
        if (socket == null) {
            throw new IOException("连接不存在");
        }
        write(socket, "Hello World", DataHead.ENCRYPT);
    }

    public void sendFile(String key, File file) throws IOException {
        TcpSocket socket = manager.get(key);
        if (socket == null) {
            throw new IOException("连接不存在");
        }
        sendFileHead(socket, file);
    }
}
