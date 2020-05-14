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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    // 日志记录
    private static Logger log = Logger.getLogger(Main.class);
    // 通过键盘输入
    private static GeneralInput input = KeyboardReader.getInstance();

    private static String listenAddr;
    private static int listenPort;
    private static String serverAddr;
    private static int serverPort;

    static {
        try {
            listenAddr = ConfigReader.get("listen_addr");
            listenPort = Integer.parseInt(ConfigReader.get("listen_port"));
            serverAddr = ConfigReader.get("server_addr");
            serverPort = Integer.parseInt(ConfigReader.get("server_port"));
        } catch (NumberFormatException e) {
            log.error("配置中有错误", e);
        }
    }

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

            String line;
            while ((line = input.read(tip)) != null && !"e".equalsIgnoreCase(line)) {
                String command = null;
                try {
                    command = line.split("\\s")[0];
                } catch (StringIndexOutOfBoundsException | ArrayIndexOutOfBoundsException e) {
                    command = line;
                }
                try {
                    switch (command) {
                        case "s":
                            server();
                            break;
                        case "c":
                            client();
                            break;
                        default:
                            log.warn("命令无效");
                    }
                } catch (StringIndexOutOfBoundsException e) {
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
            server.listen(new InetSocketAddress(listenAddr, listenPort));
            // 命令解析部分
            String line;
            while ((line = input.read()) != null && !"EXIT".equalsIgnoreCase(line)) {
                String[] command = getParam(line);
                try {
                    switch (command[0]) {
                        case "send":
                            server.service(ServerHandler.Function.SEND, command);
                            break;
                        case "file":
                            server.service(ServerHandler.Function.FILE, command);
                            break;
                        case "encrypt":
                            server.service(ServerHandler.Function.ENCRYPT, command);
                            break;
                        case "show":
                            server.service(ServerHandler.Function.SHOW);
                            break;
                        case "kill":
                            server.service(ServerHandler.Function.KILL, command);
                            break;
                        default:
                            log.warn("命令无效: " + line);
                            server.tip();
                    }
                } catch (StringIndexOutOfBoundsException e) {
                    log.warn("命令无效: " + line);
                    server.tip();
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
        ClientHandler client = null;
        try {
            client = new ClientHandler();
            client.setCacheSize(Integer.parseInt(ConfigReader.get("cache_size")));
            client.setCharset(ConfigReader.charset());
            // 尝试启动
            client.connect(new InetSocketAddress(serverAddr, serverPort));
            //命令解析部分
            String line;
            while ((line = input.read()) != null && !"EXIT".equalsIgnoreCase(line) && !client.isClosed()) {
                String[] command = getParam(line);
                try {
                    switch (command[0]) {
                        case "send":
                            client.service(ClientHandler.Function.SEND, command);
                            break;
                        case "file":
                            client.service(ClientHandler.Function.FILE, command);
                            break;
                        case "encrypt":
                            client.service(ClientHandler.Function.ENCRYPT);
                            break;
                        case "find":
                            client.service(ClientHandler.Function.FIND, command);
                            break;
                        case "name":
                            client.service(ClientHandler.Function.NAME, command);
                            break;
                        case "udp":
                            client.service(ClientHandler.Function.UDP, command);
                            break;
                        default:
                            log.warn("命令无效: " + line);
                            client.tip();
                    }
                } catch (ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException e) {
                    log.warn("命令无效: " + line);
                    client.tip();
                }
            }
            // 循环结束关闭客户端
            client.close();
        } catch (IOException e) {
            log.warn("与服务器断开连接");
            log.debug(e);
            if (client != null) {
                client.close();
            }
        }
    }

    private static String[] getParam(String line) {
        List<String> list = new ArrayList<>();
        String regex = "(\"[[^\"].]+\"|\'[[^\'].]+\'|[\\S]+)";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(line);
        while (m.find()) {
            list.add(m.group(1).replaceAll("\"", ""));
        }
        return list.toArray(new String[list.size()]);
    }
}
