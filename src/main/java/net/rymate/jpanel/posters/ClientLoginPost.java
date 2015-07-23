package net.rymate.jpanel.posters;

import net.rymate.jpanel.Utils.PasswordHash;
import spark.Request;
import spark.Response;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.UUID;

/**
 * Created by Ryan on 07/07/2015.
 */
public class ClientLoginPost extends PosterBase {
    public ClientLoginPost(String path) {
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
                return "SUCCESS: " + sessionId.toString();
            } else {
                return "FAIL - PASSWORD INCORRECT";
            }
        } catch (Exception e) {
            return "FAIL - " + e.getMessage();
        }
    }
}
