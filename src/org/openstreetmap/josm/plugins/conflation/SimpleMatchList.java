// License: GPL. For details, see LICENSE file.
// Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 *  Holds a list of {@see Match}es and provides convenience functions.
 */
public class SimpleMatchList implements Iterable<SimpleMatch> {
    private final CopyOnWriteArrayList<SimpleMatchListListener> listeners = new CopyOnWriteArrayList<>();

    private final ArrayList<SimpleMatch> matches = new ArrayList<>();
    private final HashSet<SimpleMatch> selected = new HashSet<>();

    private final HashMap<OsmPrimitive, SimpleMatch> byReference = new HashMap<>();
    private final HashMap<OsmPrimitive, SimpleMatch> bySubject = new HashMap<>();

    private int updateCount = 0;
    private boolean updateHasChanged = false;

    public SimpleMatchList() {
    }

    public boolean hasMatch(SimpleMatch c) {
        return hasMatchForReference(c.getReferenceObject());
    }

    public boolean hasMatch(OsmPrimitive referenceObject, OsmPrimitive subjectObject) {
        return hasMatchForReference(referenceObject) || hasMatchForSubject(subjectObject);
    }

    public boolean hasMatchForReference(OsmPrimitive referenceObject) {
        return getMatchByReference(referenceObject) != null;
    }

    public boolean hasMatchForSubject(OsmPrimitive subjectObject) {
        return getMatchBySubject(subjectObject) != null;
    }

    public SimpleMatch getMatchByReference(OsmPrimitive referenceObject) {
        return byReference.get(referenceObject);
    }

    public SimpleMatch getMatchBySubject(OsmPrimitive subjectObject) {
        return bySubject.get(subjectObject);
    }

    @Override
    public Iterator<SimpleMatch> iterator() {
        return matches.iterator();
    }

    public boolean add(SimpleMatch element) {
        int index = Collections.binarySearch(matches, element);
        if (index < 0) {
            index = -index - 1;
            matches.add(index, element);
            byReference.put(element.getReferenceObject(), element);
            bySubject.put(element.getSubjectObject(), element);
            fireIntervalAdded(index, index);
            return true;
        } else {
            return false;
        }
    }

    public boolean addAll(Collection<SimpleMatch> toAdd) {
        if ((matches.size() == 0) && (toAdd.size() > 0)) {
            matches.addAll(toAdd);
            Collections.sort(matches);
            for (SimpleMatch sm : toAdd) {
                byReference.put(sm.getReferenceObject(), sm);
                bySubject.put(sm.getSubjectObject(), sm);
            }
            fireListChanged();
            return true;
        } else {
            boolean changed = false;
            for (SimpleMatch sm : toAdd) {
                changed = add(sm) || changed;
            }
            return changed;
        }
    }

    public int size() {
        return matches.size();
    }

    public SimpleMatch get(int index) {
        return matches.get(index);
    }

    public int indexOf(SimpleMatch match) {
        int index = Collections.binarySearch(matches, match);
        if (index < -1)
            index = -1;
        return index;
    }

    /**
     * Remove all matches and clear selection.
     */
    public void clear() {
        if (matches.size() > 0) {
            setSelected(new ArrayList<SimpleMatch>());
            matches.clear();
            byReference.clear();
            bySubject.clear();
            fireListChanged();
        }
    }

    public boolean remove(SimpleMatch c) {
        return removeAll(Collections.singleton(c));
    }

    public SimpleMatch findNextSelection() {

        // FIXME: The SimpleMatchesTableModel can be sorted,
        // so the next selection computed here may
        // not be the next one the displayed list as expected.

        if (selected.size() == matches.size()) {
            // special case (all elements selected because of Cntrl-A)
            // that would take a lot of time to compute otherwise
            return null;
        }
        int min = selected.stream().map(s -> matches.indexOf(s)).min(Integer::compare).orElse(-1);
        int index = min + 1;
        while ((index < matches.size()) && selected.contains(matches.get(index))) index++;
        if (index < matches.size()) {
            return matches.get(index);
        } else if (min > 0) {
            return matches.get(min - 1);
        } else {
            return null;
        }

    }

    public boolean removeAll(Collection<SimpleMatch> matchesToRemove) {
        // find next to select if entire selection is removed
        SimpleMatch next = findNextSelection();

        boolean isChanged = false;
        Set<Integer> removedIdx = new HashSet<>();
        int maxSize = matches.size();
        for (SimpleMatch sm : matchesToRemove) {
            int index = Collections.binarySearch(matches, sm);
            if (index >= 0) {
                byReference.remove(sm.getReferenceObject());
                bySubject.remove(sm.getSubjectObject());
                removedIdx.add(index);
                isChanged = true;
            }
        }
        matches.removeAll(matchesToRemove);
        // regroup removed items by interval to avoid firing too many
        // "IntervalRemoved" events which is used for GUI updates which
        // are time-consuming
        for (int i = 0; i < maxSize; i++) {
            if (!removedIdx.contains(i))
                continue;
            int startRange = i;
            while (i < maxSize && removedIdx.contains(i)) {
                i++;
            }
            fireIntervalRemoved(startRange, i-1);
        }


        if (selected.removeAll(matchesToRemove)) {

            if (selected.isEmpty() && next != null)
                selected.add(next);

            fireSelectionChanged();
        }

        return isChanged;
    }

    public void addConflationListChangedListener(SimpleMatchListListener listener) {
        listeners.addIfAbsent(listener);
    }

    public void removeConflationListChangedListener(SimpleMatchListListener listener) {
        listeners.remove(listener);
    }

    public void removeAllConflationListChangedListener() {
        listeners.clear();
    }

    public void fireListChanged() {
        if (!shouldFireEvent()) return;
        for (SimpleMatchListListener l : listeners) {
            l.simpleMatchListChanged(this);
        }
    }

    public void fireSelectionChanged() {
        if (!shouldFireEvent()) return;
        for (SimpleMatchListListener l : listeners) {
            l.simpleMatchSelectionChanged(selected);
        }
    }

    public void fireIntervalAdded(int index0, int index1) {
        if (!shouldFireEvent()) return;
        for (SimpleMatchListListener l : listeners) {
            l.simpleMatchListIntervalAdded(this, index0, index1);
        }
    }

    public void fireIntervalRemoved(int index0, int index1) {
        if (!shouldFireEvent()) return;
        for (SimpleMatchListListener l : listeners) {
            l.simpleMatchListIntervalRemoved(this, index0, index1);
        }
    }

    public Collection<SimpleMatch> getSelected() {
        return selected;
    }

    /**
     * Set which {@see SimpleMatch} is currently selected. Set to null to clear selection.
     */
    public void setSelected(SimpleMatch match) {
        if (match != null)
            setSelected(Collections.singleton(match));
        else
            setSelected(new ArrayList<SimpleMatch>());
    }

    public void setSelected(Collection<SimpleMatch> matches) {
        if (selected.containsAll(matches) && selected.size() == matches.size())
            return;

        selected.clear();
        selected.addAll(matches);
        fireSelectionChanged();
    }

    public boolean shouldFireEvent() {
        if (updateCount > 0) {
            updateHasChanged = true;
            return false;
        }
        return true;
    }
    public void beginUpdate() {
        updateCount++;
    }

    /**
     * @see DataSet#beginUpdate()
     */
    public void endUpdate() {
        if (updateCount > 0) {
            updateCount--;
            if (updateCount == 0 && updateHasChanged) {
                updateHasChanged = false;
                fireListChanged();
            }
        } else {
            throw new AssertionError("endUpdate called without beginUpdate");
        }
    }
}
