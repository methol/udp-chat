import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by tuzhihao on 2018/9/23.
 */
public class P2pUdpClient {
    private final static String SERVER_HOST = "s.tuzhihao.com";
    private final static int SERVER_PORT = 9999;
    /**
     * 消息id 自增
     */
    private AtomicLong requestId = new AtomicLong(1);
    private DatagramSocket client = null;
    /**
     * 服务端下发的客户端id(uuid)
     */
    private String clientId;
    /**
     * 客户端缓存
     * key: ip:port
     * value: uuid
     */
    private Map<String, String> clients = new HashMap<>();

    public static void main(String[] args) throws Exception {
        P2pUdpClient client = new P2pUdpClient();
        client.start();
        client.login();
        client.list();
        client.chat();
    }

    public void chat() throws Exception {
        new Thread(() -> {
            while (true) {
                final Scanner scanner = new Scanner(System.in);
                final String s = scanner.nextLine();
                sendDataToOthers(s);
            }
        }).start();

    }

    /**
     * 发送数据到所有的client列表
     *
     * @param content 数据内容
     */
    private void sendDataToOthers(String content) {
        for (Map.Entry<String, String> entry : clients.entrySet()) {
            if (entry.getValue().equals(clientId)) {
                continue;
            }
            Msg msg = new Msg();
            msg.setId(requestId.getAndIncrement());
            msg.setCmd(Msg.CMD_CHAT);
            msg.setContent(content);
            byte[] data = msg.toString().getBytes();
            try {
                // 找一个客户端 发送数据
                String[] ipPort = entry.getKey().split(":");
                int port = Integer.valueOf(ipPort[1]);
                InetAddress address = InetAddress.getByName(ipPort[0]);
                client.send(new DatagramPacket(data, data.length, address, port));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() throws Exception {
        client = new DatagramSocket();
        new Thread(() -> {
            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                try {
                    client.receive(receivePacket);
                    onReceivePacket(receivePacket);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        System.out.println("start success");
    }

    protected void onReceivePacket(DatagramPacket packet) throws Exception {
        final String receiveData = new String(packet.getData(), StandardCharsets.UTF_8);
        System.out.println("receive data: " + receiveData + " from " + packet.getAddress() + ":" + packet.getPort());
        final Msg receiveMsg = Msg.from(receiveData);
        if (receiveMsg == null) {
            throw new IllegalArgumentException("ERROR DATA: " + receiveData);
        }
        final String content = receiveMsg.getContent();
        switch (receiveMsg.getCmd()) {
            case Msg.CMD_LOGIN:
                // LOGIN
                clientId = content;
                break;
            case Msg.CMD_LIST: //LIST
                final String[] list = content.split("\\|");
                Map<String, String> clients = new HashMap<>();
                for (String s : list) {
                    if (s == null || "".equals(s) || s.length() < 10 || 0 == s.charAt(0)) {
                        continue;
                    }
                    final String clientAdd = s.substring(0, s.indexOf("~"));
                    final String clientId = s.substring(s.indexOf("~") + 1);
                    clients.put(clientAdd, clientId);
                }
                System.out.println("update client , size = " + clients.size());
                this.clients = clients;
                if (this.clients.size() > 1) {
                    for (Map.Entry<String, String> entry : this.clients.entrySet()) {
                        if (entry.getValue().equals(clientId)) {
                            continue;
                        }
                        // 给所有客户端发送数据
                        sendDataToOthers("send to " + entry.getValue() + " ! My clientId is " + clientId);
                    }
                }
                break;
            case Msg.CMD_CHAT:
                //CHAT
                System.out.println(" --- receive msg " + receiveMsg.getId() + " --- " + content);
                break;
            default:
                throw new IllegalArgumentException(receiveMsg.getCmd() + " not support");
        }
    }

    private void sendCmd(String cmd) throws Exception {
        Msg msg = new Msg();
        msg.setId(requestId.getAndIncrement());
        msg.setCmd(cmd);
        byte[] data = msg.toString().getBytes();

        InetAddress server = InetAddress.getByName(SERVER_HOST);
        DatagramPacket packet = new DatagramPacket(data, data.length, server, SERVER_PORT);
        client.send(packet);
    }

    /**
     * 登录
     *
     * @throws Exception
     */
    private void login() throws Exception {
        sendCmd(Msg.CMD_LOGIN);
        System.out.println("login success");
    }

    /**
     * 获取用户列表，每隔5s更新一次
     */
    private void list() {
        new Thread(() -> {
            while (true) {
                try {
                    sendCmd(Msg.CMD_LIST);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
