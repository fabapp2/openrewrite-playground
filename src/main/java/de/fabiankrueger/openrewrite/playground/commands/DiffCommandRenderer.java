package de.fabiankrueger.openrewrite.playground.commands;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.openrewrite.Change;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class DiffCommandRenderer {
    public AttributedString render(List<Change> changes, String recipeName) {


        AttributedStringBuilder builder = new AttributedStringBuilder();
        builder.style(AttributedStyle.DEFAULT.bold());
        builder.append("Changes for recipe '"+ recipeName +"'");
        builder.style(AttributedStyle.DEFAULT);
        builder.append("\n\n");

        changes.stream()
                .map(Change::diff)
                .forEach(diff -> {
                    AttributedStringBuilder b = new AttributedStringBuilder();
                    b.append("\n");
                    b.append(diff);
                    b.styleMatches(Pattern.compile("-.*"), AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
                    b.styleMatches(Pattern.compile("\\+.*"), AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
                    b.append("\n");
                    builder.append(b.toAnsi());
                });



        builder.append("\n");
        return builder.toAttributedString();
    }
}
