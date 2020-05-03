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
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

public class Main {
    // 日志记录
    private static Logger log = Logger.getLogger(Main.class);
    // 通过键盘输入
    private static GeneralInput input = KeyboardReader.getInstance();

    public static void main(String[] args) throws IOException, SQLException, NoSuchAlgorithmException {
        System.out.println("Hello world");

        // 检测输入命令 s: 服务器 c: 客户端 e: 退出
        if (args.length > 0) {
            if ("s".equalsIgnoreCase(args[0])) {
                server();
            } else if ("c".equalsIgnoreCase(args[0])) {
                client();
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
                        server();
                        break;
                    case "c":
                        client();
                        break;
                    default:
                        log.warn("命令无效");
                }
                log.info("返回主界面");
            }
        }
    }

    private static void server() throws IOException {
        // 新建对象
        ServerHandler server = new ServerHandler();
        server.setCacheSize(Integer.parseInt(ConfigReader.get("cache_size")));
        server.setCharset(ConfigReader.charset());
        // 尝试启动
        try {
            server.listen(new InetSocketAddress(
                    ConfigReader.get("listen_addr"),
                    Integer.parseInt(ConfigReader.get("listen_port"))
            ));
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

    private static void client() {
        // 新建对象
        try {
            ClientHandler client = new ClientHandler();
            client.setCacheSize(Integer.parseInt(ConfigReader.get("cache_size")));
            client.setCharset(ConfigReader.charset());
            // 尝试启动
            client.connect(new InetSocketAddress(
                    ConfigReader.get("server_addr"),
                    Integer.parseInt(ConfigReader.get("server_port"))
            ));
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
                            if (msg.split("#").length > 1) {
                                String file = msg.substring(0, msg.lastIndexOf("#"));
                                String filename = msg.substring(msg.lastIndexOf("#")+1, msg.length());
                                client.sendFile(new File(file), filename);
                            } else {
                                client.sendFile(new File(msg));
                            }
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
                    log.warn("命令无效: "+e.getMessage());
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
