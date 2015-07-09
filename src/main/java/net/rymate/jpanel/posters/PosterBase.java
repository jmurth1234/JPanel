package net.rymate.jpanel.posters;

import net.rymate.jpanel.PanelSessions;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.util.HashMap;

import static spark.Spark.get;
import static spark.Spark.post;

/**
 * Created by Ryan on 07/07/2015.
 */
public abstract class PosterBase {
    private PanelSessions sessions;
    private String template;
    private HashMap templateMap;

    public PosterBase(String path) {
        sessions = PanelSessions.getInstance();
        post(path, this::getResponse);
    }

    abstract Object getResponse(Request request, Response response);

    public String getTemplate() {
        return template;
    }

    public HashMap getTemplateMap() {
        return templateMap;
    }

    public boolean isLoggedIn(String token) {
        return sessions.isLoggedIn(token);
    }

    public PanelSessions getSessions() {
        return sessions;
    }
}
