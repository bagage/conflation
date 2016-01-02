package com.vividsolutions.jcs.conflate.polygonmatch;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.task.TaskMonitor;

class DisambiguationMatch implements Comparable<DisambiguationMatch> {
    private Feature target;
    private Feature candidate;
    private double score;
    public double getScore() {
        return score;
    }

    public Feature getCandidate() {
        return candidate;
    }

    public Feature getTarget() {
        return target;
    }

    public DisambiguationMatch(Feature target, Feature candidate, double score) {
        this.target = target;
        this.candidate = candidate;
        this.score = score;
    }
    @Override
    public int compareTo(DisambiguationMatch other) {
        //Highest scores first. [Jon Aquino]
        if (score > other.score) { return -1; }
        if (score < other.score) { return 1; }
        if (target.compareTo(other.target) != 0) { return target.compareTo(other.target); }
        if (candidate.compareTo(other.candidate) != 0) { return candidate.compareTo(other.candidate); }
        return 0;
    }
    public static SortedSet<DisambiguationMatch> createDisambiguationMatches(Map<Feature, Matches> targetToMatchesMap, TaskMonitor monitor) {
        SortedSet<DisambiguationMatch> set = new TreeSet<>();
        monitor.report("Sorting scores");
        int k = 0;
        for (Feature target : targetToMatchesMap.keySet()) {
            Matches matches = targetToMatchesMap.get(target);
            monitor.report(++k, targetToMatchesMap.keySet().size(), "features");
            for (int j = 0; j < matches.size(); j++) {
                set.add(new DisambiguationMatch(target, matches.getFeature(j), matches.getScore(j)));
            }
        }
        return set;
    }
}