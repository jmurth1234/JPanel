package net.rymate.test;

import net.rymate.jpanel.PanelNavigation;
import net.rymate.jpanel.PanelPlugin;
import net.rymate.jpanel.getters.GetterBase;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Created by Ryan on 01/08/2015.
 */
public class MainClass extends JavaPlugin {

    public void onEnable() {
        // extract the resource from the jar file
        saveResource("test.hbs", true);

        PanelPlugin.extractResources(getClass(), "public");

        new TestGetter("/test", new File(getDataFolder() + "/test.hbs"), this);
        PanelNavigation.getInstance().registerExternalPath("/test", "Test Path");
    }

    private class TestGetter extends GetterBase{
        public TestGetter(String s, File s1, MainClass mainClass) {
            super(s, s1, mainClass);
        }
    }
}
