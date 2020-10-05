/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.maven;

import org.openrewrite.Formatting;
import org.openrewrite.Tree;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.MavenModel;
import org.openrewrite.refactor.Formatter;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.openrewrite.Formatting.format;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.Validated.required;

/**
 * Adds a dependency if there is no dependency matching <code>groupId</code> and <code>artifactId</code>.
 * A matching dependency with a different version or scope does NOT have its version or scope updated.
 * Use {@link ChangeDependencyVersion} or {@link UpgradeDependencyVersion} in the case of a different version.
 * Use {@link ChangeDependencyScope} in the case of a different scope.
 * <p>
 * Places a new dependency as physically "near" to a group of similar dependencies as possible.
 */
public class AddDependencyManagement extends MavenRefactorVisitor {
    private String groupId;
    private String artifactId;
    private String description;

    public String getDescription() {
        return description;
    }

    @Nullable
    private String version;

    @Nullable
    private String scope;

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setVersion(@Nullable String version) {
        this.version = version;
    }

    public void setScope(@Nullable String scope) {
        this.scope = scope;
    }

    @Override
    public Validated validate() {
        return required("groupId", groupId)
                .and(required("artifactId", artifactId))
                .and(required("version", version));
    }

    @Override
    public boolean isIdempotent() {
        return false;
    }

    @Override
    public Maven visitPom(Maven.Pom pom) {
        if (dependencyManagementSectionExists(pom) && managedDependencyExists(pom)) {
            return pom;
        }

        andThen(new AddDependenciesTagIfNotPresent());
        andThen(new InsertDependencyInOrder());

        return pom;
    }

    private boolean managedDependencyExists(Maven.Pom pom) {
        final Optional<Xml.Tag> optionalDependencyManagement = pom.getDocument()
                .getRoot()
                .getChild("dependencyManagement");
        if(optionalDependencyManagement.isPresent()){
            boolean managedDependencyExists = optionalDependencyManagement.get()
                    .getChildren().stream()
                    .anyMatch(d ->  d.getChild("groupId").get().getValue().get().equals(groupId) &&
                            d.getChild("artifactId").get().getValue().get().equals(artifactId)
                    );
            return managedDependencyExists;
        }
        return false;

//        boolean managedDependencyExists = pom.getModel().getDependencyManagement().getDependencies().stream()
//                .map(MavenModel.Dependency::getModuleVersion)
//                .anyMatch(mvid -> mvid.getGroupId().equals(groupId) &&
//                        mvid.getArtifactId().equals(artifactId));
//        return managedDependencyExists;
    }

    private boolean dependencyManagementSectionExists(Maven.Pom pom) {
        return pom.getModel().getDependencyManagement() != null;
    }

    private static class AddDependenciesTagIfNotPresent extends MavenRefactorVisitor {
        @Override
        public Maven visitPom(Maven.Pom pom) {
            Maven.Pom p = refactor(pom, super::visitPom);

            Xml.Tag root = p.getDocument().getRoot();
            if (!root.getChild("dependencyManagement").isPresent()) {
                MavenTagInsertionComparator insertionComparator = new MavenTagInsertionComparator(
                        root.getChildren());
                List<Xml.Tag> content = new ArrayList<>(root.getChildren());

                Formatting fmt = format(formatter.findIndent(0, root.getChildren().toArray(new Tree[0])).getPrefix());
                content.add(
                        new Xml.Tag(
                                randomId(),
                                "dependencyManagement",
                                emptyList(),
                                emptyList(),
                                new Xml.Tag.Closing(randomId(), "dependencyManagement", "", fmt),
                                "",
                                fmt
                        )
                );

                content.sort(insertionComparator);

                //noinspection unchecked
                return p.withDocument(p.getDocument().withRoot(
                        root.withContent((List) content)
                ));
            }
            return p;
        }
    }

    private class InsertDependencyInOrder extends MavenRefactorVisitor {
        @Override
        public Maven visitPom(Maven.Pom pom) {
            Maven.Pom p = refactor(pom, super::visitPom);
            List<Maven.Dependency> dependencies = new ArrayList<>(pom.getDependencyManagement().getDependencies());

            Formatter.Result indent = formatter.findIndent(0, pom.getDocument().getRoot()
                    .getChild("dependencyManagement").get());

            // TODO if the dependency is manageable, make it managed
            Xml.Tag dependencyTag = createDependencyTag(indent);

            Maven.Dependency toAdd = new Maven.DependencyManagement.Dependency(false,
                    new MavenModel.Dependency(
                            new MavenModel.ModuleVersionId(groupId, artifactId, null, version, "jar"),
                            version,
                            scope
                    ),
                    dependencyTag
            );

            if (dependencies.isEmpty()) {
                dependencies.add(toAdd);
            } else {
                // if everything were ideally sorted, which dependency would the addable dependency
                // come after?
                List<Maven.Dependency> sortedDependencies = new ArrayList<>(pom.getDependencies());
                sortedDependencies.add(toAdd);
                dependencies.sort(Comparator.comparing(d -> d.getModel().getModuleVersion()));

                int addAfterIndex = -1;
                for (int i = 0; i < sortedDependencies.size(); i++) {
                    Maven.Dependency d = sortedDependencies.get(i);
                    if (toAdd == d) {
                        addAfterIndex = i - 1;
                        break;
                    }
                }
                dependencies.add(addAfterIndex + 1, toAdd);
            }

            p.getDocument()
                    .getRoot()
                    .getChild("dependencyManagement")
                    .ifPresent(dependencyManagementTag -> dependencyManagementTag.getContent().add(dependencyTag));

            return p;
        }

        private Xml.Tag createDependencyTag(Formatter.Result indent) {
            return new XmlParser().parse(
                    "<dependency>" +
                            indent.getPrefix(2) + "<groupId>" + groupId + "</groupId>" +
                            indent.getPrefix(2) + "<artifactId>" + artifactId + "</artifactId>" +
                            (version == null ? "" :
                                    indent.getPrefix(2) + "<version>" + version + "</version>") +
                            (scope == null ? "" :
                                    indent.getPrefix(2) + "<scope>" + scope + "</scope>") +
                            indent.getPrefix(1) + "</dependency>"
            ).get(0).getRoot().withFormatting(format(indent.getPrefix(1)));
        }
    }
}
