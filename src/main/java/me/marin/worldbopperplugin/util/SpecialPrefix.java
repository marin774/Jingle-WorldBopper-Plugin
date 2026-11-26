package me.marin.worldbopperplugin.util;

import lombok.Getter;
import me.marin.worldbopperplugin.io.WorldBopperSettings;

import java.util.*;

public class SpecialPrefix {

    public static final Map<String, SpecialPrefix> ALL_PREFIXES;
    static {
        Map<String, SpecialPrefix> prefixes = new LinkedHashMap<>();
        // Neither demo nor set speedruns use the wall, so they're not included for now.
        // prefixes.put("Demo Speedrun #", new SpecialPrefix("demo-attempts.txt", WorldBopperSettings.KeepCondition.END));
        // prefixes.put("Set Speedrun #", new SpecialPrefix("ssg-attempts.txt", WorldBopperSettings.KeepCondition.END));
        prefixes.put("Random Speedrun #", new SpecialPrefix("Random Speedrun #", 100, WorldBopperSettings.KeepCondition.NETHER));

        ALL_PREFIXES = Collections.unmodifiableMap(prefixes);
    }

    private final String prefix;

    @Getter
    private final WorldBopperSettings.KeepCondition defaultKeepCondition;

    @Getter
    private final int keepLatest;

    public SpecialPrefix(String prefix, int keepLatest, WorldBopperSettings.KeepCondition defaultKeepCondition) {
        this.prefix = prefix;
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

}
