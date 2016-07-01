package net.rymate.jpanel.posters;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.milkbowl.vault.permission.Permission;
import net.rymate.jpanel.PanelPlugin;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.RegisteredServiceProvider;
import spark.Request;
import spark.Response;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class allows for the renaming and deleting of files
 *
 * Created by rymate1234 on 29/07/2015.
 */
public class FileManager extends PosterBase {
    private final PanelPlugin plugin;

    public FileManager(String path, PanelPlugin plugin) {
        super(path);
        this.plugin = plugin;
    }

    @Override
    Object getResponse(Request request, Response response) {
        if (!isLoggedIn(request.cookie("loggedin")))
            return 0;

        HashMap responseMap = new HashMap();
        JsonParser parser = new JsonParser();
        JsonObject requestJson = (JsonObject) parser.parse(request.body());
		File file = null;

		if (requestJson.has("target")) {
			file = new File(new File(".").getAbsolutePath() + "/" + requestJson.get("target").getAsString());
		}

		if (file == null || !file.exists()) {
			responseMap.put("success", false);
			responseMap.put("reason", "File " + file.getName() + " does not exist");
		} else if (requestJson.has("action")) {
            String action = requestJson.get("action").getAsString();
            if (action.equalsIgnoreCase("remove")) {
				if (file.delete()) {
					responseMap.put("success", true);
				} else {
					responseMap.put("success", false);
					responseMap.put("reason", "File could not be deleted");
				}
            } else if (action.equalsIgnoreCase("rename")) {
				if (!requestJson.has("value")) {
					responseMap.put("success", false);
					responseMap.put("reason", "No new name given");
				} else {
					String value = requestJson.get("value").getAsString();
					File newFile = new File(file.getParent() + "/" + value);
					if (file.renameTo(newFile)) {
						responseMap.put("success", true);
					} else {
						responseMap.put("success", false);
						responseMap.put("reason", "File could not be renamed");
					}
				}
			}
        }

        return new GsonBuilder().setPrettyPrinting().create().toJson(responseMap);
    }

}
