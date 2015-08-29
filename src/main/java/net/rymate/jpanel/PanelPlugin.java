package net.rymate.jpanel;

import net.rymate.jpanel.Utils.Lag;
import net.rymate.jpanel.Utils.PasswordHash;
import net.rymate.jpanel.getters.*;
import net.rymate.jpanel.posters.ClientLoginPost;
import net.rymate.jpanel.posters.FilePost;
import net.rymate.jpanel.posters.LoginPost;
import net.rymate.jpanel.getters.PlayerManagerPath;
import net.rymate.jpanel.posters.PlayerManagerPlus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.java_websocket.drafts.Draft_17;

import java.io.IOException;
import java.net.UnknownHostException;

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
                boolean canEditFiles = config.getBoolean("users." + key + ".canEditFiles", false);
                boolean canChangeGroups = config.getBoolean("users." + key + ".canChangeGroups", false);
                boolean canSendCommands = config.getBoolean("users." + key + ".canSendCommands", false);
                PanelUser user = new PanelUser(password, canEditFiles, canChangeGroups, canSendCommands);
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

        // pages
        new IndexGetter("/", "index.hbs", this);
        new SimplePageGetter("/files", "file-manager.hbs", this);

        // text only paths
        new StatsGetter("/stats");
        new LoginPost("/login", logger);
        new ClientLoginPost("/auth", logger);
        new FilePost("/file/*");
        new SwitchThemeGetter("/switchtheme");
        new FileGetter("/file/*");

        PanelNavigation nav = PanelNavigation.getInstance();
        nav.registerPath("/", "Home");
        nav.registerPath("/players", "Players");
        nav.registerPath("/files", "Files");

        if (getServer().getPluginManager().isPluginEnabled("Vault")) {
            //PanelNavigation.getInstance().registerPath("/permissions", "Permissions");
            new SimplePageGetter("/players", "playersplus.hbs", this);
            new PlayerManagerPlus("/players", this);
        } else {
            new PlayersPageGetter("/players", "players.hbs", this);
            new PlayersGetter("/players", this);
            new PlayerManagerPath("/player/:name/:action", this);
        }


        setupWS();

        System.out.println("[JPanel] JPanel enabled!");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
        String pluginName = event.getPlugin().getName();

        if (pluginName.equals("Vault")) {
            PanelNavigation.getInstance().registerPath("/perms", "Permissions");
            new SimplePageGetter("/perms", "playersplus.hbs", this);
            new PlayerManagerPlus("/permissions", this);
        }

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

                PanelUser user = new PanelUser(password, false, false, false);

                sessions.addUser(args[0], user);

                config.set("users." + args[0] + ".password", user.password);
                config.set("users." + args[0] + ".canEditFiles", user.canEditFiles);
                config.set("users." + args[0] + ".canChangeGroups", user.canChangeGroups);
                config.set("users." + args[0] + ".canSendCommands", user.canSendCommands);
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
