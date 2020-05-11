package cn.erika.handler;

import cn.erika.core.TcpServer;
import cn.erika.core.TcpSocket;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;

public class ServerHandler extends DefaultHandler {
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
        System.out.println("客户端接入: " +
                socket.getSocket().getRemoteSocketAddress().toString());
        String id = manager.add(socket, new Reader(charset, this));
        socket.setAttr(Extra.NICKNAME, id);
        socket.setAttr(Extra.HIDE, false);
    }

    @Override
    public void close(TcpSocket socket) {
        String add = socket.getSocket().getRemoteSocketAddress().toString();
        String id = manager.get(socket);
        try {
            if (!socket.isClosed()) {
                log.info("正在关闭连接 User: " + id + " From: " + add);
                System.out.println("正在关闭连接 User: " + id + " From: " + add);
                socket.close();
            }
        } catch (IOException e) {
            log.warn("连接已经中断");
        } finally {
            log.warn("连接中断 User:" + id + " From: " + add);
            System.out.println("连接中断 User:" + id + " From: " + add);
        }

        manager.del(socket);
    }

    @Override
    public void read(TcpSocket socket, byte[] data, int len) throws IOException {
        manager.getCache(socket).read(socket, data, len);
    }

    @Override
    protected void handler(TcpSocket socket, DataHead head, byte[] data) throws IOException {
        String nickName;
        switch (head.getOrder()) {
            case REG:
                // 注册
                nickName = new String(data, charset);
                if ("".equals(nickName)) {
                    write(socket, "昵称不能为空", DataHead.Order.WARN);
                } else {
                    if (manager.getByNickname(nickName) != null) {
                        write(socket, "昵称已被使用", DataHead.Order.WARN);
                    } else {
                        socket.setAttr(Extra.NICKNAME, nickName);
                        write(socket, "昵称注册成功", DataHead.Order.INFO);
                    }
                }
                break;
            case FIND:
                // 查找
                Set<String> list = manager.linksInfo();
                StringBuilder buffer = new StringBuilder();
                for (String name : list) {
                    boolean hide = manager.get(name).getAttr(Extra.HIDE);
                    if (socket.equals(manager.get(name))) {
                        continue;
                    }
                    if (!hide) {
                        nickName = manager.get(name).getAttr(Extra.NICKNAME);
                        buffer.append(nickName).append(":");
                    }
                }
                if (buffer.length() > 1) {
                    buffer.deleteCharAt(buffer.length() - 1);
                } else {
                    buffer.append("当前没有其他用户");
                }
                write(socket, buffer.toString());
                break;
            case TALK:
                // 聊天
                String tmp = new String(data, charset);
                String nickname = tmp.split(":")[0];
                String message = tmp.substring(nickname.length() + 1);
                TcpSocket dest = manager.getByNickname(nickname);
                if (dest != null) {
                    String from = socket.getAttr(Extra.NICKNAME);
                    write(dest, from + ":" + message);
                } else {
                    write(socket, "未找到昵称: " + nickname);
                }
                break;
            case HIDE:
                // 不允许查找
                socket.setAttr(Extra.HIDE, true);
                write(socket, "已禁止查找");
                break;
            case SEEK:
                // 允许查找
                socket.setAttr(Extra.HIDE, false);
                write(socket, "已允许查找");
                break;
            default:
                log.warn("未知消息头: " + head.show() + "\n内容: " + new String(data, charset));
        }
    }

    @Override
    protected void display(TcpSocket socket, String message) {
        boolean isEncrypt = socket.getAttr(Extra.ENCRYPT);
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
        for (String key : manager.get()) {
            TcpSocket socket = manager.get(key);
            write(socket, "See you later", DataHead.Order.BYE);
            socket.close();
        }
        manager.shutdown();
        server.shutdown();
    }

    public void close(String key) throws IOException {
        TcpSocket socket = manager.get(key);
        if (socket != null) {
            manager.del(socket);
            write(socket, "See you later", DataHead.Order.BYE);
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
        System.out.println("当前接入数量: " + manager.size());
        for (String link : manager.linksInfo()) {
            System.out.println(link);
        }
    }

    public void encrypt(String key) throws IOException {
        log.info("请求加密通信");
        System.out.println("请求加密通信");
        TcpSocket socket = manager.get(key);
        if (socket == null) {
            log.warn("连接不存在");
            System.err.println("连接不存在");
        } else {
            write(socket, "Hello World", DataHead.Order.HELLO_WORLD);
        }
    }

    public void sendFile(String key, File file) throws IOException {
        TcpSocket socket = manager.get(key);
        if (socket == null) {
            log.warn("连接不存在");
            System.err.println("连接不存在");
        } else {
            sendFileHead(socket, file, file.getAbsolutePath());
        }
    }
}
