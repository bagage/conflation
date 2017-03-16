// License: GPL. For details, see LICENSE file.
// Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;

import javax.swing.Icon;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.PseudoCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.conflict.tags.CombinePrimitiveResolverDialog;
import org.openstreetmap.josm.plugins.conflation.SimpleMatch;
import org.openstreetmap.josm.plugins.conflation.SimpleMatchList;
import org.openstreetmap.josm.plugins.conflation.SimpleMatchSettings;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.UserCancelException;


/**
 * Command to conflate one object with another.
 */
public class ConflateMatchCommand extends Command {

    private final SimpleMatch match;
    private final SimpleMatchList matcheList;
    private final SimpleMatchSettings settings;
    private final Command replaceGeometryOnlyCommand;
    private Command tagsMergingCommand = null;
    private boolean tagsMergingBuilded;
    private boolean executionOk = false;

    public ConflateMatchCommand(SimpleMatch match,
            SimpleMatchList matcheList, SimpleMatchSettings settings) {
        super(settings.subjectLayer);
        this.match = match;
        this.matcheList = matcheList;
        this.settings = settings;
        replaceGeometryOnlyCommand = settings.isReplacingGeometry ?
                new ReplaceGeometryOnlyCommand(match.getSubjectObject(), match.getReferenceObject(), settings.subjectLayer)
                : null;
    }

    @Override
    public boolean executeCommand() {
        boolean tagsMerged = false;
        try {
            // Be sure we don't ask the user twice in case of undo/redo:
            if (!tagsMergingBuilded) {
                tagsMergingBuilded = true;
                try { 
                    tagsMergingCommand = new SequenceCommand(tr("Merge Tags"),
                            CombinePrimitiveResolverDialog.launchIfNecessary(
                            match.getMergingTagCollection(settings),
                            Arrays.asList(match.getReferenceObject(), match.getSubjectObject()),
                            Collections.singleton(match.getSubjectObject())));
                } catch (UserCancelException e) {
                    tagsMergingBuilded = true;
                }
            }
            if ((tagsMergingCommand != null) && tagsMergingCommand.executeCommand()) {
                tagsMerged = true;
                if ((replaceGeometryOnlyCommand == null) || (replaceGeometryOnlyCommand.executeCommand())) {
                    matcheList.remove(match);
                    executionOk = true;
                }
            }
        } finally {
            if (tagsMerged && !executionOk) {
                tagsMergingCommand.undoCommand();
            }
        }
        return executionOk;
    }

    @Override
    public void undoCommand() {
        if (executionOk) {
            matcheList.add(match);
            executionOk = false;
            if (replaceGeometryOnlyCommand != null) {
                replaceGeometryOnlyCommand.undoCommand();
            }
            if (tagsMergingCommand != null) {
                tagsMergingCommand.undoCommand();
            }
        }
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        if (replaceGeometryOnlyCommand != null) {
            replaceGeometryOnlyCommand.fillModifiedData(modified, deleted, added);
        }
        if (tagsMergingCommand != null) {
            tagsMergingCommand.fillModifiedData(modified, deleted, added);
        }
    }

    @Override
    public String getDescriptionText() {
        //TODO: make more descriptive
        return tr("Conflate object pair");
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get("dialogs", "conflation");
    }

    @Override
    public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
        Collection<OsmPrimitive> prims = new HashSet<>();
        if (tagsMergingCommand != null)
            prims.addAll(tagsMergingCommand.getParticipatingPrimitives());
        if (replaceGeometryOnlyCommand != null)
            prims.addAll(replaceGeometryOnlyCommand.getParticipatingPrimitives());
        return prims;
    }

    @Override
    public Collection<PseudoCommand> getChildren() {
        return Arrays.asList(tagsMergingCommand, replaceGeometryOnlyCommand).stream()
            .filter(c -> c != null)
            .collect(Collectors.toList());
    }
}
