package net.rymate.jpanel;

import java.util.HashMap;

/**
 * Class to manage sessions and users within the panel
 *
 * Created by Ryan on 07/07/2015.
 */
public class PanelSessions {
    static PanelSessions panelSessions = new PanelSessions();

    private HashMap<String, PanelUser> users = new HashMap<>();

    // First string is the session UUID, second is the username
    private HashMap<String, String> sessions = new HashMap<>();

    private PanelSessions () {}

    public static PanelSessions getInstance() {
        if (panelSessions == null)
            panelSessions = new PanelSessions();
        return panelSessions;
    }

    public void addUser(String key, PanelUser user) {
        users.put(key, user);
    }

    public boolean isLoggedIn(String loggedin) {
        return sessions.containsKey(loggedin);
    }

    public String getPasswordForUser(String username) {
        PanelUser user = users.get(username);
        return user.password;
    }

    public void addSession(String sessionKey, String username) {
        sessions.put(sessionKey, username);
    }

    public void removeSession(String sessionKey) {
        sessions.remove(sessionKey);
    }

    public PanelUser getAuthedUser(String loggedin) {
        String user = sessions.get(loggedin);
        return users.get(user);
    }

    public PanelUser getUser(String username) {
        return users.get(username);
    }

    public void destroy() {
        this.sessions = null;
        this.users = null;
        panelSessions = null;
    }

    public String getAuthedUsername(String token) {
        String user = sessions.get(token);
        return user;
    }
}
