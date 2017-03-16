// License: GPL. For details, see LICENSE file.
// Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation;

import com.vividsolutions.jcs.conflate.polygonmatch.FCMatchFinder;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;


import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Result of the configuration {@Link SettingsDialog}.
 * @author joshdoe
 */
public class SimpleMatchSettings {

    public List<OsmPrimitive> subjectSelection;
    public List<OsmPrimitive> referenceSelection;
    public OsmDataLayer referenceLayer;
    public DataSet subjectDataSet;
    public OsmDataLayer subjectLayer;
    public DataSet referenceDataSet;
    public FCMatchFinder matchFinder;

    /*=
     * If conflation should replace the geometry.
     */
    public boolean isReplacingGeometry;

    /**
     * List of tags to merge during conflation.
     * Should be set to the {@link ALL} constant to mean all tags.
     */
    public Collection<String> mergeTags;

    /**
     * List of tags to overwrite without confirmation during conflation.
     */
    public Collection<String> overwriteTags;


    /**
     * A Collection that always answer true when asked if it contains any object.
     */
    public static final Collection<String> ALL = new Collection<String>() {
        @Override public int size() {
            return Integer.MAX_VALUE; }

        @Override public boolean isEmpty() {
            return false; }

        @Override public boolean contains(Object o) {
            return true; }

        @Override public boolean containsAll(Collection<?> c) {
            return true; }

        @Override public Iterator<String> iterator() {
            throw new UnsupportedOperationException(); }

        @Override public Object[] toArray() {
            throw new UnsupportedOperationException(); }

        @Override public <T> T[] toArray(T[] a) {
            throw new UnsupportedOperationException(); }

        @Override public boolean add(String e) {
            throw new UnsupportedOperationException(); }

        @Override public boolean remove(Object o) {
            throw new UnsupportedOperationException(); }

        @Override public boolean addAll(Collection<? extends String> c) {
            throw new UnsupportedOperationException(); }

        @Override public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException(); }

        @Override public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException(); }

        @Override public void clear() {
            throw new UnsupportedOperationException(); }
    };
}

