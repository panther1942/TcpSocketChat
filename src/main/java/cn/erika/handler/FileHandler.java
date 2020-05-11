package cn.erika.handler;

import cn.erika.core.TcpSocket;
import cn.erika.plugins.io.ConfigReader;
import cn.erika.plugins.security.AES;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.charset.Charset;
import java.text.DecimalFormat;

public class FileHandler extends CommonHandler {
    private Logger log = Logger.getLogger(this.getClass().getName());
    private DecimalFormat df = new DecimalFormat("#.00%");
    private String baseDir = ConfigReader.get("baseDir");

    private TcpSocket socket;
    private DefaultHandler handler;
    private Reader reader;

    private File file;
    // 传输文件使用UTF-8编码
    private Charset charset = Charset.forName("UTF-8");

    private boolean encrypt;
    private byte[] pubKey;
    private String password;

    private long fileLength = 0;
    private long filePos = 0;

    private boolean deleteAfterFinished = false;

    /**
     * 暂不使用此构造方法
     * 目前使用的是Socket连接复用
     * 因此不需要创建新的Socket连接
     * <p>
     * 客户端目前不监听端口
     * 如果需要使用不复用Socket连接传输文件
     * 则需要让客户端也监听端口才可以
     */
    FileHandler() {

    }

    /**
     * 复用Socket连接传输文件
     * 在传输文件的过程中不能传输其他数据
     * 即独占该条线路
     *
     * @param socket              传输文件使用的Socket连接
     * @param handler             原先的处理器,传输完成需要交回控制权
     * @param filename            传输的文件名
     * @param fileLength          传输的文件长度
     * @throws IOException 如果传输过程发生错误则抛出该异常
     */
    FileHandler(TcpSocket socket, DefaultHandler handler, String filename, long fileLength) throws IOException {
        this.reader = new Reader(charset, this);
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
        reader.read(socket, data, len);
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
