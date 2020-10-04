package de.fabiankrueger.openrewrite.playground.openrewrite;

import org.openrewrite.Change;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class ChangesContainer {
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