package cn.erika.handler;

import cn.erika.core.Reader;
import cn.erika.core.TcpClient;
import cn.erika.core.TcpSocket;
import cn.erika.plugins.security.AES;
import cn.erika.plugins.security.RSA;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class DefaultClient extends DefaultHandler {
    // TcpClient对象用来管理TcpSocket对象
    private TcpClient client;
    // TcpSocket对象持有一个Socket对象
    private TcpSocket socket;
    // Reader用来解析数据
    private Reader reader;
    // UDP的连接对象
    private DatagramSocket udpSocket;
    // UDP的监听端口
    private String udpPort;
    // 存储聊天用户的UDP地址
    private HashMap<String, InetSocketAddress> addMap;

    // 对外提供的功能
    public enum Function {
        SEND(0x00),
        FILE(0x01),
        ENCRYPT(0x02),
        FIND(0x03),
        NAME(0x04),
        UDP(0x05);

        private int code;

        Function(int code) {
            this.code = code;
        }
    }

    /**
     * 唯一构建客户端处理器的方法,执行一些初始化操作
     *
     * @throws IOException 如果在构建对象的过程中发生异常,则终止初始化
     */
    public DefaultClient() throws IOException {
        client = new TcpClient(this);
        udpSocket = new DatagramSocket();
        addMap = new HashMap<>();
    }

    /**
     * 连接服务器
     *
     * @param address 服务器的地址
     * @throws IOException 如果无法连接服务器则抛出该异常
     */
    public void connect(InetSocketAddress address) throws IOException {
        log.info("尝试连接服务器 [" + address.getHostName() + ":" + address.getPort() + "]");
        System.out.println("尝试连接服务器 [" + address.getHostName() + ":" + address.getPort() + "]");
        this.client.connect(address);
    }

    @Override
    public void accept(TcpSocket socket) throws IOException {
        log.info("成功连接到服务器");
        System.out.println("成功连接到服务器");
        this.socket = socket;
        reader = new DefaultReader(charset, socket, this);
        // UDP的端口号无法通过getPort获取
        this.udpPort = udpSocket.getLocalSocketAddress().toString().split(":")[1];
        // 顺手开个UDP玩玩
        Thread udpReceiver = new Thread(() -> {
            byte[] recBuf = new byte[4096];
            DatagramPacket rec = new DatagramPacket(recBuf, recBuf.length);
            while (true) {
                try {
                    udpSocket.receive(rec);
                    String remote = rec.getSocketAddress().toString();
                    String msg = new String(rec.getData(), rec.getOffset(), rec.getLength());
                    System.out.println("<UDP> " + remote + ": " + msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        udpReceiver.setDaemon(true);
        udpReceiver.start();
    }

    /**
     * 这里是数据的解析工作,交给Reader对象进行数据解析
     *
     * @param socket 对应的Socket对象
     * @param data   读取到的字节 注意 传输的数组的长度为缓冲区的大小 而不是读取到的字节数
     * @param len    读取到的字节数
     * @throws IOException 如果数据解析过程中出现非法数据或者连接异常,则抛出该异常
     */
    @Override
    public void read(TcpSocket socket, byte[] data, int len) throws IOException {
        reader.process(data, len);
    }

    @Override
    protected void handler(TcpSocket socket, DataHead head, byte[] data) throws IOException {
        String[] tmp;
        String message = new String(data, charset);

        switch (head.getOrder()) {
            case HELLO_WORLD:
                log.info("服务器要求加密通信");
                System.out.println("服务器要求加密通信");
                write(socket, keyPair[0], DataHead.Order.ENCRYPT);
                break;
            // 收到对方公钥
            case ENCRYPT_RSA:
                log.debug("收到对方公钥");
                socket.setAttr(Extra.PUBLIC_KEY, data);
                String password = AES.randomPassword(20);
                socket.setAttr(Extra.PASSWORD, password);
                try {
                    log.debug("发送AES秘钥: [" + password + "]");
                    write(socket, RSA.encryptByPublicKey(password.getBytes(charset), data), new DataHead(DataHead.Order.ENCRYPT_AES));
                    socket.setAttr(Extra.ENCRYPT, true);
                    System.out.println("通信已加密");
                } catch (Exception e) {
                    log.error(e.getMessage());
                    write(socket, "客户端不支持这种加密方式: AES", DataHead.Order.LOG_ERROR);
                    e.printStackTrace();
                }
                break;
            case FILE_RECEIVE_FINISHED:
                log.info("文件传输完成: " + message);
                System.out.println("文件传输完成: " + message);
                if (getFlag(Action.CLOSE_AFTER_FINISHED)) {
                    close();
                }
                break;
            case NICKNAME_SUCCESS:
                System.out.println(message);
                break;
            case NICKNAME_FAILED:
                System.out.println("昵称注册失败: " + message);
                break;
            case UDP_NOT_FOUNT:
                log.info("未找到用户: " + message);
                System.out.println("未找到用户: " + message);
                if (addMap.containsKey(message)) {
                    addMap.remove(message);
                }
                break;
            case UDP_REMOTE_ADDRESS:
                log.info("远端地址: " + message);
                System.out.println("远端地址: " + message);
                tmp = message.split("#");
                String dest = tmp[0];
                String add = tmp[1].split(":")[0];
                String port = tmp[1].split(":")[1];
                if (!addMap.containsKey(dest)) {
                    write(socket, dest + "#" + udpPort, DataHead.Order.UDP);
                }
                addMap.put(dest, new InetSocketAddress(add, Integer.parseInt(port)));
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

    public void service(Function function, String... args) throws IOException {
        StringBuffer buffer;
        switch (function) {
            case SEND:
                buffer = new StringBuffer();
                for (int i = 2; i < args.length; i++) {
                    buffer.append(args[i]);
                    buffer.append(" ");
                }
                if (args[1].equals("server")) {
                    write(socket, buffer.toString());
                } else {
                    write(socket, args[1] + ":" + buffer.toString(), DataHead.Order.FORWARD);
                }
                break;
            case FILE:
                File file = new File(args[1]);
                if (args.length > 2) {
                    String filename = args[2];
                    sendFileHead(socket, file, filename);
                } else {
                    sendFileHead(socket, file, file.getAbsolutePath());
                }
                break;
            case ENCRYPT:
                log.info("请求加密通信");
                System.out.println("请求加密通信");
                write(socket, keyPair[0], DataHead.Order.ENCRYPT);
                break;
            case FIND:
                if (args.length == 1) {
                    write(socket, "", DataHead.Order.FIND);
                } else {
                    write(socket, args[1] + "#" + udpPort, DataHead.Order.UDP);
                    addMap.put(args[1], null);
                }
                break;
            case NAME:
                write(socket, args[1], DataHead.Order.NICKNAME);
                break;
            case UDP:
                if (addMap.containsKey(args[1])) {
                    buffer = new StringBuffer();
                    for (int i = 2; i < args.length; i++) {
                        buffer.append(args[i]);
                        buffer.append(" ");
                    }
                    byte[] data = buffer.toString().getBytes("UTF-8");
                    DatagramPacket packet = new DatagramPacket(
                            data,
                            data.length,
                            addMap.get(args[1])
                    );
                    udpSocket.send(packet);
                } else {
                    System.out.println("未获得目标地址");
                }
                break;
            default:
                System.out.println("不支持的命令");
        }
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    public void close() {
        log.info("即将关闭客户端");
        System.out.println("即将关闭客户端");
        close(socket);
    }

    public void tip() {
        System.out.println("\n" +
                "send 发送消息给服务器\n" +
                "     发送消息给指定的连接\n" +
                "  例:send server Hello World\n" +
                "     send id0 Hello World\n" +
                "file 发送文件给服务器 (文件名不要出现#)\n" +
                "  例:file /var/www/html/index.html\n" +
                "     file /var/www/html/index.html index\n" +
                "encrypt 请求加密通信\n" +
                "find 显示所有在线用户或者查找指定用户的UDP端口\n" +
                "  例:find id0\n" +
                "name 注册昵称\n" +
                "udp 使用UDP协议向某个用户发送信息" +
                "exit 退出\n");
    }
}