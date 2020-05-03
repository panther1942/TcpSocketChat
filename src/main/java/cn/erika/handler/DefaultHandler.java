package cn.erika.handler;

import cn.erika.core.Attribute;
import org.apache.log4j.Logger;

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
import java.util.HashMap;

public abstract class DefaultHandler extends CommonHandler {
    private static DecimalFormat df = new DecimalFormat("#.00%");

    // 日志
    protected Logger log = Logger.getLogger(this.getClass().getName());
    // 字符编码 默认UTF-8编码
    Charset charset = Charset.forName("UTF-8");
    // RSA密钥对
    byte[][] keyPair;
    // 缓冲区大小 默认值4K
    private int cacheSize = 4096;

    private HashMap<Integer, Boolean> flags = new HashMap<>();

    public enum Action {
        CLOSE_AFTER_FINISHED(0x01),
        DELETE_FILE_AFTER_FINISHED(0x02);

        private int code;

        Action(int code) {
            this.code = code;
        }
    }

    // 建立Socket连接的时候需要进行初始化
    @Override
    public void init(TcpSocket socket) throws IOException {
        socket.setAttr(Attribute.Standard.CACHE_SIZE, cacheSize);
        socket.setAttr(Extra.ENCRYPT, false);
        try {
            // 每次启动都会生成不同的RSA秘钥对
            keyPair = RSA.initKey(2048);
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage());
        }
    }

    // 向指定用户发送消息
    void write(TcpSocket socket, String msg) throws IOException {
        DataHead head = new DataHead(DataHead.Order.ASC);
        write(socket, msg.getBytes(charset), head);
    }

    // 用于发送消息头
    void write(TcpSocket socket, String msg, DataHead.Order order) throws IOException {
        DataHead head = new DataHead(order);
        write(socket, msg != null ? msg.getBytes(charset) : null, head);
    }

    // 用于交换秘钥
    void write(TcpSocket socket, byte[] data, DataHead.Order order) throws IOException {
        DataHead head = new DataHead(order);
        write(socket, data, head);
    }

    private void write(TcpSocket socket, byte[] data, DataHead head) throws IOException {
        boolean encrypt = socket.getAttr(Extra.ENCRYPT);
        String password = socket.getAttr(Extra.PASSWORD);

        if (!socket.getSocket().isClosed()) {
            if (data != null) {
                if (encrypt) {
                    log.debug("加密传输");
                    data = AES.encrypt(data, password);
                } else {
                    log.debug("明文传输");
                }
                head.setLen(data.length);
            }
            byte[] sign = RSA.sign(head.toString().getBytes(charset), keyPair[1]);
            head.setSign(sign);
            byte[] bHead = head.toString().getBytes(charset);
            byte[] bHeadSign = new byte[DataHead.LEN];
            System.arraycopy(bHead, 0, bHeadSign, 0, 37);
            System.arraycopy(sign, 0, bHeadSign, 37, 256);
            log.debug("头部长度: " + bHeadSign.length);
            socket.write(bHeadSign, bHeadSign.length);
            if (data != null) {
                socket.write(data, data.length);
                log.debug("数据长度: " + data.length);
            }
        } else {
            throw new IOException("连接已被关闭: " + socket.getSocket().getRemoteSocketAddress());
        }
    }

    @Override
    public void deal(TcpSocket socket, DataHead head, byte[] data) throws IOException {
        boolean encrypt = socket.getAttr(Extra.ENCRYPT);
        byte[] pubKey = socket.getAttr(Extra.PUBLIC_KEY);
        String password = socket.getAttr(Extra.PASSWORD);

        if (encrypt) {
            data = AES.decrypt(data, password);
        }
        if (pubKey != null) {
            verify(head.toString().getBytes(charset), pubKey, head.getSign());
        } else {
            log.warn("无签名信息");
        }

        String[] msg;
        String filename;
        long length;

        DataHead.Order order = head.getOrder();
        try {
            switch (order) {
                // 收到加密请求
                case ENCRYPT:
                    log.info("对方请求启用加密: " + socket.getSocket().getRemoteSocketAddress());
                    socket.setAttr(Extra.PUBLIC_KEY, data);
                    log.debug("发送自身的公钥");
                    write(socket, keyPair[0], new DataHead(DataHead.Order.RSA));
                    break;
                // 收到对方公钥
                case RSA:
                    log.debug("收到对方公钥");
                    socket.setAttr(Extra.PUBLIC_KEY, data);
                    password = AES.randomPassword(20);
                    socket.setAttr(Extra.PASSWORD, password);
                    try {
                        log.debug("发送AES秘钥: [" + password + "]");
                        write(socket, RSA.encryptByPublicKey(password.getBytes(charset), data), new DataHead(DataHead.Order.AES));
                        socket.setAttr(Extra.ENCRYPT, true);
                    } catch (Exception e) {
                        log.error(e.getMessage());
                        write(socket, "客户端不支持这种加密方式: AES", DataHead.Order.ERROR);
                        e.printStackTrace();
                    }
                    break;
                // 收到对方发送的AES秘钥
                case AES:
                    try {
                        password = new String(RSA.decryptByPrivateKey(data, keyPair[1]), charset);
                        log.info("加密协商完成");
                        log.debug("收到AES密钥");
                        socket.setAttr(Extra.PASSWORD, password);
                        socket.setAttr(Extra.ENCRYPT, true);
                        write(socket, "加密协商成功", DataHead.Order.INFO);
                    } catch (Exception e) {
                        log.error(e.getMessage());
                        write(socket, "服务器错误", DataHead.Order.ERROR);
                        e.printStackTrace();
                    }
                    break;
                // 收到断开信息
                case BYE:
                    log.info(new String(data, charset));
                    close(socket);
                    break;
                case FILE_RECEIVE_READY:
                    filename = new String(data, charset).split("\\|")[0];
                    log.debug("准备发送文件: " + filename);
                    sendFile(socket, new File(filename));
                    break;
                case FILE_RECEIVE_REFUSE:
                    log.warn("对方拒绝接收文件");
                    break;
                // 收到文件
                case BIN:
                    msg = new String(data, charset).split("\\|");
                    filename = msg[2];
                    length = Long.parseLong(msg[1]);
                    File file = new File(filename);
                    log.info("收到文件: " + file.getName());
                    try {
                        log.debug("切换处理器");
                        new FileHandler(socket, this, file.getName(), length);
                        head.setOrder(DataHead.Order.FILE_RECEIVE_READY);
                    } catch (IOException e) {
                        log.error("发生读写错误: " + e.getMessage());
                        head.setOrder(DataHead.Order.FILE_RECEIVE_REFUSE);
                    } finally {
                        log.debug("发送反馈信息");
                        write(socket, data, head);
                    }
                    break;
                // 收到文字及其他无需处理的消息 直接打印出来
                case DEBUG:
                    log.debug(new String(data, charset));
                    break;
                case INFO:
                    break;
                case WARN:
                    log.warn(new String(data, charset));
                    break;
                case ERROR:
                    log.error(new String(data, charset));
                    break;
                case ASC:
                    display(socket, new String(data, charset));
                    break;
                default:
                    handler(socket, head, data);
            }
        } catch (IOException e) {
            log.error("发生读写错误: " + e.getMessage());
        }
    }

    void sendFileHead(TcpSocket socket, File file, String filename) throws IOException {
        if (!file.exists()) {
            log.info("文件不存在: " + file.getAbsolutePath());
        } else if (!file.canRead()) {
            log.info("文件不可读: " + file.getAbsolutePath());
        } else {
            System.out.println("文件完整路径: " + file.getAbsolutePath());
            System.out.println("文件名: " + filename);
            System.out.println("文件长度: " + file.length());
            DataHead head = new DataHead(DataHead.Order.BIN);
            String msg = file.getAbsolutePath() + "|" + file.length() + "|" + filename;
            write(socket, msg.getBytes(charset), head);
        }
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
                DataHead head = new DataHead(DataHead.Order.BIN);
                head.setPos(pos);
                write(socket, tmp, head);
                pos += len;
                log.info("进度: " + df.format(pos / (double) file.length()));
            }
            log.debug("发送完成");
            if (getFlag(Action.DELETE_FILE_AFTER_FINISHED)) {
                log.info("删除原始文件: " + file.getAbsolutePath());
                if (file.delete()) {
                    log.info("原始文件已删除");
                } else {
                    log.warn("无法删除原始文件");
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public boolean getFlag(Action action) {
        Boolean flag = this.flags.get(action.code);
        return flag == null ? false : flag;
    }

    public void setFlag(Action action, boolean flag) {
        this.flags.put(action.code, flag);
    }

    public HashMap<Integer, Boolean> getFlags() {
        return flags;
    }

    protected abstract void handler(TcpSocket socket, DataHead head, byte[] data) throws IOException;

    protected abstract void display(TcpSocket socket, String message);
}
