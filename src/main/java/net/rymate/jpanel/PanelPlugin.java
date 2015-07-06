package net.rymate.jpanel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jdk.nashorn.internal.ir.RuntimeNode;
import net.rymate.jpanel.Utils.Lag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.java_websocket.drafts.Draft_17;
import spark.ModelAndView;
import spark.template.handlebars.HandlebarsTemplateEngine;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.util.*;

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
    private HashMap<String, PanelUser> users;
    private HashMap<String, String> sessions = new HashMap<>();
    private FileConfiguration config;

    private int httpPort = 4567;
    private int socketPort = 9003;


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
        users.clear();
    }

    public void onEnable() {
        Lag lag = Lag.getInstance();

        getServer().getScheduler().scheduleSyncRepeatingTask(this, lag, 100L, 1L);

        config = getConfig();

        users = new HashMap<>();

        if (config.isConfigurationSection("users")) {
            // load the users
            for (String key : config.getConfigurationSection("users").getKeys(false)) {
                users.put(key, (PanelUser) config.get("users." + key));
            }
        }

        config.set("http-port", config.get("http-port", httpPort));
        config.set("websocket-port", config.get("websocket-port", socketPort));

        httpPort = config.getInt("http-port");
        socketPort = config.getInt("websocket-port");

        saveConfig();

        // init spark server
        setupSpark();
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
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(args[1].getBytes());

                byte byteData[] = md.digest();

                //convert the byte to hex format method 1
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < byteData.length; i++) {
                    sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
                }

                PanelUser user = new PanelUser(sb.toString(), false);

                users.put(args[0], user);
            } catch (Exception e) {
                e.printStackTrace();
                return true;
            }

            config.set("users", users);
            saveConfig();

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

    private void setupSpark() {
        staticFileLocation("/public");
        port(httpPort);

        get("/", (req, res) -> {
            Map map = new HashMap();
            String version = getServer().getVersion();
            map.put("version", version);

            if (req.cookie("theme") != null) {
                if (req.cookie("theme").equals("dark"))
                    map.put("dark", true);
            }

            if (sessions.containsKey(req.cookie("loggedin"))) {
                return new ModelAndView(map, "index.hbs");
            } else {
                return new ModelAndView(map, "login.hbs");
            }

        }, new HandlebarsTemplateEngine());

        get("/files", (req, res) -> {
            Map map = new HashMap();
            String version = getServer().getVersion();
            map.put("version", version);

            if (req.cookie("theme") != null) {
                if (req.cookie("theme").equals("dark"))
                    map.put("dark", true);
            }

            if (sessions.containsKey(req.cookie("loggedin"))) {
                return new ModelAndView(map, "file-manager.hbs");
            } else {
                return new ModelAndView(map, "login.hbs");
            }

        }, new HandlebarsTemplateEngine());

        get("/players", (req, res) -> {
            Map map = new HashMap();
            String version = getServer().getVersion();
            if (req.cookie("theme") != null) {
                if (req.cookie("theme").equals("dark"))
                    map.put("dark", true);
            }
            map.put("version", version);

            List<Map> names = new ArrayList<Map>();

            for (Player p : getServer().getOnlinePlayers()) {
                Map playerMap = new HashMap();
                playerMap.put("name", p.getName());
                playerMap.put("health", p.getHealth());
                names.add(playerMap);
            }

//            for (int i = 0; i < 10; i++) {
//                Map playerMap = new HashMap();
//                playerMap.put("name", "player" + i);
//                playerMap.put("health", 18);
//                names.add(playerMap);
//            }

            map.put("players", names);

            if (sessions.containsKey(req.cookie("loggedin"))) {
                return new ModelAndView(map, "players.hbs");
            } else {
                return new ModelAndView(map, "login.hbs");
            }

        }, new HandlebarsTemplateEngine());

        get("/player/:name/:action", (request, response) -> {
            if (!sessions.containsKey(request.cookie("loggedin")))
                return 0;

            managePlayer(request.params(":name"), request.params(":action"));

            return "OK";
        });

        get("/stats", "application/json", (request, response) -> {
            if (!sessions.containsKey(request.cookie("loggedin")))
                return 0;

            Gson gson = new Gson();

            // Get RAM usage

            Runtime runtime = Runtime.getRuntime();
            NumberFormat format = NumberFormat.getInstance();

            long allocatedMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();

            // Get CPU usage

            OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
            int processors = os.getAvailableProcessors();

            double usage = os.getSystemLoadAverage() / processors;

            long cpuUsage = Math.round(usage * 100.0D);

            // shove in a hashmap
            Map map = new HashMap();
            map.put("total", (allocatedMemory / 1024) );
            map.put("free", (freeMemory / 1024) );
            map.put("tps", Lag.getTPS());
            map.put("cpu", cpuUsage);

            return gson.toJson(map);
        });

        get("/wsport", (request, response) -> socketPort );

        post("/login", (request, response) -> {
            String username = request.raw().getParameter("username");
            String password = request.raw().getParameter("password");

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(password.getBytes());

            byte byteData[] = md.digest();

            //convert the byte to hex format method 1
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }

            if (Objects.equals(users.get(username).password, sb.toString())) {
                UUID sessionId = UUID.randomUUID();
                sessions.put(sessionId.toString(), username);
                response.cookie("loggedin", sessionId.toString(), 3600);
            }

            response.redirect("/");
            return 0;
        });

        get("/switchtheme", (request, response) -> {
            if (request.cookie("theme") == null) {
                response.cookie("theme", "dark");
            } else if (request.cookie("theme").equals("dark")) {
                response.cookie("theme", "light");
            } else {
                response.cookie("theme", "dark");
            }
            response.redirect("/");
            return 0;
        });

        get("/file/*", (request, response) -> {
            if (!sessions.containsKey(request.cookie("loggedin")))
                return 0;

            String splat = "";
            for (String file : request.splat()) {
                splat = splat + file;
            }
            splat = splat + "/";

            File file = new File(new File(".").getAbsolutePath() + "/" + splat);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            Map map = new HashMap();
            ArrayList<String> folders = new ArrayList<String>();
            ArrayList<String> files = new ArrayList<String>();

            if (!file.exists()) {
                return file;
            }

            if (file.isDirectory()) {
                for (File fileEntry : file.listFiles()) {
                    if (fileEntry.isDirectory()) {
                        folders.add(fileEntry.getName());
                    } else {
                        files.add(fileEntry.getName());
                    }
                }
            } else {
                byte[] encoded = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
                return new String(encoded, Charset.defaultCharset());
            }

            map.put("folders", folders);
            map.put("files", files);

            return gson.toJson(map);
        });

        post("/file/*", (request, response) -> {
            if (!sessions.containsKey(request.cookie("loggedin")))
                return 0;

            if (!users.get(sessions.get(request.cookie("loggedin"))).canEditFiles)
                return 0;

            String splat = "";
            for (String file : request.splat()) {
                splat = splat + file;
            }
            splat = splat + "/";

            File file = new File(new File(".").getAbsolutePath() + "/" + splat);

            if (!file.exists()) {
                return false;
            }

            if (!file.isDirectory()) {
                String text = request.body();
                file.delete();
                file.createNewFile();
                PrintWriter out = new PrintWriter(file);
                out.print(text);
                out.close();
            } else {
                return false;
            }

            return true;
        });
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

    public HashMap getSessions() {
        return sessions;
    }

    public Logger getServerLogger() {
        return logger;
    }
}
