package de.fabiankrueger.openrewrite.playground.openrewrite;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ProjectContextHolder {
    private final Map<String, ProjectContext> projectContexts = new HashMap<>();
    private String currentPath;

    public ProjectContext getOrCreateProjectContext(String path) {
        if( ! projectContexts.containsKey(path)) {
            // TODO: use factory to create context
            ProjectContext projectContext = new ProjectContext(path);
            add(path, projectContext);
        }
        this.currentPath = path;
        return projectContexts.get(path);
    }

    public void add(String path, ProjectContext projectContext) {
        this.projectContexts.put(path, projectContext);
    }

    public ProjectContext getCurrent() {
        return getOrCreateProjectContext(currentPath);
    }
}
