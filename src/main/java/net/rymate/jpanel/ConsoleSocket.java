package net.rymate.jpanel;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.bukkit.Bukkit;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.nio.ByteBuffer;
import java.util.Date;

import org.apache.logging.log4j.core.appender.AbstractAppender;

/**
 * Web socket server
 * <p>
 * Created by ryan on 24/06/15.
 */
public class ConsoleSocket extends WebSocketServer {
    private PanelPlugin plugin;
    private ArrayList<String> oldMsgs = new ArrayList();
    private ArrayList<WebSocket> sockets = new ArrayList();


    public ConsoleSocket(int port, Draft d, PanelPlugin panelPlugin) throws UnknownHostException {
        super(new InetSocketAddress(port), Collections.singletonList(d));
        this.plugin = panelPlugin;
        plugin.getServerLogger().addAppender(new LogHandler(this));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        sockets.add(conn);
        for (String message : oldMsgs) {
            conn.send(message);
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        sockets.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), message);
        //conn.send("Command recieved!");
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

    }

    public void appendMessage(String message) {
        oldMsgs.add(message);
        for (WebSocket socket : sockets) {
            socket.send(message);
        }
    }

    private class LogHandler extends AbstractAppender {
        private ConsoleSocket socket;

        public LogHandler(ConsoleSocket socket) {
            super("RemoteController", null, null);
            this.socket = socket;
            start();
        }

        @Override
        public void append(LogEvent event) {
            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            Date date = new Date();
            appendMessage(dateFormat.format(date) + " [" + event.getLevel().toString() + "] " + event.getMessage().getFormattedMessage());
        }
    }
}
