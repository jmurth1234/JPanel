package net.rymate.jpanel.posters;

import net.rymate.jpanel.Utils.PasswordHash;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;
import spark.Request;
import spark.Response;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Objects;
import java.util.UUID;

/**
 * Created by Ryan on 07/07/2015.
 */
public class LoginPost extends PosterBase {
    private Logger logger;

    public LoginPost(String path, Logger logger) {
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
                logger.log(Level.INFO, "JPanel user " + username + " logged in! IP: " + request.ip());
                response.cookie("loggedin", sessionId.toString(), 3600);
            } else {
                logger.log(Level.INFO, "Someone failed to login with the user " + username + "! IP: " + request.ip());
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }

        response.redirect("/");
        return 0;
    }
}
