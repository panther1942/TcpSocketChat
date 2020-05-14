package cn.erika.handler;

import java.util.Date;

/**
 * 因为消息不定长,不能保证每次发送和接收消息的大小正好是缓冲区的大小,
 * 所以需要数据头来分片传输,数据头用来记录当前信息的长度,偏移量,指令等信息
 */
public class DataHead {
    // 指令信息,用来标记本次信息的用途
    public enum Order {
        // 服务器要求客户端加密通信
        HELLO_WORLD(0x00),
        // 本次消息为普通文本信息
        ASC(0x01),
        // 本次消息为不可读二进制信息(文件)
        BIN(0x02),
        LOG_DEBUG(0x03),
        LOG_INFO(0x04),
        LOG_WARN(0x05),
        LOG_ERROR(0x06),
        ENCRYPT_CONFIRM(0x07),
        // 客户端请求加密,服务器发送自身公钥
        ENCRYPT(0x10),
        // 服务器返回自身的公钥,客户端使用公钥加密AES秘钥
        ENCRYPT_RSA(0x11),
        // 服务器拿到AES秘钥,并标记该连接启用加密
        ENCRYPT_AES(0x12),
        // 文件接收方获取文件头信息,并准备好接收文件
        FILE_RECEIVE_READY(0x20),
        // 文件接收方因某些原因拒绝接收文件
        FILE_RECEIVE_REFUSE(0x21),
        // 文件接收方完成文件接收
        FILE_RECEIVE_FINISHED(0x22),
        // 客户端用来查找在线用户的Nickname/指定用户的UDP端口
        FIND(0x30),
        // 客户端使用服务器转发向另一用户发送文本信息
        FORWARD(0x31),
        // 客户端请求注册Nickname
        NICKNAME(0x32),
        // 服务器同意注册该Nickname
        NICKNAME_SUCCESS(0x33),
        // 服务器拒绝注册该Nickname
        NICKNAME_FAILED(0x34),
        // 客户端设置允许查找自己
        NICKNAME_ALLOW_FIND(0x35),
        // 客户端设置禁止查找自己
        NICKNAME_REFUSE_FIND(0x36),
        // 使用UDP向某个用户发送文本信息(未经加密)
        UDP(0x40),
        // 服务器返回请求用户的UDP端口
        UDP_REMOTE_ADDRESS(0x41),
        // 服务器上不存在该用户,返回错误信息
        UDP_NOT_FOUNT(0x42),
        // 客户端离线时发送该请求通知服务器
        BYE(0xFF),;

        public int value;

        Order(int value) {
            this.value = value;
        }

        public static Order getByValue(int value) {
            for (Order order : Order.values()) {
                if (order.value == value) {
                    return order;
                }
            }
            return null;
        }
    }

    // 时间戳(13)+数据偏移量(10)+数据长度(10)+指令(4)+RSA签名(256)
    // 签名校验仅限加密通信使用
    static final int LEN = 13 + 10 + 10 + 4 + 256;

    // 时间戳 13
    private Date timestamp;
    // 数据偏移量 10
    private long pos = 0;
    // 数据长度 10
    private int len = 0;
    // 指令 4
    private Order order = Order.ASC;
    // 签名 256
    private byte[] sign;

    public DataHead() {
        this.timestamp = new Date();
    }

    public DataHead(Order order) {
        this();
        this.order = order;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public long getPos() {
        return pos;
    }

    public void setPos(long pos) {
        this.pos = pos;
    }

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public void setOrder(int order) {
        this.order = Order.getByValue(order);
    }

    public byte[] getSign() {
        return sign;
    }

    public void setSign(byte[] sign) {
        this.sign = sign;
    }

    @Override
    public String toString() {
        String timestamp = String.format("%13s", this.timestamp.getTime()).replaceAll(" ", "0");
        String pos = String.format("%10s", this.pos).replaceAll(" ", "0");
        String len = String.format("%10s", this.len).replaceAll(" ", "0");
        String order = String.format("%4s", Integer.toString(this.order.value)).replaceAll(" ", "0");
        return timestamp + pos + len + order;
    }

    public String show() {
        return "DataHead{" +
                "timestamp=" + timestamp +
                ", pos=" + pos +
                ", len=" + len +
                ", order=" + order +
                '}';
    }
}
