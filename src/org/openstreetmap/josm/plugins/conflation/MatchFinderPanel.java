package org.openstreetmap.josm.plugins.conflation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import net.miginfocom.swing.MigLayout;

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


public class MatchFinderPanel extends JPanel {
    private final JComboBox<String> matchFinderComboBox;
    private final CentroidDistanceComponent centroidDistanceComponent;

    public MatchFinderPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder(tr("Match finder settings")));

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
    }

    public FCMatchFinder getMatchFinder() {
        IdenticalFeatureFilter identical = new IdenticalFeatureFilter();
        FeatureMatcher[] matchers = {centroidDistanceComponent.getFeatureMatcher(), identical};
        ChainMatcher chain = new ChainMatcher(matchers);
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

        public DistanceComponent(String title) {
            setBorder(BorderFactory.createTitledBorder(title));
            setLayout(new MigLayout());
            JLabel threshDistanceLabel = new JLabel(tr("Threshold distance"));
            threshDistanceLabel.setToolTipText(tr("Distances greater than this will result in a score of zero."));
            //TODO: how to set reasonable default?
            threshDistanceSpinnerModel = new SpinnerNumberModel(20, 0, Double.MAX_VALUE, 1);
            JSpinner threshDistanceSpinner = new JSpinner(threshDistanceSpinnerModel);
            threshDistanceSpinner.setMaximumSize(new Dimension(100, 20));
            add(threshDistanceLabel);
            add(threshDistanceSpinner);
        }
    }

    class CentroidDistanceComponent extends DistanceComponent {
        public CentroidDistanceComponent() {
            super(tr("Centroid distance"));
        }

        @Override
        FeatureMatcher getFeatureMatcher() {
            AbstractDistanceMatcher matcher = new CentroidDistanceMatcher();
            matcher.setMaxDistance(threshDistanceSpinnerModel.getNumber().doubleValue());
            return matcher;
        }
    }

    class HausdorffDistanceComponent extends DistanceComponent {
        public HausdorffDistanceComponent() {
            super(tr("Hausdorff distance"));
        }

        @Override
        FeatureMatcher getFeatureMatcher() {
            AbstractDistanceMatcher matcher = new HausdorffDistanceMatcher();
            matcher.setMaxDistance(threshDistanceSpinnerModel.getNumber().doubleValue());
            return matcher;
        }
    }

    abstract class AbstractScoreComponent extends JPanel {
        abstract FeatureMatcher getFeatureMatcher();
    }
}
