import javax.tools.ToolProvider;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

/** Offline test runner; invokes the JDK compiler without depending on a javac executable in PATH. */
public class TestCore {
    public static void main(String[] args) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("A Java 17 JDK with jdk.compiler is required");
        Path output = Path.of("build/core-tests"); Files.createDirectories(output);
        int result = compiler.run(null, System.out, System.err, "-encoding", "UTF-8", "-d", output.toString(),
            "src/main/java/dev/crowncinder/progress/Progress.java", "src/test/java/dev/crowncinder/progress/ProgressTest.java");
        if (result != 0) throw new IllegalStateException("Compilation failed: " + result);
        try (var loader = new URLClassLoader(new java.net.URL[]{output.toUri().toURL()})) {
            loader.loadClass("dev.crowncinder.progress.ProgressTest").getMethod("main", String[].class).invoke(null, (Object) new String[0]);
        }
    }
}
