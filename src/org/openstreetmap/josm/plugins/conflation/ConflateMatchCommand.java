// License: GPL. For details, see LICENSE file.
// Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.Icon;

import org.openstreetmap.josm.command.AddPrimitivesCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.PseudoCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.plugins.utilsplugin2.replacegeometry.ReplaceGeometryUtils;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.UserCancelException;


/**
 * Command to conflate one object with another.
 */
public class ConflateMatchCommand extends Command {

    private final DataSet sourceDataSet;
    private final DataSet targetDataSet;
    private final SimpleMatch match;
    private final SimpleMatchList matches;
    private Command replaceCommand = null;
    private AddPrimitivesCommand addPrimitivesCommand = null;

    public ConflateMatchCommand(SimpleMatch match,
            SimpleMatchList matches, SimpleMatchSettings settings) throws UserCancelException {
        super(settings.getSubjectLayer());
        this.match = match;
        this.matches = matches;
        this.sourceDataSet = settings.getReferenceDataSet();
        this.targetDataSet = settings.getSubjectDataSet();
        // REM: in the previous version of this class, the sub commands 
        // (AddPrimitiveCommand and ReplaceGeometryUtils.buildReplaceCommand)
        // where builded in this constructor, now they are only builded in executeCommand(),
        // so some exceptions that where thrown at construction will now only be be thrown at execution. 
    }

    @Override
    public boolean executeCommand() {
        boolean ok = true;
        try {
            if (targetDataSet != sourceDataSet) {
                if (addPrimitivesCommand == null) {
                    List<PrimitiveData> newObjects = ConflationUtils.copyObjects(sourceDataSet, match.getReferenceObject());
                    addPrimitivesCommand = new AddPrimitivesCommand(newObjects, newObjects, getLayer());
                }
                if (!addPrimitivesCommand.executeCommand()) {
                    ok = false;
                    addPrimitivesCommand = null;
                }
            }
            if (ok) {
                if (replaceCommand == null) {
                    replaceCommand = ReplaceGeometryUtils.buildReplaceCommand(
                                        match.getSubjectObject(),
                                        targetDataSet.getPrimitiveById(match.getReferenceObject().getPrimitiveId()));
                    if (replaceCommand == null) {
                        throw new UserCancelRuntimeException();
                    }
                }
                if (replaceCommand.executeCommand()) {
                    matches.remove(match);
                } else {
                    ok = false;
                }
            }
        } catch (RuntimeException e) {
            ok = false;
            throw e;
        } finally {
            if (!ok) {
                replaceCommand = null;
                if (addPrimitivesCommand != null) {
                    addPrimitivesCommand.undoCommand();
                    addPrimitivesCommand = null;
                }
            }
        }
        return ok;
    }

    @Override
    public void undoCommand() {
        if (replaceCommand != null) {
            replaceCommand.undoCommand();
            matches.add(match);
        }
        if (addPrimitivesCommand != null) {
            addPrimitivesCommand.undoCommand();
        }
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        throw new UnsupportedOperationException("Not supported yet.");
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
        if (addPrimitivesCommand != null)
            prims.addAll(addPrimitivesCommand.getParticipatingPrimitives());
        if (replaceCommand != null)
            prims.addAll(replaceCommand.getParticipatingPrimitives());
        return prims;
    }

    @Override
    public Collection<PseudoCommand> getChildren() {
        return Arrays.asList(addPrimitivesCommand, replaceCommand).stream()
            .filter(c -> c != null)
            .collect(Collectors.toList());
    }
}
