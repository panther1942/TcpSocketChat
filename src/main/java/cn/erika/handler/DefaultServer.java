package cn.erika.handler;

import cn.erika.core.Reader;
import cn.erika.core.TcpServer;
import cn.erika.core.TcpSocket;
import cn.erika.plugins.security.RSA;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

public class DefaultServer extends DefaultHandler {
    private TcpServer server;
    // 连接管理器
    private SocketManager manager = new SocketManager();

    // 对外提供的功能
    public enum Function {
        SEND(0x00),
        FILE(0x01),
        ENCRYPT(0x02),
        SHOW(0x03),
        KILL(0x04);

        private int code;

        Function(int code) {
            this.code = code;
        }
    }

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
        String id = manager.add(socket, new DefaultReader(charset, socket, this));
        socket.setAttr(Extra.NICKNAME, id);
        socket.setAttr(Extra.HIDE, false);
    }

    /**
     * @param socket 对应的Socket对象
     * @param data   读取到的字节 注意 传输的数组的长度为缓冲区的大小 而不是读取到的字节数
     * @param len    读取到的字节数
     * @throws IOException
     */
    @Override
    public synchronized void read(TcpSocket socket, byte[] data, int len) throws IOException {
        manager.getReader(socket).process(data, len);
    }

    @Override
    protected void handler(TcpSocket socket, DataHead head, byte[] data) throws IOException {
        String nickName;
        TcpSocket dest = null;
        switch (head.getOrder()) {
            // 收到加密请求
            case ENCRYPT:
                log.info("对方请求启用加密: " + socket.getSocket().getRemoteSocketAddress());
                socket.setAttr(Extra.PUBLIC_KEY, data);
                log.debug("发送自身的公钥");
                write(socket, keyPair[0], new DataHead(DataHead.Order.ENCRYPT_RSA));
                break;
            // 收到对方发送的AES秘钥
            case ENCRYPT_AES:
                try {
                    String password = new String(RSA.decryptByPrivateKey(data, keyPair[1]), charset);
                    log.info("加密协商完成");
                    log.debug("收到AES密钥");
                    socket.setAttr(Extra.PASSWORD, password);
                    socket.setAttr(Extra.ENCRYPT, true);
                    write(socket, "加密协商成功", DataHead.Order.ENCRYPT_CONFIRM);
                    System.out.println("通信已加密");
                } catch (Exception e) {
                    log.error(e.getMessage());
                    write(socket, "服务器错误", DataHead.Order.LOG_ERROR);
                    e.printStackTrace();
                }
                break;
            case NICKNAME:
                // 注册
                nickName = new String(data, charset);
                if ("".equals(nickName)) {
                    write(socket, "昵称不能为空", DataHead.Order.NICKNAME_FAILED);
                } else {
                    if (manager.getByNickname(nickName) != null) {
                        write(socket, "昵称已被使用", DataHead.Order.NICKNAME_FAILED);
                    } else {
                        socket.setAttr(Extra.NICKNAME, nickName);
                        write(socket, "昵称注册成功", DataHead.Order.NICKNAME_SUCCESS);
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
            case FORWARD:
                // 使用服务器中继向某用户发送文本信息
                String tmp = new String(data, charset);
                String nickname = tmp.split(":")[0];
                String message = tmp.substring(nickname.length() + 1);
                dest = manager.getByNickname(nickname);
                if (dest != null) {
                    log.info("服务器转发消息: \nsrc[" + socket.getSocket().getRemoteSocketAddress() + "]\n" +
                            "dst[" + dest.getSocket().getRemoteSocketAddress() + "]\n" +
                            "msg[" + message + "]");
                    String from = socket.getAttr(Extra.NICKNAME);
                    write(dest, from + ": " + message);
                } else {
                    write(socket, "未找到昵称: " + nickname);
                }
                break;
            case NICKNAME_ALLOW_FIND:
                // 允许查找
                socket.setAttr(Extra.HIDE, false);
                write(socket, "已允许查找");
                break;
            case NICKNAME_REFUSE_FIND:
                // 不允许查找
                socket.setAttr(Extra.HIDE, true);
                write(socket, "已禁止查找");
                break;
            case UDP:
                String msg = new String(data, charset);
                dest = manager.get(msg.split("#")[0]);
                int port = Integer.parseInt(msg.split("#")[1]);
                if (dest != null) {
                    String src = manager.get(socket);
                    String srcRemote = src + "#" + socket.getSocket().getRemoteSocketAddress().toString().substring(1).split(":")[0] + ":" + port;
                    write(dest, srcRemote, DataHead.Order.UDP_REMOTE_ADDRESS);
                } else {
                    write(socket, msg, DataHead.Order.UDP_NOT_FOUNT);
                }
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

    public void service(Function function, String... args) throws IOException {
        TcpSocket socket;
        switch (function) {
            case SEND:
                socket = manager.get(args[1]);
                if (socket == null) {
                    throw new IllegalArgumentException("连接不存在");
                }
                StringBuffer buf = new StringBuffer();
                for (int i = 2; i < args.length; i++) {
                    buf.append(args[i]);
                    buf.append(" ");
                }
                write(socket, buf.toString());
                break;
            case FILE:
                socket = manager.get(args[1]);
                if (socket == null) {
                    log.warn("连接不存在");
                    System.err.println("连接不存在");
                } else {
                    File file = new File(args[2]);
                    sendFileHead(socket, file, file.getAbsolutePath());
                }
                break;
            case ENCRYPT:
                log.info("请求加密通信");
                System.out.println("请求加密通信");
                socket = manager.get(args[1]);
                if (socket == null) {
                    log.warn("连接不存在");
                    System.err.println("连接不存在");
                } else {
                    write(socket, "Hello World", DataHead.Order.HELLO_WORLD);
                }
                break;
            case KILL:
                socket = manager.get(args[1]);
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
                break;
            case SHOW:
                System.out.println("当前接入数量: " + manager.size());
                for (String link : manager.linksInfo()) {
                    System.out.println(link);
                }
                break;
            default:
                System.out.println("不支持的命令");
        }
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

    public void tip() {
        System.out.println("\nshow 显示当前接入的连接\n" +
                "send 发送消息给指定的连接\n" +
                "  例:send id0 Hello World\n" +
                "file 发送文件给指定的连接\n" +
                "  例:file id0 /var/www/html/index.html\n" +
                "     file /var/www/html/index.html index\n" +
                "encrypt 请求加密通信\n" +
                "  例:encrypt id0" +
                "kill 强制指定的连接下线\n" +
                "exit 退出\n");
    }

    private class SocketManager {
        private Logger log = Logger.getLogger(this.getClass().getName());

        private HashMap<String, TcpSocket> links = new HashMap<>();
        private HashMap<TcpSocket, Reader> cachePool = new HashMap<>();
        private int order = 0;

        private Timer timer = new Timer();

        SocketManager() {
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    log.debug("开始运行定时清理程序");
                    int count;
                    synchronized (this) {
                        Iterator<Map.Entry<String, TcpSocket>> it = links.entrySet().iterator();
                        count = 0;
                        while (it.hasNext()) {
                            Map.Entry<String, TcpSocket> en = it.next();
                            TcpSocket socket = en.getValue();
                            if (socket.isClosed()) {
                                it.remove();
                                count++;
                            }
                        }
                    }
                    log.info("清除无效连接[" + count + "]");
                }
            };
            timer.schedule(task, 15000, 15000);
        }

        void shutdown() throws IOException {
            timer.cancel();
        }

        synchronized String add(TcpSocket socket, Reader reader) throws IOException {
            String id = "id" + this.order++;
            links.put(id, socket);
            cachePool.put(socket, reader);
            return id;
        }

        /**
         * 用于关闭客户端连接
         *
         * @param socket 客户端连接对应的TcpSocket对象 应该从Socket管理程序取出
         */
        synchronized void del(TcpSocket socket) {
            String target = get(socket);
            if (target != null) {
                links.remove(target);
            }
        }

        TcpSocket get(String key) {
            return this.links.get(key);
        }

        TcpSocket getByNickname(String nickname) {
            for (String key : links.keySet()) {
                String _nickname = links.get(key).getAttr(CommonHandler.Extra.NICKNAME);
                if (_nickname.equals(nickname)) {
                    return this.links.get(key);
                }
            }
            return null;
        }

        Set<String> get() {
            return links.keySet();
        }

        String get(TcpSocket socket) {
            for (String key : links.keySet()) {
                if (links.get(key) == socket) {
                    return key;
                }
            }
            return null;
        }

        Reader getReader(TcpSocket socket) {
            return cachePool.get(socket);
        }

        int size() {
            return links.size();
        }

        Set<String> linksInfo() {
            return links.keySet();
        }
    }
}
