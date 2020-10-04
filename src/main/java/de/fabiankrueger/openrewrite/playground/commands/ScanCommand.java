package de.fabiankrueger.openrewrite.playground.commands;

import de.fabiankrueger.openrewrite.playground.openrewrite.ProjectContext;
import de.fabiankrueger.openrewrite.playground.openrewrite.ProjectContextHolder;
import lombok.RequiredArgsConstructor;
import org.jline.utils.AttributedString;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.nio.file.Path;

@ShellComponent
@RequiredArgsConstructor
public class ScanCommand {

    private final ProjectContextHolder projectContextHolder;
    private final ScanCommandRenderer scanCommandRenderer;

    @ShellMethod("Scans given project root and print applicable recipes")
    public AttributedString scan(String projectRootString) {
        Path projectRoot = Path.of(projectRootString);
        final ProjectContext projectContext = projectContextHolder.getOrCreateProjectContext(projectRoot.toAbsolutePath().toString());
        projectContext.scan();
        return scanCommandRenderer.render(projectContext);
    }
}
