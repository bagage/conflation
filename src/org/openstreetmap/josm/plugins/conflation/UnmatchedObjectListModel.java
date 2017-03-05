// License: GPL. For details, see LICENSE file.
// Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.swing.AbstractListModel;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * List model for unmatched objects, for both subject and reference layers.
 *
 * The list is kept sorted, for better lookup time, and to ensure that Undo/Redo
 * will restore items at the same position.
 *
 * @author joshdoe
 */
public class UnmatchedObjectListModel extends AbstractListModel<OsmPrimitive> {

    private ArrayList<OsmPrimitive> list = new ArrayList<>();

    public void clear() {
        int size = list.size();
        if (size > 0) {
            list.clear();
            fireIntervalRemoved(this, 0, size - 1);
        }
    }

    @Override
    public int getSize() {
        return list.size();
    }

    @Override
    public OsmPrimitive getElementAt(int index) {
        return list.get(index);
    }

    public int indexOf(OsmPrimitive item) {
        int index = Collections.binarySearch(list, item);
        if (index < -1)
            index = -1;
        return index;
    }

    public boolean addElement(OsmPrimitive element) {
        int index = Collections.binarySearch(list, element);
        if (index < 0) {
            index = -index - 1;
            list.add(index, element);
            fireIntervalAdded(this, index, index);
            return true;
        } else {
            return false;
        }
    }

    public boolean addAll(Collection<OsmPrimitive> objects) {
        if ((list.size() == 0) && (objects.size() > 0)) {
            list.addAll(objects);
            Collections.sort(list);
            fireIntervalAdded(this, 0, list.size() - 1);
            return true;
        } else {
            boolean changed = false;
            for (OsmPrimitive p : objects) {
                changed = addElement(p) || changed;
            }
            return changed;
        }
    }

    public boolean removeElement(OsmPrimitive element) {
        int index = Collections.binarySearch(list, element);
        if (index >= 0) {
            list.remove(index);
            fireIntervalRemoved(this, index, index);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeAll(Collection<OsmPrimitive> objects) {
        boolean changed = false;
        for (OsmPrimitive p : objects) {
            changed = removeElement(p) || changed;
        }
        return changed;
    }
}
