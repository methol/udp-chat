package com.tuzhihao.chat;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Random;

public class Stun {

    private static final String STUN_HOST = "stun.services.mozilla.com";
    private static final int STUN_PORT = 3478;
    private static final int FAIL_COUNTER = 10;

    public static void main(String[] args) {
        String ip = "";
        int port = -1;
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            clientSocket.setSoTimeout(10000);
            InetAddress IPAddress = InetAddress.getByName(STUN_HOST);
            byte[] sendData = new byte[20];//network bytes order
            byte[] respData = new byte[32];//network bytes order
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, STUN_PORT);
            DatagramPacket receivePacket = new DatagramPacket(respData, respData.length);
            sendData[0] = 0;
            sendData[1] = 1;//STUN_BINDREQ    0x0001

            sendData[2] = 0;
            sendData[3] = 0;//length = 0

            sendData[4] = 0x42;
            sendData[5] = (byte) 0xA4;
            sendData[6] = 0x12;
            sendData[7] = 0x21;//4 magic bytes = 0x2112A442

            Random random = new Random();
            for (int i = 8; i < 20; i++) {
                sendData[i] = (byte) random.nextInt(255);//set last 12 bytes to rand vals
            }

            for (int i = 0; i < FAIL_COUNTER; i++) {
                clientSocket.send(sendPacket);
                clientSocket.receive(receivePacket);

                // STUN_BINDRESP   0x0101
                if (respData[0] == 1 && respData[1] == 1) {// last 6 bytes -> 2 bytes port+4 bytes IP
                    ip = Integer.toString(GetUnsignedByte(respData[28])) + "." + Integer.toString(
                            GetUnsignedByte(respData[29])) + "."
                            + Integer.toString(GetUnsignedByte(respData[30])) + "." + Integer.toString(
                            GetUnsignedByte(respData[31]));
                    byte[] portbuf = new byte[2];
                    portbuf[0] = respData[26];
                    portbuf[1] = respData[27];
                    port = GetUnsignedShort(convertntohs(portbuf));
                    clientSocket.setSoTimeout(0);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(ip + ":" + port);
    }


    private static int GetUnsignedByte(byte val) {
        return ((int) val) & 0xFF;
    }

    private static int GetUnsignedShort(short val) {
        return ((int) val) & 0xFFFF;
    }

    private static short convertntohs(byte[] value) {
        ByteBuffer buf = ByteBuffer.wrap(value);
        return buf.getShort();
    }

}
