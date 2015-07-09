package net.rymate.jpanel.posters;

import spark.Request;
import spark.Response;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update(password.getBytes());

        byte byteData[] = md.digest();

        //convert the byte to hex format method 1
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }

        if (Objects.equals(getSessions().getPasswordForUser(username), sb.toString())) {
            UUID sessionId = UUID.randomUUID();
            getSessions().addSession(sessionId.toString(), username);
            response.cookie("loggedin", sessionId.toString(), 3600);
        }

        response.redirect("/");
        return 0;
    }
}
