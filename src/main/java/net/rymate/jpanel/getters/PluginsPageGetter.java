package net.rymate.jpanel.getters;

import net.rymate.jpanel.PanelPlugin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import spark.ModelAndView;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Ryan on 26/06/2017.
 */
public class PluginsPageGetter extends GetterBase {
    public PluginsPageGetter(String path, String template, PanelPlugin plugin) {
        super(path, template, plugin);
    }

    @Override
    protected ModelAndView getPage(Request request, Response response) {
        List<Map> names = new ArrayList<>();

        for (Plugin p : getPlugin().getServer().getPluginManager().getPlugins()) {
            Map pluginMap = new HashMap();
            pluginMap.put("name", p.getName());
            pluginMap.put("enabled", p.isEnabled());
            names.add(pluginMap);
        }

        getTemplateMap().put("plugins", names);
        return super.getPage(request, response);
    }

}