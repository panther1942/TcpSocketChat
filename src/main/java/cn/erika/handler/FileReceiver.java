package cn.erika.handler;

import cn.erika.core.Reader;
import cn.erika.core.TcpSocket;
import cn.erika.plugins.io.ConfigReader;
import cn.erika.plugins.security.AES;

import java.io.*;
import java.text.DecimalFormat;

/**
 * 接收文件的处理器,目前采用复用连接的方式传输文件,在文件传输过程中不要干扰运行
 */
public class FileReceiver extends CommonHandler {
    // 用于格式化文件传输进度
    private DecimalFormat df = new DecimalFormat("#.00%");
    // 设置文件的默认存储位置
    private String baseDir = ConfigReader.get("baseDir");

    // 复用的Socket连接
    private TcpSocket socket;
    // 之前的处理器
    private DefaultHandler handler;
    // 数据解析器 这里依然使用那一套通信协议 所以还需要使用Reader来解析数据
    private Reader reader;

    private File file;

    // 通信是否加密
    private boolean encrypt;
    // 服务器公钥
    private byte[] pubKey;
    // AES秘钥
    private String password;

    // 文件长度 允许超过2G
    private long fileLength = 0;
    // 传输偏移量
    private long filePos = 0;

    /**
     * 暂不使用此构造方法
     * 目前使用的是Socket连接复用
     * 因此不需要创建新的Socket连接
     * <p>
     * 客户端目前不监听端口
     * 如果需要使用不复用Socket连接传输文件
     * 则需要让客户端也监听端口才可以
     */
    FileReceiver() {

    }

    /**
     * 复用Socket连接传输文件
     * 在传输文件的过程中不能传输其他数据
     * 即独占该条线路
     *
     * @param socket     传输文件使用的Socket连接
     * @param handler    原先的处理器,传输完成需要交回控制权
     * @param filename   传输的文件名
     * @param fileLength 传输的文件长度
     * @throws IOException 如果传输过程发生错误则抛出该异常
     */
    FileReceiver(TcpSocket socket, DefaultHandler handler, String filename, long fileLength) throws IOException {
        this.reader = new DefaultReader(charset, socket,this);
        this.socket = socket;
        this.handler = handler;
        this.file = new File(baseDir + filename);
        this.fileLength = fileLength;
        this.encrypt = socket.getAttr(Extra.ENCRYPT);
        this.pubKey = socket.getAttr(Extra.PUBLIC_KEY);
        if (encrypt) {
            this.password = socket.getAttr(Extra.PASSWORD);
        }

        this.socket.setHandler(this);

        File base = new File(baseDir);
        if (!base.isDirectory() && !base.exists() && !base.mkdirs()) {
            throw new IOException("无法创建下载目录");
        }

        log.info("保存文件在: " + file.getAbsolutePath());
        System.out.println("保存文件在: " + file.getAbsolutePath());
        if (file.exists()) {
            log.warn("文件已存在,即将覆盖: " + file.getAbsolutePath());
            System.out.println("文件已存在,即将覆盖: " + file.getAbsolutePath());
            if (!file.delete()) {
                throw new IOException("无法删除目标文件");
            }
        }
        if (!file.createNewFile() && !file.canWrite()) {
            throw new IOException("无法创建目标文件");
        }
    }

    @Override
    public void init(TcpSocket socket) throws IOException {
        log.debug("当前策略下文件处理器无需初始化socket连接\n" +
                "如果传输文件新开socket连接则需要处理");
    }

    @Override
    public void accept(TcpSocket socket) throws IOException {
        log.debug("当前策略下文件处理器无需初始化socket连接\n" +
                "如果传输文件新开socket连接则需要处理");
    }

    @Override
    public void read(TcpSocket socket, byte[] data, int len) throws IOException {
        reader.process(data, len);
    }

    @Override
    public void deal(TcpSocket socket, DataHead head, byte[] data) throws IOException {
        log.debug("处理数据 长度:" + data.length);
        try (FileOutputStream out = new FileOutputStream(file, true)) {
            if (encrypt) {
                data = AES.decrypt(data, password);
            }
            if (pubKey != null) {
                verify(head.toString().getBytes(charset), pubKey, head.getSign());
            } else {
                log.warn("无签名信息");
            }
            log.debug("解析完成 写入数据");
            log.info("当前进度: " + df.format((head.getPos() + data.length) / (double) fileLength));
            System.out.println("当前进度: " + df.format((head.getPos() + data.length) / (double) fileLength));
            out.write(data, 0, data.length);
            filePos += data.length;
        }
        if (filePos == fileLength) {
            this.socket.setHandler(handler);
            handler.write(this.socket, file.getAbsolutePath(), DataHead.Order.FILE_RECEIVE_FINISHED);
            log.debug("交回控制权");
            log.info("传输完成");
            System.out.println("传输完成");
        }
    }

    @Override
    public void close(TcpSocket socket) {

    }
}
