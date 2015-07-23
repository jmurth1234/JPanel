package net.rymate.jpanel.getters;

import net.rymate.jpanel.PanelPlugin;
import net.rymate.jpanel.getters.GetterBase;
import org.bukkit.entity.Player;
import spark.ModelAndView;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Ryan on 08/07/2015.
 */
public class PlayersPageGetter extends GetterBase {

    public PlayersPageGetter(String path, String template, PanelPlugin plugin) {
        super(path, template, plugin);
    }

    @Override
    protected ModelAndView getPage(Request request, Response response) {
        List<Map> names = new ArrayList<>();

        for (Player p : getPlugin().getServer().getOnlinePlayers()) {
            Map playerMap = new HashMap();
            playerMap.put("name", p.getName());
            playerMap.put("health", p.getHealth());
            names.add(playerMap);
        }

        getTemplateMap().put("players", names);
        return super.getPage(request, response);
    }
}
