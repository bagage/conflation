// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation.matcher;

import java.util.HashMap;

import org.openstreetmap.josm.data.validation.tests.SimilarNamedWays.NormalizeRule;
import org.openstreetmap.josm.data.validation.tests.SimilarNamedWays.SynonymRule;

/**
 * Some normalizations that seems sensible to me from an OSM point of view.
 */
public class OsmNormalizeRule implements NormalizeRule {

    static HashMap<String, NormalizeRule[]> rulesMap = new HashMap<>();

    static {
        rulesMap.put("name", new NormalizeRule[]{
                AccentlessNormalizeRule.INSTANCE,
                LowerCaseNormalizeRule.INSTANCE,
                LetterOrDigitNormalizeRules.INSTANCE,
        });
        rulesMap.put("oneway", new NormalizeRule[]{
                LowerCaseNormalizeRule.INSTANCE,
                new SynonymRule("yes", "true", "1"),
                new SynonymRule("-1", "reverse"),
                new SynonymRule("", "no", "false", "0"),
        });
        rulesMap.put("wall", new NormalizeRule[]{
                LowerCaseNormalizeRule.INSTANCE,
                new SynonymRule("", "yes"),
        });
    }

    static NormalizeRule[] defaultRules = new NormalizeRule[]{
            AccentlessNormalizeRule.INSTANCE, LowerCaseNormalizeRule.INSTANCE
    };

    public static NormalizeRule[] get(String attributeName) {
        if (attributeName.contains("name") || attributeName.equals("addr:street") || attributeName.equals("addr:place")) {
            return rulesMap.get("name");
        } else {
            return rulesMap.getOrDefault(attributeName, defaultRules);
        }

    }


    private final NormalizeRule[] rules;

    public OsmNormalizeRule(String attributeName) {
        rules = OsmNormalizeRule.get(attributeName);
    }

    @Override
    public String normalize(String value) {
        for (NormalizeRule rule: rules) {
            value = rule.normalize(value);
        }
        return value;
    }

}
