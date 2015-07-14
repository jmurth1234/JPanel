package net.rymate.jpanel.posters;

import net.rymate.jpanel.Utils.PasswordHash;
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
    public LoginPost(String path) {
        super(path);
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
