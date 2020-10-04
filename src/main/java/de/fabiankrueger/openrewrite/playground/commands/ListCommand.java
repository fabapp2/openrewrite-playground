package de.fabiankrueger.openrewrite.playground.commands;

import de.fabiankrueger.openrewrite.playground.openrewrite.ProjectContext;
import de.fabiankrueger.openrewrite.playground.openrewrite.ProjectContextHolder;
import lombok.RequiredArgsConstructor;
import org.jline.utils.AttributedString;
import org.openrewrite.Recipe;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.nio.file.Path;
import java.util.List;

@ShellComponent
@RequiredArgsConstructor
public class ListCommand {

    private final ListCommandRenderer listCommandRenderer;
    private final ProjectContextHolder projectContextHolder;

    @ShellMethod("List available recipes.")
    public AttributedString list(String projectRootString) {
        Path projectRoot = Path.of(projectRootString);
        ProjectContext projectContext = projectContextHolder.getOrCreateProjectContext(projectRoot.toAbsolutePath().toString());
        List<Recipe> foundRecipes = projectContext.getRecipesFound();
        return listCommandRenderer.render(foundRecipes);
    }
}
