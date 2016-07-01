package net.rymate.jpanel;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.bukkit.scheduler.BukkitRunnable;

import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.api.annotations.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.logging.log4j.core.appender.AbstractAppender;

/**
 * Web socket server
 * <p>
 * Created by ryan on 24/06/15.
 */

@WebSocket
public class ConsoleSocket {
    private final PanelSessions sessions;
    private PanelPlugin plugin;
    private ArrayList<String> oldMsgs = new ArrayList();

	private ConcurrentHashMap<Session, String> sockets = new ConcurrentHashMap<>();

    public ConsoleSocket() throws UnknownHostException {
        this.plugin = PanelPlugin.getInstance();
        sessions = PanelSessions.getInstance();
        plugin.getServerLogger().addAppender(new LogHandler(this));
		plugin.getServerLogger().info("[JPanel] WebSocket started");
	}

	@OnWebSocketConnect
	public void connected(Session session) throws IOException {
		List<HttpCookie> cookies = session.getUpgradeRequest().getCookies();
		String token = "";

		for (HttpCookie cookie : cookies) {
			if (cookie.getName().equalsIgnoreCase("loggedin")) {
				token = cookie.getValue();
			}
		}

		if (sessions.isLoggedIn(token)) {
			sockets.put(session, sessions.getAuthedUsername(token));
			session.getRemote().sendString("SCROLLBACK " + oldMsgs.size());
			for (String msg : oldMsgs) {
				session.getRemote().sendString(msg);
			}
			session.getRemote().sendString("Connected!");
		} else {
			session.getRemote().sendString("Failed to authenticate with the web socket!");
			session.close();
		}
	}


	@OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
		sockets.remove(session);
	}

	@OnWebSocketMessage
	public void message(Session session, String message) throws IOException {
		String username = sockets.get(session);

        if (!sessions.getUser(username).canSendCommands) {
			session.getRemote().sendString("You're not allowed to send commands! Contact the server admin if this is in error.");
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), message);
            }
        }.runTask(plugin);

        if (!message.contains("passwd"))
            plugin.getServerLogger().log(Level.INFO, "Console user " + username + " ran the command " + message);
    }

    public void appendMessage(String message) throws IOException {
        oldMsgs.add(message);
        if (oldMsgs.size() > 1000) {
            oldMsgs.remove(0);
            oldMsgs.trimToSize();
        }
        for (Session socket : sockets.keySet()) {
            socket.getRemote().sendString(message);
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
            //message = message.replaceAll("\\e\\[[\\d;]*[^\\d;]",""); // remove ansi characters as they don't work well
			try {
				appendMessage(dateFormat.format(date) + " [" + event.getLevel().toString() + "] " + message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    }
}
