package de.fabiankrueger.openrewrite.playground.openrewrite;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.*;
import org.openrewrite.config.ResourceLoader;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.java.JavaParser;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.net.URI;
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
        List<SourceFile> sourceFiles = parseSources();
        String[] recipesFound = getRecipesFound().stream()
                .map(Recipe::getName)
                .collect(Collectors.toList()).toArray(new String[]{});
        changes = new ArrayList<>(applyRecipe(sourceFiles, recipesFound));
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

    protected List<Path> listJavaSources(String sourceDirectory)  {
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
//            final File file = new File("./rewrite.yaml");
//            InputStream yamlInput = new FileInputStream(file);
            InputStream yamlInput = classPathResource.getInputStream();
            URI source = URI.create("whatever");
            Properties properties = new Properties();
            ResourceLoader resourceLoader = new YamlResourceLoader(yamlInput, source, properties);

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
