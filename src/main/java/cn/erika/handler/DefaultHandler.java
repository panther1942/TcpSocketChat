package cn.erika.handler;

import org.apache.log4j.Logger;
import com.alibaba.fastjson.JSON;

import cn.erika.core.TcpHandler;
import cn.erika.core.TcpSocket;
import cn.erika.plugins.security.AES;
import cn.erika.plugins.security.RSA;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class DefaultHandler implements TcpHandler, Handler {
    public static final int ENCRYPT = 0x100; // 启用加密
    public static final int PUBLIC_KEY = 0x101; // 对方公钥
    public static final int PASSWORD = 0x102; // 加密密码

    protected Logger log = Logger.getLogger(this.getClass().getName()); // 日志
    private DecimalFormat df = new DecimalFormat("#.00%");

    private Cache cache;

    private byte[][] keyPair; // RSA密钥对
    private Charset charset; // 字符编码
    private int cacheSize;

    DefaultHandler(Charset charset) {
        this.charset = charset;
        this.cacheSize = 4096;
        this.cache = new Cache(charset, this);
    }

    DefaultHandler(Charset charset, int cacheSize) {
        this(charset);
        this.cacheSize = cacheSize;
    }

    // 建立Socket连接的时候需要进行初始化
    @Override
    public void init(TcpSocket socket) throws IOException {
        socket.setAttr(TcpSocket.CACHE_SIZE, cacheSize);
        socket.setAttr(ENCRYPT, false);
        try {
            keyPair = RSA.initKey(2048); // 每次启动都会生成不同的RSA秘钥对
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage());
        }

        /*
        这个是从数据库读取配置
        keyPair = DataServer.getKeyPair();
         */
    }

    @Override
    public void read(TcpSocket socket, byte[] data, int len) throws IOException {
        cache.read(socket, data, len);
    }

    @Override
    public void write(TcpSocket socket, byte[] data) throws IOException {
        socket.write(data, data.length);
    }

    void write(TcpSocket socket, String msg) throws IOException {
        DataHead head = new DataHead(DataHead.ASC);
        write(socket, msg.getBytes(charset), head);
    }

    void write(TcpSocket socket, String msg, int order) throws IOException {
        DataHead head = new DataHead(order);
        write(socket, msg.getBytes(charset), head);
    }

    private void write(TcpSocket socket, byte[] data, DataHead head) throws IOException {
        cache.write(socket, data, head);
    }

    public void read(TcpSocket socket, DataHead head, byte[] data) throws IOException {
        boolean encrypt = socket.getAttr(ENCRYPT);
        String password = socket.getAttr(PASSWORD);

        if (encrypt) {
            data = AES.decrypt(data, password);
        }

        String[] msg;
        String filename;
        long length;

        int order = head.getOrder();
        switch (order) {
            // 收到加密请求
            case DataHead.ENCRYPT:
                log.info("对方请求启用加密 发送自身的公钥: " + Base64.getEncoder().encodeToString(keyPair[0]));
                write(socket, keyPair[0], new DataHead(DataHead.RSA));
                socket.setAttr(PUBLIC_KEY, keyPair[0]);
                break;
            // 收到对方公钥
            case DataHead.RSA:
                log.debug("收到对方公钥: " + Base64.getEncoder().encodeToString(data));
                socket.setAttr(PUBLIC_KEY, data);
                password = AES.randomPassword(6);
                log.debug("生成AES秘钥: " + password);
                socket.setAttr(PASSWORD, password);
                try {
                    log.info("发送AES秘钥: " + password);
                    write(socket, RSA.encryptByPublicKey(password.getBytes(charset), data), new DataHead(DataHead.AES));
                    socket.setAttr(ENCRYPT, true);
                } catch (Exception e) {
                    log.error(e.getMessage());
                    write(socket, "客户端不支持这种加密方式: AES", DataHead.ERROR);
                    e.printStackTrace();
                }
                break;
            // 收到对方发送的AES秘钥
            case DataHead.AES:
                try {
                    password = new String(RSA.decryptByPrivateKey(data, keyPair[1]), charset);
                    log.info("加密协商完成");
                    log.debug("AES密钥: " + password);
                    socket.setAttr(PASSWORD, password);
                    socket.setAttr(ENCRYPT, true);
                } catch (Exception e) {
                    log.error(e.getMessage());
                    write(socket, "服务器错误", DataHead.ERROR);
                    e.printStackTrace();
                }
                break;
            // 收到断开信息
            case DataHead.BYE:
                log.info(new String(data, charset));
                close(socket);
                break;
            case DataHead.READY:
                log.debug("服务器准备好接收");
                filename = new String(data, charset).split("\\|")[0];
                log.debug("准备发送文件: " + filename);
                sendFile(socket, new File(filename));
                break;
            // 收到文件
            case DataHead.BIN:
                msg = new String(data, charset).split("\\|");
                filename = msg[0];
                length = Long.parseLong(msg[1]);
                File file = new File(filename);
                log.info("收到文件: " + file.getName());
                new FileHandler(socket, this, file.getName(), length);
                head.setOrder(DataHead.READY);
                log.debug("切换处理器完成");
                write(socket, data, head);
                log.debug("发送就绪信息");
                break;
            // 收到文字
            case DataHead.DEBUG:
            case DataHead.INFO:
            case DataHead.WARN:
            case DataHead.ERROR:
            case DataHead.ASC:
                display(socket, new String(data, charset));
                break;
            default:
                log.warn("未知消息头: " + head.show() + "\n内容: " + new String(data, charset));
        }
    }

    abstract void display(TcpSocket socket, String message);

    void sendFileHead(TcpSocket socket, File file) throws IOException {
        System.out.println("文件名: " + file.getAbsolutePath());
        System.out.println("文件长度: " + file.length());
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            throw new IOException("文件过大: " + length);
        }
        DataHead head = new DataHead(DataHead.BIN);
        String msg = file.getAbsolutePath() + "|" + file.length();
        write(socket, msg.getBytes(charset), head);
    }

    private void sendFile(TcpSocket socket, File file) throws IOException {
        try (FileInputStream in = new FileInputStream(file)) {
            long pos = 0;
            int len;
            byte[] data = new byte[8 * 1024 * 1024];
            log.debug("发送文件");

            while ((len = in.read(data)) > -1) {
                byte[] tmp = new byte[len];
                System.arraycopy(data, 0, tmp, 0, len);
                log.debug("本次发送长度: " + len);
                DataHead head = new DataHead(DataHead.BIN);
                head.setPos(pos);
                write(socket, tmp, head);
                pos += len;
                log.info("进度: " + df.format(pos / (double) file.length()));
            }
            log.debug("发送完成");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
