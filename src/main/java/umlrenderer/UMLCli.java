package umlrenderer;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "umlvisualizer", mixinStandardHelpOptions = true,
        version = "UMLVisualizer 0.2.0",
        description = "Visualize the UML structure of a Java source directory.")
public class UMLCli implements Callable<Integer> {
    @Parameters(index = "0", arity = "0..1", paramLabel = "SOURCE_DIRECTORY",
            description = "Java source directory to analyze (default: current directory)")
    private Path sourceDirectory = Path.of(".");

    @Option(names = {"--summary"}, description = "Print parsed types without opening a window")
    private boolean summary;

    @Override
    public Integer call() throws Exception {
        Path directory = sourceDirectory.toAbsolutePath().normalize();
        if (!Files.isDirectory(directory)) {
            System.err.println("Not a directory: " + directory);
            return 2;
        }

        UMLParser parser = new UMLParser();
        var types = parser.parseDirectory(directory);

        if (types.isEmpty()) {
            System.err.println("No Java type declarations found in " + directory);
            return 1;
        }

        System.out.printf("Analyzed %d Java types in %s%n", types.size(), directory);
        if (summary) {
            for (UMLClass type : types) {
                System.out.printf("%-18s %s%n", type.getKind(), type.getQualifiedName());
            }
            return 0;
        }

        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Cannot open a diagram in a headless environment. Use --summary.");
            return 2;
        }

        UMLVisualizer.initFrom(directory);
        return 0;
    }

    public static void main(String[] args) {
        int code = new CommandLine(new UMLCli()).execute(args);
        if (code != 0) System.exit(code);
    }
}
