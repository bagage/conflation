package com.vividsolutions.jcs.conflate.polygonmatch;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.task.TaskMonitor;
/**
 * Enforces a one-to-one relationship between target features and
 * matched candidate features, in the returned result set.
 * "Aggressive" because 2nd, 3rd, 4th, etc. best
 * matches are tried if the 1st, 2nd, 3rd, etc. match is "taken" by another
 * feature.
 */
public class DisambiguatingFCMatchFinder implements FCMatchFinder {
    private FCMatchFinder matchFinder;
    public DisambiguatingFCMatchFinder(FCMatchFinder matchFinder) {
        this.matchFinder = matchFinder;
    }
    @Override
    public Map<Feature, Matches> match(
        FeatureCollection targetFC,
        FeatureCollection candidateFC,
        TaskMonitor monitor) {
        List<Feature> targets = new ArrayList<>();
        List<Feature> candidates = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        SortedSet<DisambiguationMatch> matchSet = DisambiguationMatch.createDisambiguationMatches(matchFinder.match(targetFC, candidateFC, monitor), monitor);
        monitor.report("Discarding inferior matches");
        int j = 0;
        for (DisambiguationMatch match : matchSet) {
            monitor.report(++j, matchSet.size(), "matches");
            if (targets.contains(match.getTarget()) || candidates.contains(match.getCandidate())) {
                continue;
            }
            targets.add(match.getTarget());
            candidates.add(match.getCandidate());
            scores.add(new Double(match.getScore()));
        }
        //Re-add filtered-out targets, but with zero-score matches [Jon Aquino]
        Map<Feature, Matches> targetToMatchesMap =
            AreaFilterFCMatchFinder.blankTargetToMatchesMap(
                targetFC.getFeatures(),
                candidateFC.getFeatureSchema());
        for (int i = 0; i < targets.size(); i++) {
            Matches matches = new Matches(candidateFC.getFeatureSchema());
            matches.add(candidates.get(i), scores.get(i).doubleValue());
            targetToMatchesMap.put(targets.get(i), matches);
        }
        return targetToMatchesMap;
    }
}