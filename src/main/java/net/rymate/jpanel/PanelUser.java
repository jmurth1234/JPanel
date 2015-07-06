package net.rymate.jpanel;

/**
 * Created by Ryan on 06/07/2015.
 */
public class PanelUser implements java.io.Serializable {
    public final String password;
    public final boolean canEditFiles;

    public PanelUser(String password, boolean canEditFiles) {
        this.password = password;
        this.canEditFiles = canEditFiles;
    }
}
