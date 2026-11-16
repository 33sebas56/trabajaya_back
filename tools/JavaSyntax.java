import java.nio.file.*;
import java.util.*;
import javax.tools.*;
import com.sun.source.util.JavacTask;
public class JavaSyntax {
    public static void main(String[] args) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("JDK required");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var manager = compiler.getStandardFileManager(diagnostics, null, java.nio.charset.StandardCharsets.UTF_8)) {
            var paths = Files.walk(Path.of(args[0])).filter(p -> p.toString().endsWith(".java")).toList();
            var task = (JavacTask) compiler.getTask(null, manager, diagnostics, List.of("--release", "21", "-proc:none"), null, manager.getJavaFileObjectsFromPaths(paths));
            int count = 0;
            for (var unit : task.parse()) count++;
            diagnostics.getDiagnostics().forEach(System.out::println);
            boolean error = diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR);
            if (error) System.exit(1);
            System.out.println("PASS: parsed " + count + " Java 21 files (syntax only, not dependency resolution).");
        }
    }
}
