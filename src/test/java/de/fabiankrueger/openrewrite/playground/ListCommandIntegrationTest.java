package de.fabiankrueger.openrewrite.playground;

import de.fabiankrueger.openrewrite.playground.commands.ListCommand;
import org.jline.utils.AttributedString;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ListCommandIntegrationTest {

    @Autowired
    private ListCommand sut;

//    @Test
    @Disabled
    void list() {
        final AttributedString list = sut.list(".");
    }
}