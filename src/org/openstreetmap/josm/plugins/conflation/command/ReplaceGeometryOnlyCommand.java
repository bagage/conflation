// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddPrimitivesCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.PseudoCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.conflation.ConflationUtils;
import org.openstreetmap.josm.plugins.utilsplugin2.replacegeometry.ReplaceGeometryException;
import org.openstreetmap.josm.plugins.utilsplugin2.replacegeometry.ReplaceGeometryUtils;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Tweaked version of the "Replace Geometry" command provided by utilsplugin2 that don't merge tags.
 *
 * The command provided by the utilsplugin2 {@ReplaceGeometryCommand.buildReplaceCommand}
 * also merge tags. This modified version don't do tag merging, it leave the subjectObject tags
 * untouched, it only modify its geometry.
 *
 * This version also support a referenceObject belonging to a different DataSet than the subject one,
 * in which case the reference will be first copied to the subject DataSet, and the original version
 * preserved in the reference DataSet.
 *
 * Last point, the command is not computed at construction, but only at execution.
 * This has the advantage that it is possible to combine many commands in a single SequenceCommand
 * even if a ReplaceGeometry execution would impact the computation for a following one.
 * (e.g.g: when ReplaceGeometry modify relations of modified objects)
 *
 * A drawback is that ReplaceGeometry computation could fail, so we need to handle this failure at execution:
 *  - user cancel tag merging
 *  - ReplaceGeometryException
 *
 * @see ReplaceGeometryUtils#buildReplaceCommand
 */
public class ReplaceGeometryOnlyCommand extends Command {

    private final OsmPrimitive subjectObject;
    private final OsmPrimitive referenceObject;
    private TagMap referenceKeys = null;
    private Command replaceGeometryCommand = null;
    private boolean replaceGeometryCommandBuilded;
    private Command addPrimitivesCommand = null;

    public ReplaceGeometryOnlyCommand(OsmPrimitive subjectObject, OsmPrimitive referenceObject, OsmDataLayer subjectLayer) {
        super(subjectLayer);
        this.subjectObject = subjectObject;
        this.referenceObject = referenceObject;
    }

    @Override
    public boolean executeCommand() {
        DataSet subjectDataSet = subjectObject.getDataSet();
        DataSet referenceDataSet = referenceObject.getDataSet();
        referenceKeys = null;
        boolean ok = true;
        try {
            if (subjectDataSet != referenceDataSet) {
                if (addPrimitivesCommand == null) {
                    List<PrimitiveData> newObjects = ConflationUtils.copyObjects(referenceObject.getDataSet(), referenceObject);
                    newObjects.forEach(o -> {
                        if (o.getPrimitiveId().equals(referenceObject.getPrimitiveId())) {
                            // remove tags to avoid tag merging action by {@link ReplaceGeometryUtils}
                            o.removeAll();
                        }
                    });
                    addPrimitivesCommand = new AddPrimitivesCommand(newObjects, newObjects, getLayer());
                }
                if (!addPrimitivesCommand.executeCommand()) {
                    ok = false;
                }
            } else {
                // remove tags to avoid tag merging action by {@link ReplaceGeometryUtils}
                referenceKeys = referenceObject.getKeys();
                referenceObject.removeAll();

            }
            if (ok) {
                // be sure that we don't try again in case of undo/redo
                if (!replaceGeometryCommandBuilded) {
                    replaceGeometryCommandBuilded = true; 
                    replaceGeometryCommand = ReplaceGeometryUtils.buildReplaceCommand(
                                        subjectObject,
                                        subjectDataSet.getPrimitiveById(referenceObject.getPrimitiveId()));
                }
                ok = (replaceGeometryCommand != null) ? replaceGeometryCommand.executeCommand() : false;
            }
        } catch (ReplaceGeometryException ex) {
            ok = false;
            JOptionPane.showMessageDialog(Main.parent,
                    ex.getMessage(), tr("Cannot replace geometry."), JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException e) {
            ok = false;
            throw e;
        } finally {
            if (!ok) {
                if (referenceKeys != null) {
                    referenceObject.setKeys(referenceKeys);
                }
                replaceGeometryCommand = null;
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
        if (replaceGeometryCommand != null) {
            replaceGeometryCommand.undoCommand();
        }
        if (referenceKeys != null) {
            referenceObject.setKeys(referenceKeys);
        }
        if (addPrimitivesCommand != null) {
            addPrimitivesCommand.undoCommand();
        }
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        if (replaceGeometryCommand != null) {
            replaceGeometryCommand.fillModifiedData(modified, deleted, added);
        }
        if (addPrimitivesCommand != null) {
            addPrimitivesCommand.fillModifiedData(modified, deleted, added);
        }
    }

    @Override
    public String getDescriptionText() {
        return tr("Replace Geometry Only");
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
        if (replaceGeometryCommand != null)
            prims.addAll(replaceGeometryCommand.getParticipatingPrimitives());
        return prims;
    }

    @Override
    public Collection<PseudoCommand> getChildren() {
        return Arrays.asList(addPrimitivesCommand, replaceGeometryCommand).stream()
            .filter(c -> c != null)
            .collect(Collectors.toList());
    }
}
