import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by tuzhihao on 2018/9/23.
 */
public class P2pUdpServer {
    /**
     * 端口号
     */
    private static final int PORT = 9999;
    /**
     * 最大数据包大小
     */
    private final static int MAX_PACKET_SIZE = 1024;
    private DatagramSocket server;
    /**
     * 客户端缓存
     * key: ip:port
     * value: uuid
     */
    private Map<String, String> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) throws SocketException {
        P2pUdpServer server = new P2pUdpServer();
        server.start();
    }

    public void start() throws SocketException {
        // 创建数据报套接字并将其绑定到本地主机上的指定端口
        server = new DatagramSocket(PORT);
        new Thread(() -> {
            while (true) {
                try {
                    byte[] buffer = new byte[MAX_PACKET_SIZE];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    server.receive(packet);
                    onReceivePacket(packet);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        System.out.println("Server Start port: " + PORT);
    }

    private void onReceivePacket(DatagramPacket packet) throws Exception {
        final String receiveData = new String(packet.getData(), StandardCharsets.UTF_8);
        System.out.println("receive data: " + receiveData + " from " +
                packet.getAddress() + ":" + packet.getPort());
        InetAddress clientAddress = packet.getAddress();
        int clientPort = packet.getPort();
        String key = clientAddress.getHostAddress() + ":" + clientPort;
        if (!clients.containsKey(key)) {
            //新增一个客户端
            clients.put(key, UUID.randomUUID().toString());
        }
        String clientId = clients.get(key);
        Msg receiveMsg = Msg.from(receiveData);
        if (receiveMsg == null) {
            throw new IllegalArgumentException("ERROR DATA: " + receiveData);
        }
        String cmd = receiveMsg.getCmd();
        Msg sendMsg = new Msg();
        sendMsg.setId(receiveMsg.getId());
        switch (cmd) {
            // 客户端请求登录的时候，返回生成的clientId
            case Msg.CMD_LOGIN:
                sendMsg.setCmd(Msg.CMD_LOGIN);
                sendMsg.setContent(clientId);
                break;
            // 客户端请求所有用户列表，返回用户列表
            case Msg.CMD_LIST:
                sendMsg.setCmd(Msg.CMD_LIST);
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> entry : clients.entrySet()) {
                    sb.append(entry.getKey()).append("~").append(entry.getValue()).append("|");
                }
                sendMsg.setContent(sb.toString());
                break;
            default:
                throw new IllegalArgumentException(cmd + " not support");
        }
        byte[] sendData = sendMsg.toString().getBytes(StandardCharsets.UTF_8);
        packet = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
        server.send(packet);
    }
}
