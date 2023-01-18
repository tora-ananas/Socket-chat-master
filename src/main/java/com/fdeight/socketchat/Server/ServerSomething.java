package com.fdeight.socketchat.Server;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Date;

import static com.fdeight.socketchat.Server.Server.DATE_FORMAT;
import com.fazecast.jSerialComm.SerialPort;
import com.github.sarxos.webcam.Webcam;
import org.bytedeco.javacv.CanvasFrame;

public class ServerSomething extends Thread{
    private final Server server;
    private final Socket socket;
    private final Socket socketCam;
    private final BufferedReader in; // поток чтения из сокета
    private final BufferedWriter out; // поток завписи в сокет
    private final DataOutputStream byteOut; //поток записи в сокет отвечающий за камеру
    private String nickName = null;
    private SerialPort serialPort;
    private byte[] byteArray;
    private Webcam webcam = Webcam.getDefault();
    /**
     * Для общения с клиентом необходим сокет (адресные данные)
     *
     * @param server сервер
     * @param socket сокет
     */
    ServerSomething(final Server server, final Socket socket, final Socket socketCam) throws IOException {
        this.server = server;
        this.socket = socket;
        this.socketCam = socketCam;
        // если потоку ввода/вывода приведут к генерированию искдючения, оно проброситься дальше
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        byteOut = new DataOutputStream(socketCam.getOutputStream());
        //webcam.setViewSize(new Dimension(320, 240));
    }

    @Override
    public void run() {
        try {
            // первое сообщение отправленное сюда - это nickName
            nickName = in.readLine();
            for (final ServerSomething ss : server.serverList) {
                if (!ss.nickName.equals(nickName)) {
                    continue;
                }
                processDuplicatedNickName();
                return;
            }
            processUniqueNickName();
            serialPort = portServer();

            webcam.open();
            byteArray = takePic(webcam);
            byteOut.write(byteArray);
            byteOut.flush();
            while (true) {
                byteArray = takePic(webcam);
                if (!processMessage()) {
                    serialPort.closePort();
                    System.out.println("SERIALPORT IS OPEN: " + serialPort.isOpen());
                    break;
                }
            }
        } catch (final IOException e) {
            this.downService();
        }
    }
    public byte[] takePic(Webcam webcam){
        ByteBuffer bytes = webcam.getImageBytes();
        byte[] arr = new byte[bytes.remaining()];
        bytes.get(arr);
        return arr;
    }

    /**
     * Обработать сообщение
     *
     * @return {@code false} окончить работу после обработки сообщения, иначе {@code true}
     * @throws IOException ошибка ввода-вывода
     */
    private boolean processMessage() throws IOException {
        final String message = in.readLine();
        final String preparedMessage = getPreparedMessage(message);
        sendMessage(preparedMessage);
        if(Server.Command.NEXT.equalCommand(message)){
            byteOut.writeInt(byteArray.length);
            byteOut.write(byteArray);
            byteOut.flush();
            System.out.println("byteOut done" + byteArray.length);
        }
        if (Server.Command.LEFT.equalCommand(message)){
            byte[] bytes = packet_byte(0,15);
            serialPort.writeBytes(bytes, bytes.length);
        }
        if (Server.Command.RIGHT.equalCommand(message)){
            byte[] bytes = packet_byte(15,0);
            serialPort.writeBytes(bytes, bytes.length);
        }
        if (Server.Command.UP.equalCommand(message)){
            byte[] bytes = packet_byte(15,15);
            serialPort.writeBytes(bytes, bytes.length);
        }
        if (Server.Command.DOWN.equalCommand(message)){
            byte[] bytes = packet_minus(-15,-15);
            serialPort.writeBytes(bytes, bytes.length);
        }
        if (Server.Command.STOP_CLIENT.equalCommand(message)) {
            downService();
            return false;
        } else if (Server.Command.STOP_ALL_CLIENTS.equalCommand(message) || Server.Command.STOP_SERVER.equalCommand(message)) {
            for (final ServerSomething ss : server.serverList) {
                ss.send(message);
                ss.downService();
            }
            if (Server.Command.STOP_SERVER.equalCommand(message)) {
                System.exit(0);
            }
            return false;
        }
        return true;
    }

    private void sendMessage(final String message) throws IOException {
        System.out.println(message);
        server.history.addHistoryEvent(message);
        for (final ServerSomething ss : server.serverList) {
            ss.send(message);
        }
    }

    private String getPreparedMessage(final String message) {
        final String preparedMessage;
        if (Server.Command.WARNING.equalCommand(message)) {
            preparedMessage = formatMessage("Warning from " + formatNickName(nickName));
        } else if (Server.Command.STOP_CLIENT.equalCommand(message)) {
            preparedMessage = formatMessage("Disconnect " + formatNickName(nickName));
        } else if (Server.Command.STOP_ALL_CLIENTS.equalCommand(message)) {
            preparedMessage = formatMessage("Stop all clients from " + formatNickName(nickName));
        } else if (Server.Command.STOP_SERVER.equalCommand(message)) {
            preparedMessage = formatMessage("Stop server from " + formatNickName(nickName));
        } else {
            preparedMessage = message;
        }
        return preparedMessage;
    }

    private void processDuplicatedNickName() throws IOException {
        final String duplicatedNickName = formatNickName(nickName + " (duplicated)");
        final String duplicatedMessage = formatMessage(String.format("%s disconnected", duplicatedNickName));
        sendMessage(duplicatedMessage);
        send(duplicatedMessage);
        send(formatCommandMessage(Server.Command.STOP_CLIENT_FROM_SERVER.commandName, duplicatedNickName));
        send(Server.Command.STOP_CLIENT_FROM_SERVER.commandName);
        downService();
    }

    private void processUniqueNickName() throws IOException {
        Server.serverList.add(this);
        server.history.printStory(out);
        final String connectMessage = formatMessage("Connect " + formatNickName(nickName));
        sendMessage(connectMessage);
    }

    /**
     * отсылка одного сообщения клиенту
     *
     * @param msg сообщение
     */
    private void send(final String msg) throws IOException {
        out.write(msg + "\n");
        out.flush();
    }

    private String formatMessage(final String message) {
        final Date date = new Date();
        final String strTime = DATE_FORMAT.format(date);
        return String.format("[%s] %s", strTime, message);
    }

    private String formatCommandMessage(final String message, final String nickName) {
        return String.format("%s [command] to %s", formatMessage(message), nickName);
    }

    private String formatNickName(final String nickName) {
        return String.format("[%s]", nickName);
    }

    /**
     * закрытие сервера, удаление себя из списка нитей
     */
    private void downService() {
        try {
            if (!socket.isClosed()) {
                socket.close();
                in.close();
                out.close();
                server.serverList.remove(this);
            }
        } catch (final IOException ignored) {
        }
    }

    public SerialPort portServer(){
        SerialPort serialPort = SerialPort.getCommPort("COM3");

        serialPort.setBaudRate(115200);
        serialPort.openPort();
        System.out.println("SERIALPORT IS OPEN: " + serialPort.isOpen());
        return serialPort;
    }

    public static byte[] packet_byte(int steer, int speed){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        String hexStr = "ABCD";
        String preamble1 = "CD";
        String preamble2 = "AB";

        String steer1 = Integer.toHexString(steer);
        String speed1 = Integer.toHexString(speed);
        String zero = Integer.toHexString(0);
        int xor = Integer.valueOf(hexStr, 16);
        xor = xor ^ steer ^ speed;
        String xor1 = Integer.toHexString(xor);
        int mid = xor1.length() /2;
        String xor_mid1 = xor1.substring(0, mid);
        String xor_mid2 = xor1.substring(mid);
        byteArrayOutputStream.write(Integer.valueOf(preamble1, 16));
        byteArrayOutputStream.write(Integer.valueOf(preamble2, 16));
        byteArrayOutputStream.write(Integer.valueOf(steer1, 16));
        byteArrayOutputStream.write(Integer.valueOf(zero, 16));
        byteArrayOutputStream.write(Integer.valueOf(speed1, 16));
        byteArrayOutputStream.write(Integer.valueOf(zero, 16));
        byteArrayOutputStream.write(Integer.valueOf(xor_mid2, 16));
        byteArrayOutputStream.write(Integer.valueOf(xor_mid1, 16));
        byte[] array = byteArrayOutputStream.toByteArray();
        /*System.out.println(" START ");
        for (byte b : array){
            System.out.format("x%02x",b);
        }*/
        return array;
    }

    public static byte[] packet_minus(int steer, int speed){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        String hexStr = "ABCD";
        String preamble1 = "CD";
        String preamble2 = "AB";

        String steer1 = Integer.toHexString(steer);
        String speed1 = Integer.toHexString(speed);

        int xor = Integer.valueOf(hexStr, 16);

        xor = xor ^ steer ^ speed;
        String xor1 = Integer.toHexString(xor);

        int mid = xor1.length() /2;

        String xor_mid1 = xor1.substring(0, mid);
        String xor_mid2 = xor1.substring(mid);

        byteArrayOutputStream.write(Integer.valueOf(preamble1, 16));
        byteArrayOutputStream.write(Integer.valueOf(preamble2, 16));
        byteArrayOutputStream.write(Integer.parseUnsignedInt(steer1, 16));
        byteArrayOutputStream.write(Integer.valueOf("ff", 16));
        byteArrayOutputStream.write(Integer.parseUnsignedInt(speed1, 16));
        byteArrayOutputStream.write(Integer.valueOf("ff", 16));
        byteArrayOutputStream.write(Integer.parseUnsignedInt(xor_mid2, 16));
        byteArrayOutputStream.write(Integer.parseUnsignedInt(xor_mid1, 16));

        byte[] array1 = byteArrayOutputStream.toByteArray();
        /*System.out.println(" START ");
        for (byte b : array1){
            System.out.format("/x%02x",b);
        }*/
        return array1;
    }

    public static void main(String[] args){
        /*byte[] arr = packet_byte(-15,0);
        byte[] arr1 = packet_byte(0,-15);
        byte[] arr2 = packet_byte(-15,-15);*/
        int val = -1;
        String hex = Integer.toHexString(val);
        System.out.println(hex);
        int parsedResult = (int) Long.parseLong(hex, 16);
        System.out.println(hex);
        System.out.println(Integer.parseUnsignedInt(hex, 16));
        packet_minus(-15, -15);
    }
}
