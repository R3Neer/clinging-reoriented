package io.github.r3neer.clingingreoriented.client;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

public final class CameraConfigTest {
    @TempDir Path directory;
    @Test void writesQuarterSecondDefaultAndReadsCustomDuration() throws Exception {
        var file=directory.resolve("camera.json");CameraConfig.load(file);
        assertEquals(250_000_000.0,CameraConfig.durationNanos());
        assertTrue(Files.readString(file).contains("0.25"));
        Files.writeString(file,"{\"cameraRotationSeconds\":0.4}");CameraConfig.load(file);
        assertEquals(400_000_000.0,CameraConfig.durationNanos());
    }
    @Test void rejectsInvalidValuesWithoutDestroyingFile() throws Exception {
        var file=directory.resolve("camera.json");
        for(var input:new String[]{"broken","null","{\"cameraRotationSeconds\":0}","{\"cameraRotationSeconds\":-2}","{\"cameraRotationSeconds\":11}","{\"cameraRotationSeconds\":1e999}","{\"cameraRotationSeconds\":\"slow\"}"}){
            Files.writeString(file,input);CameraConfig.load(file);
            assertEquals(250_000_000.0,CameraConfig.durationNanos());assertEquals(input,Files.readString(file));
        }
    }
}
