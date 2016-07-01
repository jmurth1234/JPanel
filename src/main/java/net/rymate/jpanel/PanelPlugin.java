package net.rymate.jpanel;

import net.rymate.jpanel.Utils.Lag;
import net.rymate.jpanel.Utils.PasswordHash;
import net.rymate.jpanel.getters.*;
import net.rymate.jpanel.posters.ClientLoginPost;
import net.rymate.jpanel.posters.FilePost;
import net.rymate.jpanel.posters.LoginPost;
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

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static spark.Spark.*;

/**
 * Main Class of JPanel
 * <p>
 * Created by Ryan on 22/06/2015.
 */
public class PanelPlugin extends JavaPlugin {

    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger("Minecraft-Server");
    private static final org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
	private static PanelPlugin instance;
	private ConsoleSocket socket;
    private FileConfiguration config;

    private int httpPort = 4567;
    private String socketPath = "";

    private PanelSessions sessions;

	public static PanelPlugin getInstance() {
		return instance;
	}

	public void onDisable() {
        stop();

        sessions.destroy();
    }

    public void onEnable() {
        Lag lag = Lag.getInstance();
        sessions = PanelSessions.getInstance();
        extractResources(getClass(), "public");
		instance = this;

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

        httpPort = config.getInt("http-port");

        saveConfig();

		webSocket("/socket", ConsoleSocket.class);

        //staticFileLocation("/public");
        externalStaticFileLocation(new File(".").getAbsolutePath() + "/JPanel-public/");
        port(httpPort);

        // pages
        new SimplePageGetter("/", "index.hbs", this);
        new SimplePageGetter("/files", "file-manager.hbs", this);

        // text only paths
        new StatsGetter("/stats");
        new LoginPost("/login", logger);
        new LogoutPath("/logout");
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
        } else if (cmd.getName().equalsIgnoreCase("passwd")) {
            if (args.length < 2) {
                sender.sendMessage("You must specify a username, the old password and the new password!");
                return true;
            }

            if (sender instanceof Player) {
                sender.sendMessage("This must be run by the console!");
                return true;
            }

            try {
                PanelUser user = sessions.getUser(args[0]);
                String oldPassword = args[1];

                if (PasswordHash.validatePassword(oldPassword, user.password)) {
                    String newPassword = PasswordHash.generateStrongPasswordHash(args[2]);

                    PanelUser newUser = new PanelUser(newPassword, user.canEditFiles, user.canChangeGroups, user.canSendCommands);

                    sessions.addUser(args[0], newUser);
                    config.set("users." + args[0] + ".password", newUser.password);
                    config.set("users." + args[0] + ".canEditFiles", newUser.canEditFiles);
                    config.set("users." + args[0] + ".canChangeGroups", newUser.canChangeGroups);
                    config.set("users." + args[0] + ".canSendCommands", newUser.canSendCommands);

                    saveConfig();

                    sender.sendMessage("Password for " + args[0] + " changed!");

                } else {
                    sender.sendMessage("Old password incorrect!");
                }

            } catch (Exception e) {
                sender.sendMessage("Failed to create user!");
                e.printStackTrace();
                return true;
            }


            return true;
        }
        return false;
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

    public static void extractResources(Class<? extends JavaPlugin> pluginClass, String filePath) {
        try {
            File dest = new File(new File(".").getAbsolutePath() + "/JPanel-public/");

            if (!dest.exists()) {
                dest.mkdir();
                dest.setWritable(true);
            }

            File jarFile = new File(pluginClass.getProtectionDomain().getCodeSource().getLocation().getPath());

            if (jarFile.isFile()) {
                JarFile jar;
                jar = new JarFile(jarFile);

                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    if (name.startsWith(filePath + "/")) {
                        InputStream in = getResourceFromJar(name, pluginClass.getClassLoader());
                        name = name.replace(filePath + "/", "");
                        File outFile = new File(dest + "/" + name);
                        if (name.endsWith("/")) {
                            outFile.mkdirs();
                        } else {
                            if (outFile.isDirectory()) continue;

                            if (!outFile.exists()) outFile.createNewFile();
                            OutputStream out = new FileOutputStream(outFile);
                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = in.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }
                            out.close();
                        }
                        in.close();
                    }
                }
                jar.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // extracted from Bukkit API
    public static InputStream getResourceFromJar(String filename, ClassLoader classLoader) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        }

        try {
            URL url = classLoader.getResource(filename);

            if (url == null) {
                return null;
            }

            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }
}
