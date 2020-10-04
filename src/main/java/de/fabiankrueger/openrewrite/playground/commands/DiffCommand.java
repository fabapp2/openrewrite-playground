package de.fabiankrueger.openrewrite.playground.commands;

import de.fabiankrueger.openrewrite.playground.openrewrite.ProjectContext;
import de.fabiankrueger.openrewrite.playground.openrewrite.ProjectContextHolder;
import lombok.RequiredArgsConstructor;
import org.jline.utils.AttributedString;
import org.openrewrite.Change;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.util.List;

@ShellComponent
@RequiredArgsConstructor
public class DiffCommand {

    private final ProjectContextHolder projectContextHolder;
    private final DiffCommandRenderer diffCommandRenderer;

    @ShellMethod("Print diff and information for an applicable recipe.")
    public AttributedString diff(String recipeName) {
        ProjectContext projectContext = projectContextHolder.getCurrent();
        List<Change> changes = projectContext.getChangesForRecipe(recipeName);
        return diffCommandRenderer.render(changes, recipeName);
    }

}
