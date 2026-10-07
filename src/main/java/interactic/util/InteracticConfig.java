package interactic.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import interactic.InteracticInit;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Plain JSON config (config/interactic.json); replaces the old owo-lib config. */
public class InteracticConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(InteracticInit.MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve(InteracticInit.MOD_ID + ".json");

    /** Client-only mode: only the visual features, safe to use on servers without Interactic. */
    @RestartRequiredOption
    public boolean clientOnlyMode = false;

    @ServerSideConfigOption @RestartRequiredOption
    public boolean rightClickPickup = true;
    @ServerSideConfigOption @RestartRequiredOption
    public boolean itemThrowing = true;
    @ServerSideConfigOption @RestartRequiredOption
    public boolean itemFilterEnabled = true;
    @ServerSideConfigOption
    public boolean itemsActAsProjectiles = true;
    @ServerSideConfigOption
    public boolean autoPickup = true;

    public boolean fancyItemRendering = true;
    public boolean renderItemTooltips = true;
    public boolean renderFullTooltip = true;
    public boolean swingArm = true;
    public boolean blocksLayFlat = true;

    public static InteracticConfig createAndLoad() {
        InteracticConfig config = new InteracticConfig();
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                InteracticConfig loaded = GSON.fromJson(reader, InteracticConfig.class);
                if (loaded != null) config = loaded;
            } catch (IOException | JsonParseException e) {
                LOGGER.warn("Couldn't read {}, using defaults", FILE, e);
            }
        }
        config.applyClientOnlyMode();
        config.save();
        return config;
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            LOGGER.warn("Couldn't save {}", FILE, e);
        }
    }

    private void applyClientOnlyMode() {
        if (!clientOnlyMode) return;
        itemsActAsProjectiles = false;
        itemThrowing = false;
        itemFilterEnabled = false;
        autoPickup = true;
        rightClickPickup = false;
    }

    public boolean clientOnlyMode() { return clientOnlyMode; }
    public boolean rightClickPickup() { return rightClickPickup; }
    public boolean itemThrowing() { return itemThrowing; }
    public boolean itemFilterEnabled() { return itemFilterEnabled; }
    public boolean itemsActAsProjectiles() { return itemsActAsProjectiles; }
    public boolean autoPickup() { return autoPickup; }
    public boolean fancyItemRendering() { return fancyItemRendering; }
    public boolean renderItemTooltips() { return renderItemTooltips; }
    public boolean renderFullTooltip() { return renderFullTooltip; }
    public boolean swingArm() { return swingArm; }
    public boolean blocksLayFlat() { return blocksLayFlat; }

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface RestartRequiredOption {}
}
