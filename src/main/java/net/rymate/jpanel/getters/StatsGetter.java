package net.rymate.jpanel.getters;

import com.google.gson.Gson;
import net.rymate.jpanel.Utils.Lag;
import spark.Request;
import spark.Response;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ryan on 07/07/2015.
 */
public class StatsGetter extends GetterBase {

    public StatsGetter(String path) {
        super(path, null);
    }

    @Override
    protected Object getText(Request request, Response response) {
        if (!isLoggedIn(request.cookie("loggedin")))
            return 0;

        Gson gson = new Gson();

        // Get RAM usage

        Runtime runtime = Runtime.getRuntime();
        NumberFormat format = NumberFormat.getInstance();

        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();

        // Get CPU usage

        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        int processors = os.getAvailableProcessors();

        //system load is -1 if you use it with windows
        double usage = os.getSystemLoadAverage() / processors;

        long cpuUsage = Math.round(usage * 100.0D);

        //works for all platforms, but only if the Oracle JRE/JDK is installed
        if (os instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean oracleOs = (com.sun.management.OperatingSystemMXBean) os;
            cpuUsage = (long) (oracleOs.getSystemCpuLoad() * 100);
        }

        // shove in a hashmap
        Map map = new HashMap();
        map.put("total", (allocatedMemory / 1024) );
        map.put("free", (freeMemory / 1024) );
        map.put("tps", Lag.getTPS());
        map.put("cpu", cpuUsage);

        return gson.toJson(map);
    }
}
