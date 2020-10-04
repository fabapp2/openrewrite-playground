package de.fabiankrueger.openrewrite.playground.commands;

import de.fabiankrueger.openrewrite.playground.openrewrite.ProjectContext;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.openrewrite.Change;
import org.openrewrite.Recipe;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ScanCommandRenderer {
    public AttributedString render(ProjectContext projectContext) {
        List<Change> changes = projectContext.getChanges();
        List<Recipe> applicableRecipes = projectContext.getApplicableRecipes();

        String recipesString = applicableRecipes.stream()
                .map(Recipe::getName)
                .map(name -> " - " + name)
                .collect(Collectors.joining("\n"));

        AttributedStringBuilder builder = new AttributedStringBuilder();
        builder.style(AttributedStyle.DEFAULT.bold());
        builder.append("Applicable recipes:");
        builder.append("\n\n");
        builder.style(AttributedStyle.DEFAULT);
        builder.append(recipesString);
        builder.append("\n");
        return builder.toAttributedString();
    }
}
