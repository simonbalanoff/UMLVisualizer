package umlrenderer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class UMLCliTest {
    @TempDir
    Path tempDir;

    @Test
    void printsHelpSuccessfully() {
        assertEquals(0, new CommandLine(new UMLCli()).execute("--help"));
    }

    @Test
    void summarizesJavaProjectWithoutOpeningWindow() throws Exception {
        Files.writeString(tempDir.resolve("Example.java"), "class Example {}");
        assertEquals(0, new CommandLine(new UMLCli()).execute("--summary", tempDir.toString()));
    }

    @Test
    void rejectsMissingDirectory() {
        Path absent = tempDir.resolve("missing");
        assertEquals(2, new CommandLine(new UMLCli()).execute("--summary", absent.toString()));
    }
}
