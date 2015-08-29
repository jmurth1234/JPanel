package net.rymate.jpanel;

import net.rymate.jpanel.getters.GetterBase;
import org.apache.logging.log4j.core.Logger;
import spark.Request;
import spark.Response;

/**
 * Created by Ryan on 29/08/2015.
 */
public class LogoutPath extends GetterBase {
    public LogoutPath(String path) {
        super(path, null);
    }

    @Override
    protected Object getText(Request request, Response response) {
        if (!isLoggedIn(request.cookie("loggedin")))
            return 0;

        PanelSessions.getInstance().removeSession(request.cookie("loggedin"));

        response.redirect("/");
        return 0;
    }
}
