package cn.erika.handler;

import cn.erika.core.TcpHandler;
import cn.erika.core.TcpSocket;
import cn.erika.plugins.security.AES;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.charset.Charset;
import java.text.DecimalFormat;

public class FileHandler implements TcpHandler, Handler {
    private Logger log = Logger.getLogger(this.getClass().getName());
    private DecimalFormat df = new DecimalFormat("#.00%");

    private TcpSocket socket;
    private TcpHandler handler;
    private Reader reader;

    private File file;
    private Charset charset = Charset.forName("UTF-8");

    private boolean encrypt;
    private String password;

    private long fileLength;
    private long filePos = 0;


    FileHandler(TcpSocket socket, TcpHandler handler, String filename, long fileLength) throws IOException {
        this.reader = new Reader(charset, this);
        this.socket = socket;
        this.handler = handler;
        this.file = new File(filename);
        this.fileLength = fileLength;
        this.encrypt = socket.getAttr(Extra.ENCRYPT);
        if (encrypt) {
            this.password = socket.getAttr(Extra.PASSWORD);
        }

        this.socket.setHandler(this);

        log.info("保存文件在: " + file.getAbsolutePath());
        if (file.exists()) {
            file.delete();
            file.createNewFile();
        }
    }

    @Override
    public void init(TcpSocket socket) throws IOException {

    }

    @Override
    public void accept(TcpSocket socket) throws IOException {

    }

    @Override
    public void close(TcpSocket socket) {

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
            log.debug("解析完成 写入数据");
            log.info("当前进度: " + df.format((head.getPos() + data.length) / (double) fileLength));
            out.write(data, 0, data.length);
            filePos += data.length;
        }
        if (filePos == fileLength) {
            this.socket.setHandler(handler);
            log.debug("交回控制权");
            log.info("传输完成");
        }
    }
}
