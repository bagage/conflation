// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation.config;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.GroupLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.plugins.conflation.matcher.AttributeMatcher;
import org.openstreetmap.josm.plugins.conflation.matcher.LevenshteinDistanceValueMatcher;
import org.openstreetmap.josm.plugins.conflation.matcher.OsmNormalizeRule;

import com.vividsolutions.jcs.conflate.polygonmatch.AbstractDistanceMatcher;
import com.vividsolutions.jcs.conflate.polygonmatch.BasicFCMatchFinder;
import com.vividsolutions.jcs.conflate.polygonmatch.CentroidDistanceMatcher;
import com.vividsolutions.jcs.conflate.polygonmatch.ChainMatcher;
import com.vividsolutions.jcs.conflate.polygonmatch.DisambiguatingFCMatchFinder;
import com.vividsolutions.jcs.conflate.polygonmatch.FCMatchFinder;
import com.vividsolutions.jcs.conflate.polygonmatch.FeatureMatcher;
import com.vividsolutions.jcs.conflate.polygonmatch.HausdorffDistanceMatcher;
import com.vividsolutions.jcs.conflate.polygonmatch.IdenticalFeatureFilter;
import com.vividsolutions.jcs.conflate.polygonmatch.OneToOneFCMatchFinder;
import com.vividsolutions.jcs.conflate.polygonmatch.WeightedMatcher;
import com.vividsolutions.jcs.conflate.polygonmatch.WindowMatcher;
import com.vividsolutions.jcs.plugin.conflate.polygonmatch.MyValidatingTextField;

public class SimpleMatchFinderPanel extends MatchFinderPanel {

    private static final double DEFAULT_DISTANCE_THRESHOLD = 30.0;

    private final JLabel methodLabel = new JLabel(tr("Method"));
    private final String[] methodString = {tr("Disambiguating"), tr("One to One")};
    private final JComboBox<String> methodCombeBox = new JComboBox<>(methodString);
    private final JLabel distanceLabel = new JLabel(tr("Distance"));
    private final String[] distanceStrings = {tr("Centroid"), tr("Hausdorff")};
    private final JComboBox<String> distanceComboBox = new JComboBox<>(distanceStrings);
    private final JLabel threshDistanceLabel = new JLabel(" < ");
    private final MyValidatingTextField threshDistanceField = new MyValidatingTextField(
            "" + DEFAULT_DISTANCE_THRESHOLD, 4, MyValidatingTextField.NON_NEGATIVE_DOUBLE_VALIDATOR, "0");
    private final JLabel tagsLabel = new JLabel(tr("Tags"));
    private final JTextField tagsField = new JTextField(15);

    public SimpleMatchFinderPanel() {
        super();
        distanceComboBox.setSelectedIndex(1);
        threshDistanceField.setToolTipText(tr("Maximum distance"));
        tagsField.setToolTipText("List of tags to match");
        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(methodLabel)
                        .addComponent(distanceLabel)
                        .addComponent(tagsLabel))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 10, 10)
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(methodCombeBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(distanceComboBox,
                                        GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(threshDistanceLabel)
                                .addComponent(threshDistanceField,
                                        GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                        .addComponent(tagsField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 0, Short.MAX_VALUE)
             );
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(methodLabel)
                        .addComponent(methodCombeBox))
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(distanceLabel)
                        .addComponent(distanceComboBox)
                        .addComponent(threshDistanceLabel)
                        .addComponent(threshDistanceField))
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(tagsLabel)
                        .addComponent(tagsField))
             );
        restoreFromPreferences();
    }

    public FCMatchFinder getMatchFinder() {
        ArrayList<FeatureMatcher> matchers = new ArrayList<>();
        if (threshDistanceField.getDouble() > 0) {
            // Use a WindowMatcher limit the search area and speed up execution time.
            matchers.add(new WindowMatcher(threshDistanceField.getDouble()));
        }
        AbstractDistanceMatcher distanceMatcher = (distanceComboBox.getSelectedIndex() == 0) ?
                new CentroidDistanceMatcher() : new HausdorffDistanceMatcher();
        distanceMatcher.setMaxDistance(threshDistanceField.getDouble());
        matchers.add(distanceMatcher);
        List<String> tags = splitBySpaceComaOrSemicolon(tagsField.getText());
        if (tags.size() > 0) {
            ArrayList<Object> weightedArgs = new ArrayList<>();
            weightedArgs.add(1.0); weightedArgs.add(distanceMatcher);
            for (String tag:tags) {
                    weightedArgs.add(1.0);
                    weightedArgs.add(new AttributeMatcher(tag, LevenshteinDistanceValueMatcher.INSTANCE, OsmNormalizeRule.get(tag)));
            }
            WeightedMatcher weightedMatcher = new WeightedMatcher(weightedArgs.toArray());
            matchers.add(weightedMatcher);
        }
        matchers.add(new IdenticalFeatureFilter());
        ChainMatcher chain = new ChainMatcher(matchers.toArray(new FeatureMatcher[matchers.size()]));
        BasicFCMatchFinder basicFinder = new BasicFCMatchFinder(chain);
        FCMatchFinder finder = (methodCombeBox.getSelectedIndex() == 0) ?
                new DisambiguatingFCMatchFinder(basicFinder) : new OneToOneFCMatchFinder(basicFinder);
        return finder;
    }

    public static List<String> splitBySpaceComaOrSemicolon(String values) {
        return Stream.of(values.trim().split("[\\s,;]+")).filter((s) -> !s.isEmpty()).collect(Collectors.toList());
    }

    public void restoreFromPreferences() {
        methodCombeBox.setSelectedIndex(Integer.max(0, Integer.min(methodCombeBox.getItemCount()-1,
                Main.pref.getInteger(getClass().getName() + ".methodIndex", 0))));
        distanceComboBox.setSelectedIndex(Integer.max(0, Integer.min(distanceComboBox.getItemCount()-1,
                Main.pref.getInteger(getClass().getName() + ".distanceIndex", 1))));
        threshDistanceField.setText("" + Double.max(0.0,
                Main.pref.getDouble(getClass().getName() + ".thresholdDistance", DEFAULT_DISTANCE_THRESHOLD)));
        tagsField.setText(Main.pref.get(getClass().getName() + ".tags", ""));
    }

    public void savePreferences() {
        Main.pref.putInteger(getClass().getName() + ".methodIndex", methodCombeBox.getSelectedIndex());
        Main.pref.putInteger(getClass().getName() + ".distanceIndex", distanceComboBox.getSelectedIndex());
        Main.pref.putDouble(getClass().getName() + ".thresholdDistance", threshDistanceField.getDouble());
        Main.pref.put(getClass().getName() + ".tags", tagsField.getText());
    }

}
