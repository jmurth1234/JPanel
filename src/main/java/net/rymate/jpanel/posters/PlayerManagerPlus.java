package net.rymate.jpanel.posters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.milkbowl.vault.permission.Permission;
import net.rymate.jpanel.PanelPlugin;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

/**
 * This class is the base of an advanced players manager
 * Unlike the old one, this can also edit the permissions via vault
 * This was originally going to be a permissions manager, however it became obvious the vault API
 * wasn't geared towards management of groups :(
 *
 * Created by rymate1234 on 29/07/2015.
 */
public class PlayerManagerPlus extends PosterBase {
    private final Permission permission;
    private final PanelPlugin plugin;

    public PlayerManagerPlus(String path, PanelPlugin plugin) {
        super(path);
        this.plugin = plugin;

        RegisteredServiceProvider<Permission> permissionProvider =
                plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);

        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        } else {
            throw new NullPointerException("We couldn't get permissions from vault! This is probably a bug!");
        }
    }

    @Override
    Object getResponse(Request request, Response response) {
        if (!isLoggedIn(request.cookie("loggedin")))
            return 0;

        HashMap responseMap = new HashMap();
        JsonParser parser = new JsonParser();
        JsonObject requestJson = (JsonObject) parser.parse(request.body());
        String type = "";

        if (requestJson.has("type")) {
            type = requestJson.get("type").getAsString();
        }

        if (requestJson.has("action")) {
            String action = requestJson.get("action").getAsString();
            if (action.equals("list")) {
                if (type.equals("groups")) {
                    if (!permission.hasGroupSupport()) {
                        return "your permissions plugin has no groups support";
                    }

                    String[] groupsList = permission.getGroups();
                    responseMap.put("result", groupsList);
                } else if (type.equals("online_players")) {
                    ArrayList<TinyPlayer> playersList = new ArrayList<>();
                    Collection<? extends Player> players = plugin.getServer().getOnlinePlayers();
                    for (Player player : players) {
                        TinyPlayer p = new TinyPlayer();
                        p.playerName = player.getName();
                        p.playerUuid = player.getUniqueId().toString();
                        playersList.add(p);
                    }
                    responseMap.put("result", playersList);
                } else if (type.equals("all_players")) {
                    ArrayList<TinyPlayer> allPlayers = new ArrayList<>();
                    for (OfflinePlayer player : plugin.getServer().getOfflinePlayers()) {
                        TinyPlayer p = new TinyPlayer();
                        p.playerName = player.getName();
                        p.playerUuid = player.getUniqueId().toString();
                        allPlayers.add(p);
                    }
                    responseMap.put("result", allPlayers);
                } else {
                    return "Invalid type specified!";
                }
            } else if (action.equals("getgroups")) {
                if (!permission.hasGroupSupport()) {
                    return "your permissions plugin has no groups support";
                }
                UUID target;

                if (!requestJson.has("target")) {
                    return "no target";
                } else {
                    target = UUID.fromString(requestJson.get("target").getAsString());
                }

                String world = "";

                if (requestJson.has("world")) {
                    world =  requestJson.get("world").getAsString();
                }

                if (plugin.getServer().getOfflinePlayer(target).isOnline()) {
                    Player player = plugin.getServer().getPlayer(target);

                    HashMap resultMap = new HashMap();

                    resultMap.put("groups", permission.getPlayerGroups(player));
                    resultMap.put("world", player.getWorld().getName());

                    responseMap.put("result", resultMap);

                } else {
                    OfflinePlayer player = plugin.getServer().getOfflinePlayer(target);
                    if (world.equals("")) {
                        return "Please specify a world for offline players!";
                    }

                    HashMap resultMap = new HashMap();

                    resultMap.put("groups", permission.getPlayerGroups(world, player));
                    resultMap.put("world", world);

                    responseMap.put("result", resultMap);

                }

            } else if (action.equals("kick") || (action.equals("ban"))) {
                String player;

                if (!requestJson.has("target")) {
                    return "no target";
                } else {
                    player = requestJson.get("target").getAsString();
                }

                plugin.managePlayer(player, action);
            }
        }

        return new GsonBuilder().setPrettyPrinting().create().toJson(responseMap);
    }

    class TinyPlayer {
        public String playerUuid;
        public String playerName;
    }
}
