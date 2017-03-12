// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation.matcher;


import org.openstreetmap.josm.data.validation.tests.SimilarNamedWays.NormalizeRule;

import com.vividsolutions.jcs.conflate.polygonmatch.FeatureMatcher;
import com.vividsolutions.jcs.conflate.polygonmatch.Matches;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;

/**
 * Match a specific attribute.
 */
public class AttributeMatcher implements FeatureMatcher {

    public final String attributeName;
    public final NormalizeRule[] rules;
    public final ValueMatcher valueMatcher;

    /**
     * Match a specific attribute.
     * @param name the attribute name to match
     * @param valueMatcher the way to score the match between two values
     * @param rules the normalization rules to apply before comparing two values.
     */
    public AttributeMatcher(String name, ValueMatcher valueMatcher, NormalizeRule...rules) {
        this.attributeName = name;
        this.valueMatcher = valueMatcher;
        this.rules = rules;
    }

    @Override
    public Matches match(Feature target, FeatureCollection candidates) {
        String targetValue = getStringAttribute(target, attributeName);
        Matches matches = new Matches(candidates.getFeatureSchema());
        for (Feature candidate : candidates) {
            String candidateValue = getStringAttribute(candidate, attributeName);
            double score = valueMatcher.match(targetValue, candidateValue);
            if (score > 0) {
                matches.add(candidate, score);
            }
        }
        return matches;
    }

    private String getStringAttribute(Feature feature, String name) {
        String value;
        try {
            value = feature.getString(name);
        } catch (IllegalArgumentException e) {
            value = "";
        }
        for (NormalizeRule r : rules) {
            value = r.normalize(value);
        }
        return value;
    }

}
