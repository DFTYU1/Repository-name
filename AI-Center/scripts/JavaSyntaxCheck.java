import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import com.sun.source.util.JavacTask;

/** Parsing only: this deliberately does not pretend Android APIs are present. */
class JavaSyntaxCheck {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics=new DiagnosticCollector<>();
        List<java.io.File> sources=new ArrayList<>();
        for(String arg:args)try(java.util.stream.Stream<Path> paths=Files.walk(Path.of(arg))){paths.filter(p->p.toString().endsWith(".java")).forEach(p->sources.add(p.toFile()));}
        try(StandardJavaFileManager manager=compiler.getStandardFileManager(diagnostics,null,java.nio.charset.StandardCharsets.UTF_8)){
            JavacTask task=(JavacTask)compiler.getTask(null,manager,diagnostics,List.of("-proc:none","--release","17"),null,manager.getJavaFileObjectsFromFiles(sources));
            for(com.sun.source.tree.CompilationUnitTree ignored:task.parse()){}
        }
        boolean fail=false;
        for(Diagnostic<?> diagnostic:diagnostics.getDiagnostics())if(diagnostic.getKind()==Diagnostic.Kind.ERROR){System.out.println(diagnostic);fail=true;}
        if(fail)System.exit(1);
        System.out.println("PASS syntax parsing of "+sources.size()+" Android source files; Android type checking and APK build NOT RUN");
    }
}
