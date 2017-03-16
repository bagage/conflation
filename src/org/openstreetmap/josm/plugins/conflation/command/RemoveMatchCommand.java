// License: GPL. For details, see LICENSE file.
// Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation.command;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.ArrayList;
import java.util.Collection;

import javax.swing.Icon;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.plugins.conflation.SimpleMatch;
import org.openstreetmap.josm.plugins.conflation.SimpleMatchList;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Command to remove selected matches from the list.
 *
 * Its optionally possible to also move the subject and reference
 * object to their respective unmatched list.
 */
public class RemoveMatchCommand extends Command {

    protected final ArrayList<SimpleMatch> toRemove;
    protected final SimpleMatchList matcheList;

    public RemoveMatchCommand(SimpleMatchList matcheList, Collection<SimpleMatch> toRemove) {
        this.toRemove = new ArrayList<>(toRemove);
        this.matcheList = matcheList;
    }

    @Override
    public boolean executeCommand() {
        matcheList.removeAll(toRemove);
        return true;
    }

    @Override
    public void undoCommand() {
        matcheList.addAll(toRemove);
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
    }

    @Override
    public String getDescriptionText() {
        return trn("Delete {0} conflation matche", "Delete {0} conflation matches", toRemove.size(), toRemove.size());
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get("dialogs", "delete");
    }
}
