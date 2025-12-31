package me.marin.worldbopperplugin.util;

import lombok.Getter;
import lombok.Setter;
import me.marin.worldbopperplugin.WorldBopperPlugin;
import me.marin.worldbopperplugin.io.WorldBopperSettings;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.util.ExceptionUtil;

import java.util.*;

public class SpecialPrefix {

    public static final Map<String, SpecialPrefix> ALL_PREFIXES;
    static {
        Map<String, SpecialPrefix> prefixes = new LinkedHashMap<>();
        // Neither demo nor set speedruns use the wall, so they're not included for now.
        // prefixes.put("Demo Speedrun #", new SpecialPrefix("demo-attempts.txt", WorldBopperSettings.KeepCondition.END));
        // prefixes.put("Set Speedrun #", new SpecialPrefix("ssg-attempts.txt", WorldBopperSettings.KeepCondition.END));
        prefixes.put("Random Speedrun #", new SpecialPrefix("Random Speedrun #", "rsg-attempts.txt", 100, WorldBopperSettings.KeepCondition.NETHER));

        ALL_PREFIXES = Collections.unmodifiableMap(prefixes);
    }

    private final String prefix;

    @Getter
    private final String fileName;

    @Getter
    private final WorldBopperSettings.KeepCondition defaultKeepCondition;

    @Getter
    private final int keepLatest;

    /**
     * Highest world number (that's found at the end of the world name) that is safe to be deleted.
     * Leaderboard verification requires 5 previously generated worlds, but if you reset on the wall a lot (without
     * using this), those worlds would be bopped.
     */
    @Setter @Getter
    private volatile int safeDeleteUpTo = 0;

    public SpecialPrefix(String prefix, String fileName, int keepLatest, WorldBopperSettings.KeepCondition defaultKeepCondition) {
        this.prefix = prefix;
        this.fileName = fileName;
        this.defaultKeepCondition = defaultKeepCondition;
        this.keepLatest = keepLatest;
    }

    public static SpecialPrefix getSpecialPrefix(String str) {
        if (str == null) {
            return null;
        }
        for (String p : ALL_PREFIXES.keySet()) {
            if (str.startsWith(p)) {
                return ALL_PREFIXES.get(p);
            }
        }
        return null;
    }

    public static boolean isSpecialPrefixExact(String str) {
        return ALL_PREFIXES.containsKey(str);
    }

    public boolean canDelete(String worldName) {
        return getWorldNumber(worldName) <= safeDeleteUpTo;
    }

    private int getWorldNumber(String worldName) {
        try {
            return Integer.parseInt(worldName.replaceFirst("^"+prefix, ""));
        } catch (Exception e) {
            WorldBopperPlugin.log(Level.ERROR, "Could not parse world name '" + worldName + "':\n" + ExceptionUtil.toDetailedString(e));
        }
        return -1;
    }

}
