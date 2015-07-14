package net.rymate.jpanel;

import net.rymate.jpanel.Utils.Lag;
import net.rymate.jpanel.Utils.PasswordHash;
import net.rymate.jpanel.getters.*;
import net.rymate.jpanel.posters.FilePost;
import net.rymate.jpanel.posters.LoginPost;
import net.rymate.jpanel.getters.PlayerManagerPath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.java_websocket.drafts.Draft_17;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import static spark.Spark.*;

/**
 * Main Class of JPanel
 *
 * Created by Ryan on 22/06/2015.
 */
public class PanelPlugin extends JavaPlugin {

    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger("Minecraft-Server");
    private static final org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
    private ConsoleSocket socket;
    private FileConfiguration config;

    private int httpPort = 4567;
    private int socketPort = 9003;
    private String socketPath = "";

    private PanelSessions sessions;


    public void onDisable() {
        stop();
        try {
            socket.stop();
            socket = null;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sessions.destroy();
    }

    public void onEnable() {
        Lag lag = Lag.getInstance();
        sessions = PanelSessions.getInstance();

        getServer().getScheduler().scheduleSyncRepeatingTask(this, lag, 100L, 1L);

        config = getConfig();

        if (config.isConfigurationSection("users")) {
            // load the users
            for (String key : config.getConfigurationSection("users").getKeys(false)) {
                String password = config.getString("users." + key + ".password");
                boolean canEditFiles = config.getBoolean("users." + key + ".canEditFiles");
                PanelUser user = new PanelUser(password, canEditFiles);
                sessions.addUser(key, user);
            }
        }

        config.set("http-port", config.get("http-port", httpPort));
        config.set("websocket-port", config.get("websocket-port", socketPort));
        config.set("websocket-path", config.get("websocket-path", socketPath));

        httpPort = config.getInt("http-port");
        socketPort = config.getInt("websocket-port");
        socketPath = config.getString("websocket-path");

        saveConfig();

        // init spark server
        //setupSpark();

        staticFileLocation("/public");
        port(httpPort);

        // pages (temporary until the page manager is implemented
        new IndexGetter("/", "index.hbs", this);
        new PlayersGetter("/players", "players.hbs", this);
        new FilesPageGetter("/files", "file-manager.hbs", this);

        // text only paths
        new StatsGetter("/stats");
        new LoginPost("/login");
        new FilePost("/file/*");
        new SwitchThemeGetter("/switchtheme");
        new FileGetter("/file/*");
        new PlayerManagerPath("/player/:name/:action", this);


        setupWS();

        System.out.println("[JPanel] JPanel enabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("addlogin")) {
            if (args.length < 1) {
                sender.sendMessage("You must specify a username and a password!");
                return true;
            }

            if (sender instanceof Player) {
                sender.sendMessage("This must be run by the console!");
                return true;
            }

            try {
                sender.sendMessage("Creating user....");
                String password = PasswordHash.generateStrongPasswordHash(args[1]);

                PanelUser user = new PanelUser(password, false);

                sessions.addUser(args[0], user);

                config.set("users." + args[0] + ".password", user.password);
                config.set("users." + args[0] + ".canEditFiles", user.canEditFiles);
                saveConfig();

                sender.sendMessage("User created!");

            } catch (Exception e) {
                sender.sendMessage("Failed to create user!");
                e.printStackTrace();
                return true;
            }

            return true;
        }
        return false;
    }

    private void setupWS() {
        System.out.println("Starting WebSocket server...");
        try {
            socket = new ConsoleSocket( socketPort, new Draft_17(), this );
            socket.start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        System.out.println("Started WebSocket server!");
    }

    public synchronized void managePlayer(String name, String action) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (action.equalsIgnoreCase("kick"))
                    getServer().getPlayer(name).kickPlayer("Kicked!");

                if (action.equalsIgnoreCase("ban")) {
                    getServer().getPlayer(name).setBanned(true);
                    getServer().getPlayer(name).kickPlayer("Banned!");
                }
            }
        }.runTask(this);

    }

    public Logger getServerLogger() {
        return logger;
    }

    public String getWebSocketPort() {
        if (!socketPath.equals(""))
            return socketPath;
        else
            return String.valueOf(socketPort);
    }
}
