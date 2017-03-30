// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation.command;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Collection;

import javax.swing.Icon;

import org.openstreetmap.josm.plugins.conflation.SimpleMatch;
import org.openstreetmap.josm.plugins.conflation.SimpleMatchList;
import org.openstreetmap.josm.plugins.conflation.UnmatchedObjectListModel;
import org.openstreetmap.josm.tools.ImageProvider;

public class MoveMatchToUnmatchedCommand extends RemoveMatchCommand {

    protected final UnmatchedObjectListModel referenceOnlyListModel;
    protected final UnmatchedObjectListModel subjectOnlyListModel;

    public MoveMatchToUnmatchedCommand(SimpleMatchList matcheList, Collection<SimpleMatch> toRemove,
            UnmatchedObjectListModel referenceOnlyListModel, UnmatchedObjectListModel subjectOnlyListModel) {
        super(matcheList, toRemove);
        this.referenceOnlyListModel = referenceOnlyListModel;
        this.subjectOnlyListModel = subjectOnlyListModel;
    }

    @Override
    public boolean executeCommand() {
        super.executeCommand();
        referenceOnlyListModel.beginUpdate();
        subjectOnlyListModel.beginUpdate();
        for (SimpleMatch match: toRemove) {
            referenceOnlyListModel.addElement(match.getReferenceObject());
            subjectOnlyListModel.addElement(match.getSubjectObject());
        }
        referenceOnlyListModel.endUpdate();
        subjectOnlyListModel.endUpdate();
        return true;
    }

    @Override
    public void undoCommand() {
        referenceOnlyListModel.beginUpdate();
        subjectOnlyListModel.beginUpdate();
        for (SimpleMatch match: toRemove) {
            referenceOnlyListModel.removeElement(match.getReferenceObject());
            subjectOnlyListModel.removeElement(match.getSubjectObject());
        }
        referenceOnlyListModel.endUpdate();
        subjectOnlyListModel.endUpdate();
        super.undoCommand();
    }

    @Override
    public String getDescriptionText() {
        return trn("Moved {0} conflation match to unmatched list",
                "Moved {0} conflation matches to unmatched list", toRemove.size(), toRemove.size());
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get("dialogs", "delete");
    }
}
