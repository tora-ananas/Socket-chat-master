package com.fdeight.socketchat.Server;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Консольный многопользовательский чат.
 * Сервер
 */
public class Server {

    public static final int PORT = 8080;
    public static final int PORTCAM = 8081;
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
    static final int COUNT_EVENTS_IN_HISTORY = 20;

    public enum Command {
        WARNING("warning"),
        STOP_CLIENT_FROM_SERVER("stop client from server"),
        STOP_CLIENT("stop client"),
        STOP_ALL_CLIENTS("stop all clients"),
        STOP_SERVER("stop server"),
        LEFT("button_left"),
        RIGHT("button_right"),
        UP("button_up"),
        DOWN("button_down"),
        NEXT("next"),
        ;

        final String commandName;

        Command(final String commandName) {
            this.commandName = commandName;
        }

        public boolean equalCommand(final String message) {
            return commandName.equals(message);
        }

        public static boolean isCommandMessage(final String message) {
            for (final Command command : values()) {
                if (command.equalCommand(message)) {
                    return true;
                }
            }
            return false;
        }
    }

    static final ConcurrentLinkedQueue<ServerSomething> serverList = new ConcurrentLinkedQueue<>();
    History history = new History(); // история

    private void startServer() throws IOException {
        history = new History();
        System.out.println(String.format("Server started, port: %d", PORT));
        try (final ServerSocket serverSocket = new ServerSocket(PORT)) {
            // serverSocket.setSoTimeout(1000);
            System.out.println(String.format("Server started, port: %d", PORTCAM));
            try (final ServerSocket serverSocketCam = new ServerSocket(PORTCAM)) {
                while (true) { // приложение с помощью System.exit() закрывается по команде от клиента
                    // Блокируется до возникновения нового соединения
                    final Socket socket = serverSocket.accept();
                    final Socket socketCam = serverSocketCam.accept();
                    try {
                        new ServerSomething(this, socket, socketCam).start();
                    } catch (final IOException e) {
                        // Если завершится неудачей, закрывается сокет,
                        // в противном случае, нить закроет его:
                        socket.close();
                        socketCam.close();
                    }
                }
            } catch (final BindException e) {
                e.printStackTrace();
            }
        } catch (final BindException e) {
            e.printStackTrace();
        }
    }

    public static void main(final String[] args) throws IOException {
        final Server server = new Server();
        server.startServer();
    }
}
