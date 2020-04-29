package cn.erika;

import cn.erika.handler.ClientHandler;
import cn.erika.handler.ServerHandler;
import cn.erika.plugins.io.ConfigReader;
import cn.erika.plugins.io.GeneralInput;
import cn.erika.plugins.io.KeyboardReader;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

public class Main {
    private static Logger log = Logger.getLogger(Main.class);
    private static GeneralInput input = KeyboardReader.getInstance();

    public static void main(String[] args) throws IOException, SQLException, NoSuchAlgorithmException {
        System.out.println("Hello world");

        if (args.length > 0) {
            if ("s".equalsIgnoreCase(args[0])) {
                server(ConfigReader.charset());
            } else if ("c".equalsIgnoreCase(args[0])) {
                client(ConfigReader.charset());
            } else {
                log.info("s : 以服务器的方式启动\n" +
                        "c : 以客户端的方式启动\n" +
                        "e : 退出\n");
            }
        } else {
            String tip = "s : 以服务器的方式启动\n" +
                    "c : 以客户端的方式启动\n" +
                    "e : 退出\n";

            String function;
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
    }

    private static void server(Charset charset) throws IOException {
        // 读配置文件
        String host = ConfigReader.get("listen_addr");
        int port = Integer.parseInt(ConfigReader.get("listen_port"));
        int cacheSize = Integer.parseInt(ConfigReader.get("cache_size"));

        // 新建对象
        ServerHandler server = new ServerHandler();
        server.setCacheSize(cacheSize);
        server.setCharset(charset);
        // 尝试启动
        try {
            server.listen(new InetSocketAddress(host, port));
            // 命令解析部分
            String line;
            while ((line = input.read()) != null && !"EXIT".equalsIgnoreCase(line)) {
                try {
                    String command = line.split("#")[0];
                    String[] tmp = line.substring(command.length() + 1).split(":");
                    String dest;
                    String msg;
                    switch (command) {
                        case "show":
                            server.display();
                            break;
                        case "send":
                            dest = tmp[0];
                            msg = line.substring(command.length() + dest.length() + 2);
                            server.send(dest, msg);
                            break;
                        case "file":
                            dest = tmp[0];
                            String filename = line.substring(command.length() + dest.length() + 2);
                            server.sendFile(dest, new File(filename));
                            break;
                        case "kill":
                            String client = line.substring(5);
                            server.close(client);
                            break;
                        default:
                            log.info("show 显示当前接入的连接\n" +
                                    "send 发送消息给指定的连接\n" +
                                    "  例:send#id0:Hello World\n" +
                                    "file 发送文件给指定的连接\n" +
                                    "  例:file#id0:~/Document.zip\n" +
                                    "kill 强制指定的连接下线\n" +
                                    "exit 退出\n");
                    }
                } catch (StringIndexOutOfBoundsException | ArrayIndexOutOfBoundsException e) {
                    log.warn("命令无效");
                } catch (IllegalArgumentException e) {
                    log.warn(e.getMessage());
                }
            }
            // 循环结束关闭服务器
            server.close();
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
        String host = ConfigReader.get("server_addr");
        int port = Integer.parseInt(ConfigReader.get("server_port"));
        int cacheSize = Integer.parseInt(ConfigReader.get("cache_size"));
        // 新建对象
        try {
            ClientHandler client = new ClientHandler();
            client.setCacheSize(cacheSize);
            client.setCharset(charset);
            // 尝试启动
            client.connect(new InetSocketAddress(host, port));
            //命令解析部分
            String line;
            while ((line = input.read()) != null && !"EXIT".equalsIgnoreCase(line)) {
                try {
                    String command = line.split("#")[0];
                    String msg = line.substring(command.length() + 1);
                    switch (command) {
                        case "send":
                            client.send(msg);
                            break;
                        case "encrypt":
                            client.encrypt();
                            break;
                        case "file":
                            client.sendFile(new File(msg));
                            break;
                        case "reg":
                            client.registry(msg);
                            break;
                        case "talk":
                            client.talk(msg);
                            break;
                        case "find":
                            client.find();
                            break;
                        default:
                            log.info("encrypt 请求加密通信\n" +
                                    "reg 注册昵称" +
                                    "find 显示服务器接入的连接\n" +
                                    "send 发送消息给服务器\n" +
                                    "file 发送文件给服务器\n" +
                                    "  例:file#~/Document.zip\n" +
                                    "kill 强制指定的连接下线\n" +
                                    "talk 发送消息给指定的连接\n" +
                                    "  例:talk#id0:Hello World\n" +
                                    "exit 退出\n");
                    }
                } catch (StringIndexOutOfBoundsException | ArrayIndexOutOfBoundsException e) {
                    log.warn("命令无效");
                }
            }
            // 循环结束关闭客户端
            client.close();
        } catch (IOException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }
}
