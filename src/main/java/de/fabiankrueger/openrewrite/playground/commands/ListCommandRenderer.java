package de.fabiankrueger.openrewrite.playground.commands;

import org.jline.utils.AttributedCharSequence;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.openrewrite.Recipe;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ListCommandRenderer {
    public AttributedString render(List<Recipe> foundRecipes) {
        String recipesString = foundRecipes.stream()
                .map(Recipe::getName)
                .map(name -> " - " + name)
                .collect(Collectors.joining("\n"));

        AttributedStringBuilder builder = new AttributedStringBuilder();
//        builder.style(AttributedStyle::bold);
        builder.style(AttributedStyle.DEFAULT.bold());
        builder.append("Found these recipes:");
        builder.append("\n\n");
        builder.style(AttributedStyle.DEFAULT);
        builder.append(recipesString);
        builder.append("\n");
        return builder.toAttributedString();

    }
}
