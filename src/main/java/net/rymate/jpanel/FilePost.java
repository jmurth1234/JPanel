package net.rymate.jpanel;

import spark.Request;
import spark.Response;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by Ryan on 09/07/2015.
 */
public class FilePost extends PosterBase{

    public FilePost(String path) {
        super(path);
    }

    @Override
    Object getResponse(Request request, Response response) {
        if (!isLoggedIn(request.cookie("loggedin")))
            return 0;

        if (getSessions().getAuthedUser(request.cookie("loggedin")).canEditFiles)
            return 0;

        String splat = "";
        for (String file : request.splat()) {
            splat = splat + file;
        }
        splat = splat + "/";

        File file = new File(new File(".").getAbsolutePath() + "/" + splat);

        if (!file.exists()) {
            return false;
        }

        if (!file.isDirectory()) {
            String text = request.body();
            try {
                file.delete();
                file.createNewFile();
                PrintWriter out = new PrintWriter(file);
                out.print(text);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            return false;
        }

        return true;
    }
}
