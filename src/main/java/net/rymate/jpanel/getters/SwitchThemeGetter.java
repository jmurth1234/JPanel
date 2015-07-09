package net.rymate.jpanel.getters;

import net.rymate.jpanel.PanelPlugin;
import spark.Request;
import spark.Response;

/**
 * Created by Ryan on 08/07/2015.
 */
public class SwitchThemeGetter extends GetterBase{

    public SwitchThemeGetter(String path) {
        super(path, null);
    }

    @Override
    protected Object getText(Request request, Response response) {
        if (request.cookie("theme") == null) {
            response.cookie("theme", "dark");
        } else if (request.cookie("theme").equals("dark")) {
            response.cookie("theme", "light");
        } else {
            response.cookie("theme", "dark");
        }
        response.redirect("/");
        return 0;
    }
}
