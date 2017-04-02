// License: GPL. For details, see LICENSE file.
// Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation.command;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

import javax.swing.Icon;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.PseudoCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A command consisting of a sequence of other commands, that will
 * be executed up to the first one that fail, without rolling back.
 * the execution of the ones that succeeded.
 */
public class StopOnErrorSequenceCommand extends Command {

    protected final String name;
    protected final Command[] sequence;
    protected final HashSet<OsmPrimitive> selection;
    protected final boolean fireSelectionChangedEvent;
    protected int nbToExecute;

    public StopOnErrorSequenceCommand(DataSet data, String name,
            boolean combineSelection, boolean fireSelectionChangedEvent,
            Command... sequenz) {
        this(data, name, combineSelection, fireSelectionChangedEvent, sequenz.clone(), sequenz.length);
    }

    public StopOnErrorSequenceCommand(DataSet data, String name,
            boolean combineSelection, boolean fireSelectionChangedEvent,
            Collection<Command> sequenz) {
        this(data, name, combineSelection, fireSelectionChangedEvent, sequenz.toArray(new Command[sequenz.size()]), sequenz.size());
    }

    private StopOnErrorSequenceCommand(DataSet data, String name,
            boolean combineSelection, boolean fireSelectionChangedEvent,
            Command[] sequence, int nbToExecute) {
        super(data);
        this.name = name;
        this.sequence = sequence;
        this.nbToExecute = nbToExecute;
        this.selection = combineSelection ? new HashSet<>() : null;
        this.fireSelectionChangedEvent = fireSelectionChangedEvent;
    }

    @Override public boolean executeCommand() {
        // REM: in case of redo we should never execute more commands that in the first execution
        if (selection != null)
            selection.clear();
        int count = 0;
        try {
            while ((count < nbToExecute) && sequence[count].executeCommand()) {
                count++;
                if (selection != null)
                    selection.addAll(getAffectedDataSet().getSelected());
            }
        } catch (RuntimeException e) {
            nbToExecute = count;
            undoCommand(); // undo up to nbToExecute
            nbToExecute = 0;
            throw e;
        }
        if (selection != null && !selection.isEmpty())
            getAffectedDataSet().setSelected(selection, fireSelectionChangedEvent);
        nbToExecute = count;
        return count > 0;
    }

    @Override public void undoCommand() {
        for (int i = nbToExecute-1; i >= 0; i--) {
            sequence[i].undoCommand();
        }
    }

    @Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDescriptionText() {
        if (nbToExecute == sequence.length) {
            return tr(marktr("Sequence: {0}"), name);
        } else {
            return tr(marktr("Interrupted Sequence ({0}/{1}): {2}"), nbToExecute, sequence.length, name);
        }
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get("data", "sequence");
    }

    @Override
    public Collection<PseudoCommand> getChildren() {
        return Arrays.<PseudoCommand>asList(Arrays.copyOf(sequence, nbToExecute));
    }

    @Override
    public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
        Collection<OsmPrimitive> prims = new HashSet<>();
        for (int i = 0; i < nbToExecute; i++) {
            prims.addAll(sequence[i].getParticipatingPrimitives());
        }
        return prims;
    }

    @Override
    public void invalidateAffectedLayers() {
        for (int i = 0; i < nbToExecute; i++) {
            sequence[i].invalidateAffectedLayers();
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), Arrays.hashCode(sequence), nbToExecute, selection != null, fireSelectionChangedEvent, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        StopOnErrorSequenceCommand that = (StopOnErrorSequenceCommand) obj;
        return nbToExecute == that.nbToExecute &&
                fireSelectionChangedEvent == that.fireSelectionChangedEvent &&
                (selection != null) == (that.selection != null) &&
                Arrays.equals(sequence, that.sequence) &&
                Objects.equals(name, that.name);
    }
}
