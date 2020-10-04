package de.fabiankrueger.openrewrite.playground;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.*;
import org.openrewrite.config.ResourceLoader;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.java.JavaParser;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class EnvironmentScan {

    public static void main(String[] args) throws IOException {
        EnvironmentScan environmentScan = new EnvironmentScan();
        environmentScan.scan();
    }

    public void scan() throws IOException {

        Environment env = createEnvironment();


        String orderImportRecipe = "playground.OrderImportsRecipe";
        final String replaceEjbRecipe = "replace.ejb";

        List<SourceFile> sourceFiles = parseSources();

        System.out.println("");
        System.out.println("### OrderImport Recipe... ###");
        System.out.println("");

        Collection<Change> orderImportChanges = applyRecipe(env, sourceFiles, orderImportRecipe);
        printChangeReport(orderImportChanges);

        System.out.println("");
        System.out.println("### Replace Ejb Recipe... ###");
        System.out.println("");

        Collection<Change> replaceEjbChanges = applyRecipe(env, sourceFiles, replaceEjbRecipe);
        printChangeReport(replaceEjbChanges);

        System.out.println("");
        System.out.println("### ALL RECIPES IN ONE RUN... ###");
        System.out.println("");

        List<SourceFile> sourceFiles2 = parseSources();
        Collection<Change> allChanges = applyRecipe(env, sourceFiles2, replaceEjbRecipe, orderImportRecipe);
        printChangeReport(allChanges);


        // get recipe by visitorThatMadeChanges
        printRecipeApplicable(env, replaceEjbRecipe, allChanges);
        printRecipeApplicable(env, orderImportRecipe, allChanges);
    }

    private void printRecipeApplicable(Environment env, String recipeName, Collection<Change> allChanges) {
        final Collection<RefactorVisitor<?>> replaceEjbVisitors = env.visitors(recipeName);
        final List<String> visitorNames = replaceEjbVisitors.stream().map(visitor -> visitor.getName()).collect(toList());

        final List<Change> applicableChange = allChanges.stream()
                .filter(change -> change.getVisitorsThatMadeChanges()
                        .containsAll(visitorNames))
                .collect(toList());

        final Recipe recipe = env.getRecipesByName().get(recipeName);

        System.out.println("");
        if(applicableChange.isEmpty()) {
            System.out.println("Recipe " + recipe.getName() + " is NOT applicable");
        } else {
            System.out.println("Recipe " + recipe.getName() + " is applicable to " + applicableChange.size() + " Files.");
            System.out.println("These are the applicable files:");
            System.out.println(applicableChange.stream().map(Change::getOriginal)
            .map(SourceFile::getSourcePath).collect(Collectors.joining("\n")));
        }

    }

    private void printChangeReport(Collection<Change> changes) {
        System.out.println("===============================");
        System.out.println("These changes would be applied:");
        changes.stream().forEach(change -> {
            System.out.println("");
            System.out.println("File: " + change.getOriginal().getSourcePath());
            System.out.println("----------------------------");
            System.out.println("These Visitors made changes:");
            System.out.println("----------------------------");
            System.out.println(change.getVisitorsThatMadeChanges().stream().collect(Collectors.joining(", ")));
            System.out.println("-------------------------");
            System.out.println("The diff looks like this:");
            System.out.println("-------------------------");
            System.out.println("");
            System.out.println(change.diff());
            System.out.println("-----------------");
            System.out.println("Original source:");
            System.out.println("-----------------");
            System.out.println("");
            System.out.println(change.getOriginal().print());
            System.out.println("-----------------");
            System.out.println("Resulting source:");
            System.out.println("-----------------");
            System.out.println("");
            System.out.println(change.getFixed().print());
        });
    }

    @NotNull
    private Collection<Change> applyRecipe(Environment env, List<SourceFile> sourceFiles, String... recipe) {
        final Collection<RefactorVisitor<?>> orderImportsVisitors = env.visitors(recipe);
        Collection<Change> changes = new Refactor()
                .visit(orderImportsVisitors)
                .fix(sourceFiles);
//        final ChangesContainer changesContainer = new ChangesContainer(changes);
        return changes;
    }

    @NotNull
    private List<SourceFile> parseSources() {
        List<SourceFile> sourceFiles = new ArrayList<>();
        List<Path> javaSources = new ArrayList<>();
        javaSources.addAll(listJavaSources("src/main/java"));


        sourceFiles.addAll(JavaParser.fromJavaVersion()
                .build()
                .parse(javaSources, Paths.get(".")));
        return sourceFiles;
    }

    @NotNull
    private Environment createEnvironment() throws FileNotFoundException {
        final File file = new File("./rewrite.yaml");
        InputStream yamlInput = new FileInputStream(file);
        URI source = URI.create("whatever.this.is.for");
        Properties properties = new Properties();
        ResourceLoader resourceLoader = new YamlResourceLoader(yamlInput, source, properties);

        Environment env = Environment.builder()
                .load(resourceLoader)
//                .loadVisitors(visitors)
                .build();

        final Map<String, Recipe> recipesByName = env.getRecipesByName();
        System.out.println("These Recipes were found: " + recipesByName.keySet().stream().collect(Collectors.joining(", ")));
        return env;
    }

    public static class ChangesContainer {
        final List<Change> generated = new ArrayList<>();
        final List<Change> deleted = new ArrayList<>();
        final List<Change> moved = new ArrayList<>();
        final List<Change> refactoredInPlace = new ArrayList<>();

        public ChangesContainer(Collection<Change> changes) {
            for (Change change : changes) {
                if (change.getOriginal() == null && change.getFixed() == null) {
                    // This situation shouldn't happen / makes no sense, log and skip
                    continue;
                }
                if (change.getOriginal() == null && change.getFixed() != null) {
                    generated.add(change);
                } else if (change.getOriginal() != null && change.getFixed() == null) {
                    deleted.add(change);
                } else if (change.getOriginal() != null && !change.getOriginal().getSourcePath().equals(change.getFixed().getSourcePath())) {
                    moved.add(change);
                } else {
                    refactoredInPlace.add(change);
                }
            }
        }
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
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

}
