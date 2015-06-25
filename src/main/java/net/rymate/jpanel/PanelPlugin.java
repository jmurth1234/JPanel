package net.rymate.jpanel;

import com.google.gson.Gson;
import jdk.nashorn.internal.ir.RuntimeNode;
import net.rymate.jpanel.Utils.Lag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.java_websocket.drafts.Draft_17;
import spark.ModelAndView;
import spark.template.handlebars.HandlebarsTemplateEngine;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.UnknownHostException;
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
    private HashMap<String, String> users;
    private ArrayList<String> sessions = new ArrayList();
    private FileConfiguration config;

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
        // init spark server
        setupSpark();
        setupWS();
        Lag lag = Lag.getInstance();

        getServer().getScheduler().scheduleSyncRepeatingTask(this, lag, 100L, 1L);

        config = getConfig();

        users = new HashMap<>();
        //Once again, you'll need to set up the HashMap and config somewhere else
        for (String key : config.getConfigurationSection("users").getKeys(false))
        {
            users.put(key, config.getString("users." + key));
        }

        System.out.println("PanelPlugin enabled!");
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

                users.put(args[0], sb.toString());
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
        int port = 9003;
        try {
            socket = new ConsoleSocket( port, new Draft_17(), this );
            socket.start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        System.out.println("Started WebSocket server!");
    }

    private void setupSpark() {
        staticFileLocation("/public");

        get("/", (req, res) -> {
            Map map = new HashMap();
            String version = getServer().getVersion();
            map.put("version", version);

            if (sessions.contains(req.cookie("loggedin"))) {
                return new ModelAndView(map, "index.hbs");
            } else {
                return new ModelAndView(map, "login.hbs");
            }

        }, new HandlebarsTemplateEngine());

        get("/players", (req, res) -> {
            Map map = new HashMap();
            String version = getServer().getVersion();
            map.put("version", version);

            List<String> names = new ArrayList<String>();

            for (Player p : getServer().getOnlinePlayers()) {
                names.add(p.getName());
            }

            map.put("players", names);

            if (sessions.contains(req.cookie("loggedin"))) {
                return new ModelAndView(map, "players.hbs");
            } else {
                return new ModelAndView(map, "login.hbs");
            }

        }, new HandlebarsTemplateEngine());

        get("/stats", "application/json", (request, response) -> {
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

            if (usage < 0.0D) {
                return "0";
            }

            long cpuUsage = Math.round(usage * 100.0D);

            // shove in a hashmap
            Map map = new HashMap();
            map.put("total", (allocatedMemory / 1024) );
            map.put("free", (freeMemory / 1024) );
            map.put("tps", Lag.getTPS());
            map.put("cpu", cpuUsage);

            return gson.toJson(map);
        });

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

            if (Objects.equals(users.get(username), sb.toString())) {
                UUID sessionId = UUID.randomUUID();
                sessions.add(sessionId.toString());
                response.cookie("loggedin", sessionId.toString(), 3600);
            }

            response.redirect("/");
            return 0;
        });
    }

    public Logger getServerLogger() {
        return logger;
    }
}
