package net.rymate.jpanel.posters;

import net.rymate.jpanel.Utils.PasswordHash;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;
import spark.Request;
import spark.Response;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.UUID;

/**
 * Created by Ryan on 07/07/2015.
 */
public class ClientLoginPost extends PosterBase {
    private Logger logger;

    public ClientLoginPost(String path, Logger logger) {
        super(path);
        this.logger = logger;
    }

    @Override
    Object getResponse(Request request, Response response) {
        String username = request.raw().getParameter("username");
        String password = request.raw().getParameter("password");

        try {
            if (PasswordHash.validatePassword(password, getSessions().getPasswordForUser(username))) {
                UUID sessionId = UUID.randomUUID();
                getSessions().addSession(sessionId.toString(), username);
                response.cookie("loggedin", sessionId.toString(), 3600);
                logger.log(Level.DEBUG, "JPanel user " + username + " logged in! IP: " + request.ip());
                return "SUCCESS: " + sessionId.toString();
            } else {
                logger.log(Level.DEBUG, "Someone failed to login with the user " + username + "! IP: " + request.ip());
                return "FAIL - PASSWORD INCORRECT";
            }
        } catch (Exception e) {
            return "FAIL - " + e.getMessage();
        }
    }
}
