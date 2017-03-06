package net.rymate.jpanel;

import com.github.jknack.handlebars.internal.Files;
import com.vaadin.sass.ArgumentParser;
import com.vaadin.sass.internal.ScssContext;
import com.vaadin.sass.internal.ScssStylesheet;
import com.vaadin.sass.internal.handler.SCSSDocumentHandlerImpl;
import com.vaadin.sass.internal.handler.SCSSErrorHandler;
import net.rymate.jpanel.Utils.Lag;
import net.rymate.jpanel.Utils.PasswordHash;
import net.rymate.jpanel.getters.*;
import net.rymate.jpanel.posters.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.Bukkit;
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
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private static boolean debugMode = false;
    private static File resFolder;
    private ConsoleSocket socket;
    private FileConfiguration config;


	private int httpPort = 4567;
	private boolean useSsl = false;
	private String keystorePath = "";
	private String keystorePassword = "";


	private PanelSessions sessions;

    public static PanelPlugin getInstance() {
		return instance;
	}

	public void onDisable() {

        sessions.destroy();
    }

    public void onEnable() {
        Lag lag = Lag.getInstance();
        sessions = PanelSessions.getInstance();
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

        config.set("debug-mode", config.get("debug-mode", debugMode));

		config.set("use-ssl", config.get("use-ssl", useSsl));
		config.set("keystore-name", config.get("keystore-name", keystorePath));
		config.set("keystore-password", config.get("keystore-password", keystorePassword));

        httpPort = config.getInt("http-port");
        debugMode = config.getBoolean("debug-mode");
		useSsl = config.getBoolean("use-ssl");

        resFolder = new File(new File(".").getAbsolutePath() + "/JPanel-public/");
        File verFile = new File (resFolder + "/ .resVersion");

        try {
            String currVer = this.getDescription().getVersion();
            if (!resFolder.exists()) {
                resFolder.mkdir();
                resFolder.setWritable(true);
            }

            if (verFile.exists()) {
                String version = Files.read(verFile);
                if (!version.equals(currVer) || debugMode) {
                    extractResources(getClass(), "public");
                }
            } else {
                verFile.createNewFile();
                extractResources(getClass(), "public");
            }

            PrintWriter out = new PrintWriter(verFile);
            out.print(currVer);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        extractResources(getClass(), "public");

        keystorePath = getDataFolder() + "/" + config.getString("keystore-name");
		keystorePassword = config.getString("keystore-password");

		if (useSsl) {
			secure(keystorePath, keystorePassword, null, null);
		}

        saveConfig();

		webSocket("/socket", ConsoleSocket.class);

        //staticFileLocation("/public");
        externalStaticFileLocation(resFolder.getName() + "/");

        Path srcRoot = Paths.get(resFolder.toURI());

        File mainScss = srcRoot.resolve("main.scss").toFile();
        File darkScss = srcRoot.resolve("dark.scss").toFile();

        boolean compiled = true;
        try {
            compiled = compileScssFile(mainScss, srcRoot.resolve("main.css").toString());
            compiled = compileScssFile(darkScss, srcRoot.resolve("dark.css").toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!compiled) {
            System.out.println("[JPanel] Error when compiling scss file, panel may not work!");
        }

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
		new FileManager("/files/manager", this);

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

    public boolean compileScssFile(File input, String output) throws Exception {
        if (!input.canRead()) {
            System.err.println(input.getCanonicalPath() + " could not be read!");
            return false;
        }
        String inputPath = input.getAbsolutePath();

        SCSSErrorHandler errorHandler = new SCSSErrorHandler(getLogger());
        errorHandler.setWarningsAreErrors(false);
        try {
            // Parse stylesheet
            ScssStylesheet scss = ScssStylesheet.get(inputPath, null, new SCSSDocumentHandlerImpl(), errorHandler);
            if (scss == null) {
                System.err.println("The scss file " + input + " could not be found.");
                return false;
            }

            // Compile scss -> css
            scss.compile(ScssContext.UrlMode.ABSOLUTE);

            // Write result
            Writer writer = createOutputWriter(output);
            scss.write(writer, true);
            writer.close();
        } catch (Exception e) {
            throw e;
        }

        return !errorHandler.isErrorsDetected();
    }

    private static Writer createOutputWriter(String filename) throws IOException {
        if (filename == null) {
            return new OutputStreamWriter(System.out, "UTF-8");
        } else {
            File file = new File(filename);
            return new FileWriter(file);
        }
    }

    public static void debug(String s) {
        if (debugMode) System.out.println("[JPanel DEBUG] - " + s);
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
        } else if (cmd.getName().equalsIgnoreCase("jpanel")) {
            if (sender instanceof Player) {
                sender.sendMessage("This must be run by the console!");
                return true;
            }

            sender.sendMessage("This server is running JPanel " + getDescription().getVersion());
            sender.sendMessage("Made by rymate1234");

            sender.sendMessage("------ Commands ------");
            sender.sendMessage("Command   | Description");
            sender.sendMessage("/addlogin | adds a user to JPanel");
            sender.sendMessage("/passwd   | change the password of a JPanel user");
            return true;
        }
        return false;
    }

    public synchronized void managePlayer(String name, String action, String message) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (action.equalsIgnoreCase("kick"))
                    getServer().getPlayer(name).kickPlayer(message);

                if (action.equalsIgnoreCase("ban")) {
                    getServer().getPlayer(name).setBanned(true);
                    getServer().getPlayer(name).kickPlayer(message);
                }
            }
        }.runTask(this);

    }

    public Logger getServerLogger() {
        return logger;
    }

    public static void extractResources(Class<? extends JavaPlugin> pluginClass, String filePath) {
        debug("Extracting resources from " + pluginClass.getName());

        try {
            debug("Destination: " + resFolder.getPath());

            if (!resFolder.exists()) {
                resFolder.mkdir();
                resFolder.setWritable(true);
            }

            File jarFile = new File(pluginClass.getProtectionDomain().getCodeSource().getLocation().getPath().replace("%20", " "));

            if (jarFile.isFile()) {
                JarFile jar;
                jar = new JarFile(jarFile);

                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    if (name.startsWith(filePath + "/")) {
                        InputStream in = getResourceFromJar(name, pluginClass.getClassLoader());
                        name = name.replace(filePath + "/", "");
                        File outFile = new File(resFolder + "/" + name);
                        if (name.endsWith("/")) {
                            debug("Creating folder: " + outFile.getPath());
                            outFile.mkdirs();
                        } else {
                            if (outFile.isDirectory()) continue;

                            if ((outFile.getName().equals("_vars.scss") || outFile.getName().equals("custom.scss"))
                                    && outFile.exists()) {
                                continue;
                            }

                            debug("Creating file: " + outFile.getPath());

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
            logger.error("Failed to copy files to the ./JPanel-public/ folder");
            logger.error("Please report the following error to rymate1234!");
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
