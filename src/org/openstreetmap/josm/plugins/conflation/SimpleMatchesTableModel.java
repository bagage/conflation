// License: GPL. For details, see LICENSE file.
// Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.swing.table.AbstractTableModel;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.TagCollection;
import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * Model for the conflation results table.
 */
class SimpleMatchesTableModel extends AbstractTableModel implements SimpleMatchListListener {

    private SimpleMatchList matches = null;
    // TODO: make columns dynamic
    private static final String[] columnNames = {tr("Reference"), tr("Subject"), "Distance (m)", "Score", "Tags"};

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        if (matches == null)
            return 0;
        return matches.size();
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (matches == null || row < 0 || row >= matches.size())
            return null;
        
        SimpleMatch c = matches.get(row);
        if (col == 0) {
            // TODO: use fancier string
            return c.getReferenceObject();
        } else if (col == 1) {
            // TODO: use fancier string
            return c.getSubjectObject();
        } else if (col == 2) {
            return c.getDistance();
        } else if (col == 3) {
            return c.getScore();
        } else if (col == 4) {
            HashSet<OsmPrimitive> set = new HashSet<>();
            set.add(c.getReferenceObject());
            set.add(c.getSubjectObject());
            TagCollection tags = TagCollection.unionOfAllPrimitives(set);
            Set<String> keys = tags.getKeysWithMultipleValues();
            if (keys.isEmpty()) {
                return "No conflicts!";
            } else {
                return "Conflicts!";
            }

        }
        
        return null;
    }

    @Override
    public Class<?> getColumnClass(int c) {
        Object value = getValueAt(0, c);
        if (value != null) {
            return value.getClass();
        } else {
            return Object.class;
        }
    }

    /**
     * @return the matches
     */
    public SimpleMatchList getMatches() {
        return matches;
    }

    /**
     * @param matches the matches to set
     */
    public void setMatches(SimpleMatchList matches) {
        if (matches != this.matches) {
            if (this.matches != null) {
                this.matches.removeConflationListChangedListener(this);
            }
            this.matches = matches;
            if (matches != null) {
                matches.addConflationListChangedListener(this);
            }
            fireTableDataChanged();
        }
    }

    @Override
    public void simpleMatchListChanged(SimpleMatchList list) {
        fireTableDataChanged();
    }

    @Override
    public void simpleMatchSelectionChanged(Collection<SimpleMatch> selected) {
    }
}
