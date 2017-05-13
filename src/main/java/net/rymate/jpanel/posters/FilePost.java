package net.rymate.jpanel.posters;

import org.bukkit.Bukkit;
import spark.Request;
import spark.Response;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
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
		MultipartConfigElement multipartConfigElement = new MultipartConfigElement(new File("").getAbsolutePath());
		request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);

        if (!isLoggedIn(request.cookie("loggedin")))
            return 0;

        if (!getSessions().getAuthedUser(request.cookie("loggedin")).canEditFiles)
            return 0;

        String splat = "";
        for (String file : request.splat()) {
            splat = splat + file;
        }
        splat = splat + "/";

        File file = new File(splat);

        if (!file.exists()) try {
			Part filePart = request.raw().getPart("upload-btn"); //file is name of the upload form

			filePart.write(splat);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (ServletException e) { }

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
