package net.rymate.jpanel.getters;

import net.rymate.jpanel.PanelPlugin;
import org.bukkit.plugin.java.JavaPlugin;

import static spark.Spark.get;

/**
 * Created by Ryan on 07/07/2015.
 */
public class IndexGetter extends GetterBase {
    public IndexGetter(String path, String template, JavaPlugin plugin) {
        super(path, template, plugin);

        // needed as part of the index page
        get("/wsport", (request, response) -> ((PanelPlugin) getPlugin()).getWebSocketPort());
    }

}
