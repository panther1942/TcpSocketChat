package cn.erika;

import cn.erika.core.*;
import cn.erika.handler.ClientHandler;
import cn.erika.handler.ServerHandler;
import cn.erika.plugins.io.ConfigReader;
import cn.erika.plugins.io.GeneralInput;
import cn.erika.plugins.io.KeyboardReader;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

public class Main {
    private static Logger log = Logger.getLogger(Main.class);
    private static GeneralInput input = KeyboardReader.getInstance();

    public static void main(String[] args) throws IOException, SQLException, NoSuchAlgorithmException {
        System.out.println("Hello world");
//        DataServer.initDb();

        String tip = "s : 以服务器的方式启动\n" +
                "c : 以客户端的方式启动\n";

        String function = null;
        while (!"e".equalsIgnoreCase(function = input.read(tip))) {
            switch (function) {
                case "s":
                    server(ConfigReader.charset());
                    break;
                case "c":
                    client(ConfigReader.charset());
                    break;
                default:
                    log.warn("命令无效");
            }
            log.info("返回主界面");
        }
    }

    private static void server(Charset charset) throws IOException {
        // 读配置文件
        String host = ConfigReader.get("listen_addr");
        int port = Integer.parseInt(ConfigReader.get("listen_port"));
        int capacity = Integer.parseInt(ConfigReader.get("wait_capacity"));

        // 新建对象
        ServerHandler handler = new ServerHandler(charset);
        TcpServer server = new TcpServer(host, port, capacity, handler);
        // 尝试启动
        try {
            server.listen();
            // 命令解析部分
            String line;
            while ((line = input.read()) != null && !"EXIT".equalsIgnoreCase(line)) {
                try {
                    String command = line.split("#")[0];
                    String[] tmp = line.substring(command.length()).split(":");
                    switch (command) {
                        case "show":
                            handler.display();
                            break;
                        case "send":
                            String dest = tmp[0];
                            String msg = tmp[1];
                            handler.send(dest, msg);
                            break;
                        case "kill":
                            String client = line.substring(4);
                            handler.close(client);
                            break;
                        default:
                            log.warn("不支持的命令");
                    }
                } catch (StringIndexOutOfBoundsException | ArrayIndexOutOfBoundsException e) {
                    log.warn("命令无效");
                }
            }
            // 循环结束关闭服务器
            server.shutdown();
            log.info("关闭服务器");
        } catch (IOException e) {
            log.error(e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private static void client(Charset charset) {
        // 读取配置文件
        String host = ConfigReader.get("listen_addr");
        int port = Integer.parseInt(ConfigReader.get("listen_port"));
        int cacheSize = Integer.parseInt(ConfigReader.get("cache_size"));
        // 新建对象
        ClientHandler handler = new ClientHandler(charset, cacheSize);
        TcpClient client = new TcpClient(host, port, handler);
        try {
            // 尝试启动
            client.connect();
            //命令解析部分
            String line;
            while ((line = input.read()) != null && !"EXIT".equalsIgnoreCase(line) && !client.isClosed()) {
                try {
                    String command = line.split("#")[0];
                    String msg = line.substring(command.length());
                    switch (command) {
                        case "send":
                            handler.send(msg);
                            break;
                        case "encrypt":
                            handler.encrypt();
                            break;
                        default:
                            log.warn("不支持的命令");
                    }
                } catch (StringIndexOutOfBoundsException | ArrayIndexOutOfBoundsException e) {
                    log.warn("命令无效");
                }
            }
            // 循环结束关闭客户端
            client.shutdown();
            log.info("关闭客户端");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
