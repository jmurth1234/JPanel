package net.rymate.jpanel;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
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
import java.util.HashMap;

import org.apache.logging.log4j.core.appender.AbstractAppender;

/**
 * Web socket server
 * <p>
 * Created by ryan on 24/06/15.
 */
public class ConsoleSocket extends WebSocketServer {
    private PanelPlugin plugin;
    private ArrayList<String> oldMsgs = new ArrayList();
    private HashMap<WebSocket, String> sockets = new HashMap<>();


    public ConsoleSocket(int port, Draft d, PanelPlugin panelPlugin) throws UnknownHostException {
        super(new InetSocketAddress(port), Collections.singletonList(d));
        this.plugin = panelPlugin;
        plugin.getServerLogger().addAppender(new LogHandler(this));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        conn.send("Connected!");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        sockets.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (message.startsWith("AUTH")) {
            String token = message.split(" ")[1];
            if (plugin.getSessions().containsKey(token)) {
                sockets.put(conn, (String) plugin.getSessions().get(token));
                for (String msg : oldMsgs) {
                    conn.send(msg);
                }
                plugin.getServerLogger().log(Level.INFO, "Console user " + sockets.get(conn) + " logged in!");
            } else {
                plugin.getServerLogger().log(Level.INFO, "Console user " + sockets.get(conn) + " failed to log in!");

                conn.close(0, "AUTH CODE INCORRECT, PLEASE LOG IN");
            }
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), message);
                }
            }.runTask(plugin);

            plugin.getServerLogger().log(Level.INFO, "Console user " + sockets.get(conn) + " ran the command " + message);
        }

        //conn.send("Command recieved!");
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

    }

    public void appendMessage(String message) {
        oldMsgs.add(message);
        if (oldMsgs.size() > 1000) {
            oldMsgs.remove(0);
            oldMsgs.trimToSize();
        }
        for (WebSocket socket : sockets.keySet()) {
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
            String message = event.getMessage().getFormattedMessage();
            message = message.replaceAll("\\e\\[[\\d;]*[^\\d;]",""); // remove ansi characters as they don't work well
            appendMessage(dateFormat.format(date) + " [" + event.getLevel().toString() + "] " + message);
        }
    }
}
