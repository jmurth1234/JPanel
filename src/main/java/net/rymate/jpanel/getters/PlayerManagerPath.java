package net.rymate.jpanel.getters;

import net.rymate.jpanel.PanelPlugin;
import net.rymate.jpanel.getters.GetterBase;
import spark.Request;
import spark.Response;

import java.awt.*;

/**
 * Created by Ryan on 09/07/2015.
 */
public class PlayerManagerPath extends GetterBase {
    public PlayerManagerPath(String path, PanelPlugin plugin) {
        super(path, plugin);
        setPlugin(plugin);
    }

    @Override
    protected Object getText(Request request, Response response) throws Exception {
        if (!isLoggedIn(request.cookie("loggedin")))
            return 0;

        String msg = (request.params(":action").equals("kick")) ? "Kicked!" : "Banned!";

        ((PanelPlugin)getPlugin()).managePlayer(request.params(":name"), request.params(":action"), msg);

        return "OK";
    }
}
