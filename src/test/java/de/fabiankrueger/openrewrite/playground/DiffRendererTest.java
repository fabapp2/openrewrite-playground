package de.fabiankrueger.openrewrite.playground;

import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.junit.jupiter.api.Test;
import org.openrewrite.Change;

import java.util.regex.Pattern;

public class DiffRendererTest {

    @Test
    public void testColor() {
        String diff = "diff --git a/src/main/java/com/example/SingletonEjb.java b/src/main/java/com/example/SingletonEjb.java\n" +
                "index c62cf7c..9a3e118 100644\n" +
                "--- a/src/main/java/com/example/SingletonEjb.java\n" +
                "+++ b/src/main/java/com/example/SingletonEjb.java\n" +
                "@@ -1,7 +1,7 @@ openrewrite.playground.OrderImports, replace.ejb\n" +
                " package com.example;\n" +
                " \n" +
                "-import org.springframework.stereotype.Component;\n" +
                " import org.springframework.beans.factory.annotation.Autowired;\n" +
                "+import org.springframework.stereotype.Component;\n" +
                " \n" +
                " @Component\n" +
                " public class SingletonEjb {";

                    AttributedStringBuilder b = new AttributedStringBuilder();
                    b.append("\n");
                    b.append(diff);
                    b.styleMatches(Pattern.compile("-.*"), AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
                    b.styleMatches(Pattern.compile("\\+.*"), AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
                    b.append("\n");

        System.out.println(b.toAnsi());

    }
}
