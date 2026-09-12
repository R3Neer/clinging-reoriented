package io.github.r3neer.clingingreoriented.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

/** Client-only visual timing. Read once at startup, never from the render loop. */
public final class CameraConfig {
    public static final double DEFAULT_SECONDS = 0.25;
    private static double seconds = DEFAULT_SECONDS;
    public static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("clinging-reoriented-client.json"); }
    public static double durationNanos() { return seconds * 1_000_000_000.0; }

    public static void load(Path file) {
        seconds = DEFAULT_SECONDS;
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.toAbsolutePath().getParent());
                var json = new JsonObject();
                json.addProperty("cameraRotationSeconds", DEFAULT_SECONDS);
                Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(json) + "\n");
                return;
            }
            var json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if (!json.has("cameraRotationSeconds")) throw new IllegalArgumentException("Missing cameraRotationSeconds");
            var value = json.get("cameraRotationSeconds");
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Expected a number");
            double candidate = value.getAsDouble();
            if (!Double.isFinite(candidate) || candidate < .05 || candidate > 10) throw new IllegalArgumentException("Expected 0.05 to 10 seconds");
            seconds = candidate;
        } catch (IOException | RuntimeException failure) {
            // Preserve invalid user files for correction instead of overwriting them.
            LoggerFactory.getLogger("clinging_reoriented").warn("Cannot read camera timing from {}; using {} seconds", file, DEFAULT_SECONDS, failure);
        }
    }
}
