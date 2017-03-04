// License: GPL. For details, see LICENSE file.
// Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * This class represents a potential match, i.e. a pair of primitives, a score
 * and related information.
 */
public class SimpleMatch implements Comparable<SimpleMatch> {

    final OsmPrimitive referenceObject;
    final OsmPrimitive subjectObject;
    final double score;
    final double distance;

    public SimpleMatch(OsmPrimitive referenceObject,
            OsmPrimitive subjectObject, double score) {
        CheckParameterUtil.ensureParameterNotNull(referenceObject, "referenceObject");
        CheckParameterUtil.ensureParameterNotNull(subjectObject, "subjectObject");
        this.referenceObject = referenceObject;
        this.subjectObject = subjectObject;
        this.score = score;
        // TODO: use distance calculated in score function, and make sure it's in meters?
        this.distance = ConflationUtils.getCenter(referenceObject).distance(ConflationUtils.getCenter(subjectObject));
    }

    public OsmPrimitive getReferenceObject() {
        return referenceObject;
    }

    public OsmPrimitive getSubjectObject() {
        return subjectObject;
    }

    public Object getScore() {
        return score;
    }

    public Object getDistance() {
        return distance;
    }

    @Override
    public int compareTo(SimpleMatch o) {
        int comp = Double.compare(this.score, o.score);
        if (comp == 0) {
            comp = -Double.compare(this.distance, o.distance); // (-) greater distance is no good
            if (comp == 0) {
                comp = referenceObject.compareTo(o.referenceObject);
                if (comp == 0) {
                    comp = subjectObject.compareTo(o.subjectObject);
                }
            }
        }
        return comp;
    }

    @Override
    public int hashCode() {
        return referenceObject.hashCode() ^ subjectObject.hashCode(); 
    }
}
