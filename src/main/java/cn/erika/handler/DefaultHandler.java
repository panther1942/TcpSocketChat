package cn.erika.handler;

import org.apache.log4j.Logger;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;

import cn.erika.core.TcpHandler;
import cn.erika.core.TcpSocket;
import cn.erika.plugins.security.AES;
import cn.erika.plugins.security.RSA;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class DefaultHandler implements TcpHandler {
    // 用于匹配JSON数据
    private static String regex = "^(\\{[^}]*\\})";
    private static Pattern pattern = Pattern.compile(regex);

    public static final int ENCRYPT = 0x100; // 启用加密
    public static final int PUBLIC_KEY = 0x101; // 对方公钥
    public static final int PASSWORD = 0x102; // 加密密码


    private byte[][] keyPair; // RSA密钥对
    protected Logger log = Logger.getLogger(this.getClass().getName()); // 日志

    private Charset charset; // 字符编码
    private byte[] cache = new byte[0]; // 缓冲区
    private int pos = 0; // 游标偏移量 标记有效数据的最后位置
    private DataHead head; // 数据头

    private int cacheSize;


    DefaultHandler(Charset charset) {
        this.charset = charset;
        this.cacheSize = 4096;
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
        // 如果缓冲区的长度为0 说明这次的数据是数据头
        if (cache.length == 0) {
            // 数据头不会使用AES加密 所以直接Base64解码
            // 可能和后续数据混在一起 需要用正则判断
            // 传来的data长度和有效数据长度(len)不相同
            // data长度应该和Socket缓冲区大小相同
            // 因此需要将有效部分复制出来再进行Base64解码
            byte[] tmp = new byte[len];
            System.arraycopy(data, 0, tmp, 0, len);
            String msg = new String(Base64.getDecoder().decode(tmp), charset);
            Matcher m = pattern.matcher(msg);
            try {
                if (m.find()) {
                    // 如果能匹配上 说明是合法数据 剔除掉数据头后将剩余的数据扔回缓冲区等传输完整后交给具体的handler处理
                    // 设置好数据头
                    head = JSON.parseObject(m.group(1), DataHead.class);
                    // 计算数据头在编码后的长度 然后将缓冲器包含数据头的部分去掉
                    byte[] bHead = Base64.getEncoder().encode(m.group(1).getBytes(charset));
                    int headLen = bHead.length;
                    // 收到数据头之后重置缓冲区
                    pos = 0;
                    // 数据头中包含这次传输的总数据长度 根据这个长度构建缓冲区
                    cache = new byte[head.getLen()];
                    if (len > headLen) {
                        // 如果缓冲区的长度比数据头长 说明需要将包含数据头的部分去除再加入缓冲区 否则不需要处理
                        System.arraycopy(data, headLen, cache, pos, len - headLen);
                        // 修正游标偏移量到有效数据最后的位置
                        pos += len - headLen;
                    }
                } else {
                    // 如果没有配上 说明这次数据没有数据头 无法处理
                    log.warn("非法数据: " + msg);
                    pos = 0;
                    cache = new byte[0];
                }
            } catch (JSONException e) {
                log.error(e.getMessage());
                cache = new byte[0];
            }
        } else {
            // 如果缓冲区长度不为0 就要把数据填进缓冲区
            System.arraycopy(data, 0, cache, pos, len);
            pos += len;
        }
        // 如果游标的位置到达缓冲区的末尾 说明数据接收完成 Base64解码后交给具体的handler处理
        if (pos == cache.length) {
            // 将数据解码 并归零缓冲区
            byte[] tmp = Base64.getDecoder().decode(cache);
            cache = new byte[0];
            try {
                // 将数据交给具体的handler处理
                handler(socket, head, tmp);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 交给用户层使用
    @Override
    public void write(TcpSocket socket, String msg) throws IOException {
        write(socket, msg.getBytes(charset), DataHead.Type.NONE);
    }

    // 系统命令调用
    void write(TcpSocket socket, String msg, DataHead.Type order) throws IOException {
        write(socket, msg.getBytes(charset), order);
    }

    // 所有写方法最终调用该方法发出消息
    void write(TcpSocket socket, byte[] data, DataHead.Type order) throws IOException {
        boolean encrypt = socket.getAttr(ENCRYPT);
        String password = socket.getAttr(PASSWORD);

        // 如果socket没有关闭 则尝试发送消息
        if (!socket.getSocket().isClosed()) {
            // 如果启用加密的话 先则使用AES加密要发送的消息
            if (encrypt) {
                try {
                    data = AES.encrypt(data, password);
                } catch (Exception e) {
                    throw new IOException("加密发生错误");
                }
            }
            // 将要发送的数据进行Base64编码
            data = Base64.getEncoder().encode(data);
            // 构建数据头 并转成JSON格式
            byte[] head = JSON.toJSONString(new DataHead(data.length, order)).getBytes(charset);
            // 为了提高代码可读 和上一行拆成两行写 可以写成一行 进行Base64编码
            head = Base64.getEncoder().encode(head);
            // 先发送数据头 再发数据
            socket.write(head, head.length);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            socket.write(data, data.length);
        } else {
            throw new IOException("连接已被关闭: " + socket.getSocket().getRemoteSocketAddress());
        }
    }

    private void handler(TcpSocket socket, DataHead head, byte[] data) throws Exception {
        /*log.debug("消息长度: " + head.getLen() + " 字节");
        log.debug("消息(Base64): " + Base64.getEncoder().encodeToString(data));
        log.debug("消息: " + new String(data, charset));*/

        boolean encrypt = socket.getAttr(ENCRYPT);
        String password = socket.getAttr(PASSWORD);

        if (encrypt) {
            data = AES.decrypt(data, password);
        }
        switch (head.getOrder()) {
            case ENCRYPT:
                log.info("对方请求启用加密 发送自身的公钥");
                write(socket, keyPair[0], DataHead.Type.RSA);
                socket.setAttr(PUBLIC_KEY, keyPair[0]);
                break;
            case RSA:
                log.debug("收到对方公钥: " + Base64.getEncoder().encodeToString(data));
                socket.setAttr(PUBLIC_KEY, data);
                password = AES.randomPassword(6);
                log.debug("生成AES秘钥: " + password);
                socket.setAttr(PASSWORD, password);
                try {
                    log.info("发送AES秘钥: " + password);
                    write(socket, RSA.encryptByPublicKey(password.getBytes(charset), data), DataHead.Type.AES);
                    socket.setAttr(ENCRYPT, true);
                } catch (Exception e) {
                    log.error(e.getMessage());
                    write(socket, "客户端不支持这种加密方式: AES", DataHead.Type.ERROR);
                    e.printStackTrace();
                }
                break;
            case AES:
                try {
                    password = new String(RSA.decryptByPrivateKey(data, keyPair[1]), charset);
                    log.info("加密协商完成");
                    log.debug("AES密钥: " + password);
                    socket.setAttr(PASSWORD, password);
                    socket.setAttr(ENCRYPT, true);
                } catch (Exception e) {
                    log.error(e.getMessage());
                    write(socket, "服务器错误", DataHead.Type.ERROR);
                    e.printStackTrace();
                }
                break;
            case BYE:
                System.err.println(new String(data, charset));
                close(socket);
                break;
            case NONE:
            default:
                display(socket, new String(data, charset));
        }
    }

    abstract void display(TcpSocket socket, String message);
}
