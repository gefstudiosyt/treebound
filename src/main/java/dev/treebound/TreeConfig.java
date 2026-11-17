package dev.treebound;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** config/treebound.json (restart the game after editing). */
public class TreeConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("treebound");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static TreeConfig current = new TreeConfig();

    public String _help = "Growth points (xp) per second: sunlight, and while rooted (sneak, standing still on dirt/grass). stage*Xp = points needed for each stage.";
    public int xpPerSecondInSun = 1;
    public int xpPerSecondRooted = 3;
    public int stage1Xp = 300;
    public int stage2Xp = 1500;
    public int stage3Xp = 6000;
    public int snareCooldownSeconds = 20;
    public double snareRadius = 10.0;

    public static TreeConfig get() {
        return current;
    }

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("treebound.json");
        try {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file)) {
                    TreeConfig loaded = GSON.fromJson(r, TreeConfig.class);
                    if (loaded != null) current = loaded;
                }
            } else {
                try (Writer w = Files.newBufferedWriter(file)) {
                    GSON.toJson(current, w);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Could not read {} - using defaults.", file, e);
        }
    }
}
