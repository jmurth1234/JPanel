package net.rymate.jpanel;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ryan on 06/07/2015.
 */
public class PanelUser  {
    public final String password;
    public final boolean canEditFiles;
    public final boolean canChangeGroups;
    public final boolean canSendCommands;

    public PanelUser(String password, boolean canEditFiles, boolean canChangeGroups, boolean canSendCommands) {
        this.password = password;
        this.canEditFiles = canEditFiles;
        this.canChangeGroups = canChangeGroups;
        this.canSendCommands = canSendCommands;
    }

}
