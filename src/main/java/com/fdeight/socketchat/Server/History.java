package com.fdeight.socketchat.Server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedDeque;

import static com.fdeight.socketchat.Server.Server.COUNT_EVENTS_IN_HISTORY;

/**
 * класс хранящий в ссылочном приватном
 * списке информацию о последних сообщениях
 */
public class History {
    private final ConcurrentLinkedDeque<String> events = new ConcurrentLinkedDeque<>();

    /**
     * добавить новый элемент в список
     *
     * @param event элемент
     */
    void addHistoryEvent(final String event) {
        if (events.size() >= COUNT_EVENTS_IN_HISTORY) {
            events.removeFirst();
        }
        events.add(event);
    }

    /**
     * отсылаем последовательно каждое сообщение из списка
     * в поток вывода данному клиенту (новому подключению)
     *
     * @param writer поток вывода
     */
    void printStory(final BufferedWriter writer) {
        if (events.size() > 0) {
            try {
                writer.write("History messages\n");
                for (final String event : events) {
                    writer.write(event + "\n");
                }
                writer.write("...\n");
                writer.flush();
            } catch (final IOException ignored) {
            }
        }
    }
}
