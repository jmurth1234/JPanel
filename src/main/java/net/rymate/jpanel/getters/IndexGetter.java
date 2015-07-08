package net.rymate.jpanel.getters;

import net.rymate.jpanel.PanelPlugin;

import static spark.Spark.get;

/**
 * Created by Ryan on 07/07/2015.
 */
public class IndexGetter extends GetterBase {
    public IndexGetter(String path, String template, PanelPlugin plugin) {
        super(path, template, plugin);

        // needed as part of the index page
        get("/wsport", (request, response) -> getPlugin().getWebSocketPort() );
    }

}
