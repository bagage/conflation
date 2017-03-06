// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.conflation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.CompoundBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
import com.vividsolutions.jcs.conflate.polygonmatch.WindowMatcher;

public class MatchFinderPanel extends JPanel {
    private final JComboBox<String> matchFinderComboBox;
    private final CentroidDistanceComponent centroidDistanceComponent;
    private final HausdorffDistanceComponent hausdorffDistanceComponent;

    public MatchFinderPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder(tr("Match finder settings")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        String[] matchFinderStrings = {"DisambiguatingFCMatchFinder", "OneToOneFCMatchFinder" };
        matchFinderComboBox = new JComboBox<>(matchFinderStrings);
        matchFinderComboBox.setSelectedIndex(0);
        JPanel comboboxPanel = new JPanel();
        comboboxPanel.setBorder(BorderFactory.createTitledBorder(tr("Match finder method")));
        comboboxPanel.setAlignmentX(LEFT_ALIGNMENT);
        comboboxPanel.add(matchFinderComboBox);
        add(comboboxPanel);

        add(Box.createRigidArea(new Dimension(0, 10)));

        centroidDistanceComponent = new CentroidDistanceComponent();
        centroidDistanceComponent.setAlignmentX(LEFT_ALIGNMENT);
        add(centroidDistanceComponent);

        hausdorffDistanceComponent = new HausdorffDistanceComponent();
        hausdorffDistanceComponent.setAlignmentX(LEFT_ALIGNMENT);
        add(hausdorffDistanceComponent);
    }

    public FCMatchFinder getMatchFinder() {
        ArrayList<FeatureMatcher> matchers = new ArrayList<>();
        // Use a WindowMatcher limit the search area
        matchers.add(new WindowMatcher(centroidDistanceComponent.getValue()));
        matchers.add(centroidDistanceComponent.getFeatureMatcher());
        if (hausdorffDistanceComponent.isSelected()) {
            matchers.add(hausdorffDistanceComponent.getFeatureMatcher());
        }
        matchers.add(new IdenticalFeatureFilter());
        ChainMatcher chain = new ChainMatcher(matchers.toArray(new FeatureMatcher[matchers.size()]));
        BasicFCMatchFinder basicFinder = new BasicFCMatchFinder(chain);
        FCMatchFinder finder;
        // FIXME: use better method of specifying match finder
        if (matchFinderComboBox.getSelectedItem().equals("DisambiguatingFCMatchFinder"))
            finder = new DisambiguatingFCMatchFinder(basicFinder);
        else if (matchFinderComboBox.getSelectedItem().equals("OneToOneFCMatchFinder"))
            finder = new OneToOneFCMatchFinder(basicFinder);
        else
            finder = new DisambiguatingFCMatchFinder(basicFinder);
        return finder;
    }

    abstract class DistanceComponent extends AbstractScoreComponent {
        SpinnerNumberModel threshDistanceSpinnerModel;
        boolean selected;

        DistanceComponent(String title, boolean mandatory) {
            setBorder(new CompoundBorder(
                    BorderFactory.createTitledBorder(tr(title)),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)));

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));

            JLabel threshDistanceLabel = new JLabel(tr("Threshold distance"));
            threshDistanceLabel.setToolTipText(
                    tr("Distances greater than this will result in a score of zero."));

            //TODO: how to set reasonable default?
            threshDistanceSpinnerModel = new SpinnerNumberModel(20, 0, Double.MAX_VALUE, 1);
            JSpinner threshDistanceSpinner = new JSpinner(threshDistanceSpinnerModel);
            JFormattedTextField spinnerTextField = (
                    (JSpinner.DefaultEditor) threshDistanceSpinner.getEditor()
                    ).getTextField();
            spinnerTextField.setColumns(10);

            selected = mandatory;
            if (!mandatory) {
                JCheckBox checkbox = new JCheckBox();
                checkbox.setSelected(selected);
                threshDistanceLabel.setEnabled(selected);
                threshDistanceSpinner.setEnabled(selected);
                checkbox.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        selected = checkbox.isSelected();
                        threshDistanceLabel.setEnabled(selected);
                        threshDistanceSpinner.setEnabled(selected);
                    }
                });
                panel.add(checkbox);
                panel.add(Box.createRigidArea(new Dimension(5, 0)));
            }

            panel.add(threshDistanceLabel);
            panel.add(Box.createRigidArea(new Dimension(5, 0)));
            panel.add(threshDistanceSpinner);
            add(panel);
        }

        double getValue() {
            return threshDistanceSpinnerModel.getNumber().doubleValue();
        }

        boolean isSelected() {
            return selected;
        }
    }

    class CentroidDistanceComponent extends DistanceComponent {
        CentroidDistanceComponent() {
            super(tr("Centroid distance"), true);
        }

        @Override
        FeatureMatcher getFeatureMatcher() {
            AbstractDistanceMatcher matcher = new CentroidDistanceMatcher();
            matcher.setMaxDistance(getValue());
            return matcher;
        }
    }

    class HausdorffDistanceComponent extends DistanceComponent {
        HausdorffDistanceComponent() {
            super(tr("Hausdorff distance"), false);
        }

        @Override
        FeatureMatcher getFeatureMatcher() {
            AbstractDistanceMatcher matcher = new HausdorffDistanceMatcher();
            matcher.setMaxDistance(getValue());
            return matcher;
        }
    }

    abstract class AbstractScoreComponent extends JPanel {
        abstract FeatureMatcher getFeatureMatcher();
    }
}
