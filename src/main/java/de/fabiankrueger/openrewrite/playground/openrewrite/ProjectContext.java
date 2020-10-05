package de.fabiankrueger.openrewrite.playground.openrewrite;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.*;
import org.openrewrite.config.ResourceLoader;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.java.JavaParser;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.tree.Maven;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class ProjectContext {
    private Path projectRoot;
    private ChangesContainer changeContainer;
    private Environment environment;
    private List<Change> changes;

    public ProjectContext(Path projectRoot) {
        this.projectRoot = projectRoot;
        createEnvironment();
    }

    public ProjectContext(String path) {
        this(Paths.get(path));
    }

    public void scan() {
        List<SourceFile> projectFiles = new ArrayList<>();

        projectFiles.addAll(parseSources());
        projectFiles.addAll(parseMaven());

        String[] recipesFound = getRecipesFound().stream()
                .map(Recipe::getName)
                .collect(Collectors.toList()).toArray(new String[]{});
        final List<Change> changes = applyRecipe(projectFiles, recipesFound);
        this.changes = changes;
    }

    private List<Change> applyRecipe(List<SourceFile> sourceFiles, String... recipe) {
        final Collection<RefactorVisitor<?>> orderImportsVisitors = environment.visitors(recipe);
        final Collection<Change> fixChanges = new Refactor()
                .visit(orderImportsVisitors)
                .fix(sourceFiles);

        this.changes = new ArrayList<>(fixChanges);
        return changes;
    }

    public List<Recipe> getApplicableRecipes() {
        return this.getRecipesFound().stream()
                .filter(recipe -> isRecipeApplicable(recipe.getName()))
                .collect(Collectors.toList());
    }

    private boolean isRecipeApplicable(String recipeName) {
        final List<Change> applicableChange = getChangesForRecipe(recipeName);
        return false == applicableChange.isEmpty();
    }

    private List<SourceFile> parseSources() {
        List<SourceFile> sourceFiles = new ArrayList<>();
        List<Path> javaSources = new ArrayList<>();
        javaSources.addAll(listJavaSources(projectRoot.resolve("src/main/java").toString()));
        sourceFiles.addAll(JavaParser.fromJavaVersion()
                .build()
                .parse(javaSources, this.projectRoot));
        return sourceFiles;
    }

    private List<Maven.Pom> parseMaven() {
        List<Path> allPoms = new ArrayList<>();
        allPoms.add(projectRoot.resolve("pom.xml"));
//        // children
//        if(project.getCollectedProjects() != null) {
//            project.getCollectedProjects().stream()
//                    .filter(collectedProject -> collectedProject != project)
//                    .map(collectedProject -> collectedProject.getFile().toPath())
//                    .forEach(allPoms::add);
//        }
//
//        // parents
//        MavenProject parent = project.getParent();
//        while (parent != null && parent.getFile() != null) {
//            allPoms.add(parent.getFile().toPath());
//            parent = parent.getParent();
//        }

        List<Maven.Pom> mavenPom =  MavenParser
                .builder()
                .resolveDependencies(true)
                .build()
                .parse(allPoms, this.projectRoot);
        return mavenPom;
    }

    protected List<Path> listJavaSources(String sourceDirectory) {
        File sourceDirectoryFile = new File(sourceDirectory);
        if (!sourceDirectoryFile.exists()) {
            return Collections.emptyList();
        }

        Path sourceRoot = sourceDirectoryFile.toPath();
        try {
            return Files.walk(sourceRoot)
                    .filter(f -> !Files.isDirectory(f) && f.toFile().getName().endsWith(".java"))
                    .collect(toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ArrayList<Recipe> getRecipesFound() {
        final ArrayList<Recipe> recipes = new ArrayList<>();
        recipes.addAll(environment.getRecipesByName().values());
        return recipes;
    }

    @NotNull
    private Environment createEnvironment() {

        try {
            ClassPathResource classPathResource = new ClassPathResource("META-INF/rewrite.yaml");
            InputStream yamlInput = classPathResource.getInputStream();
            Properties properties = new Properties();
            ResourceLoader resourceLoader = new YamlResourceLoader(yamlInput, classPathResource.getURI(), properties);

            environment = Environment.builder()
                    .load(resourceLoader)
//                .loadVisitors(visitors)
                    .build();

            return environment;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not find rewrite.yaml", e);
        } catch (IOException e) {
            throw new RuntimeException("Could not find rewrite.yaml", e);
        }

    }

    public List<Change> getChanges() {
        return (List<Change>) this.changes;
    }

    public List<Change> getChangesForRecipe(String recipeName) {
        final Collection<RefactorVisitor<?>> visitors = environment.visitors(recipeName);
        final List<String> visitorNames = visitors.stream().map(visitor -> visitor.getName()).collect(toList());

        return changes.stream()
                .filter(change -> change.getVisitorsThatMadeChanges().containsAll(visitorNames))
                .collect(toList());
    }
}
