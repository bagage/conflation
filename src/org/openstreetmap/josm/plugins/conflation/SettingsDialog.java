// License: GPL. See LICENSE file for details. Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Dialog for selecting objects and configuring conflation settings
 */
public class SettingsDialog extends ExtendedDialog {

    private JButton freezeReferenceButton;
    private JButton freezeSubjectButton;
    private JPanel jPanel3;
    private JPanel jPanel5;
    private JButton restoreReferenceButton;
    private JButton restoreSubjectButton;
    private JLabel referenceLayerLabel;
    private JPanel referencePanel;
    private JLabel referenceSelectionLabel;
    private JLabel subjectLayerLabel;
    private JPanel subjectPanel;
    private JLabel subjectSelectionLabel;
    private MatchFinderPanel matchFinderPanel;

    List<OsmPrimitive> subjectSelection = null;
    List<OsmPrimitive> referenceSelection = null;
    OsmDataLayer referenceLayer;
    DataSet subjectDataSet;
    OsmDataLayer subjectLayer;
    DataSet referenceDataSet;

    public SettingsDialog() {
        super(Main.parent,
                tr("Configure conflation settings"),
                new String[]{tr("Generate matches"), tr("Cancel")},
                false);
        setButtonIcons(new Icon[] {ImageProvider.get("ok"),
                ImageProvider.get("cancel")});
        referenceSelection = new ArrayList<>();
        subjectSelection = new ArrayList<>();
        initComponents();
    }

    /**
     * Build GUI components
     */
    private void initComponents() {
        referencePanel = new JPanel();
        referenceLayerLabel = new JLabel();
        referenceSelectionLabel = new JLabel();

        jPanel3 = new JPanel();
        restoreReferenceButton = new JButton(new RestoreReferenceAction());
        freezeReferenceButton = new JButton(new FreezeReferenceAction());

        subjectPanel = new JPanel();
        subjectLayerLabel = new JLabel();
        subjectSelectionLabel = new JLabel();

        jPanel5 = new JPanel();
        restoreSubjectButton = new JButton(new RestoreSubjectAction());
        freezeSubjectButton = new JButton(new FreezeSubjectAction());

        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.PAGE_AXIS));

        referencePanel.setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder(tr("Reference")),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        referencePanel.setAlignmentX(LEFT_ALIGNMENT);
        referencePanel.setLayout(new BoxLayout(referencePanel,
                BoxLayout.PAGE_AXIS));

        JPanel referenceLayerPanel = new JPanel();
        referenceLayerPanel.setLayout(new BoxLayout(referenceLayerPanel,
                BoxLayout.LINE_AXIS));
        referenceLayerPanel.setAlignmentX(LEFT_ALIGNMENT);
        referenceLayerPanel.add(new JLabel(tr("Layer:")));
        referenceLayerPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        referenceLayerLabel.setText("(none)");
        referenceLayerPanel.add(referenceLayerLabel);
        referencePanel.add(referenceLayerPanel);

        referenceSelectionLabel.setText(tr("{0}: 0 / {1}: 0 / {2}: 0",
                "Relations", "Ways", "Nodes"));
        referenceSelectionLabel.setAlignmentX(LEFT_ALIGNMENT);
        referencePanel.add(referenceSelectionLabel);

        jPanel3.setLayout(new BoxLayout(jPanel3, BoxLayout.LINE_AXIS));
        jPanel3.setAlignmentX(LEFT_ALIGNMENT);
        jPanel3.add(Box.createHorizontalGlue());
        restoreReferenceButton.setText(tr("Restore"));
        jPanel3.add(restoreReferenceButton);
        jPanel3.add(Box.createRigidArea(new Dimension(5, 0)));
        jPanel3.add(freezeReferenceButton);
        jPanel3.add(Box.createHorizontalGlue());

        referencePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        referencePanel.add(jPanel3);
        pnl.add(referencePanel);

        pnl.add(Box.createRigidArea(new Dimension(0, 10)));

        subjectPanel.setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder(tr("Subject")),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        subjectPanel.setAlignmentX(LEFT_ALIGNMENT);
        subjectPanel.setLayout(new BoxLayout(subjectPanel, BoxLayout.PAGE_AXIS));

        JPanel subjectLayerPanel = new JPanel();
        subjectLayerPanel.setLayout(new BoxLayout(subjectLayerPanel,
                BoxLayout.LINE_AXIS));
        subjectLayerPanel.setAlignmentX(LEFT_ALIGNMENT);
        subjectLayerPanel.add(new JLabel(tr("Layer:")));
        subjectLayerPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        subjectLayerLabel.setText("(none)");
        subjectLayerPanel.add(subjectLayerLabel);
        subjectPanel.add(subjectLayerPanel);

        subjectSelectionLabel.setText(tr("{0}: 0 / {1}: 0 / {2}: 0",
                "Relations", "Ways", "Nodes"));
        subjectPanel.add(subjectSelectionLabel);

        jPanel5.setLayout(new BoxLayout(jPanel5, BoxLayout.LINE_AXIS));
        jPanel5.setAlignmentX(LEFT_ALIGNMENT);
        jPanel5.add(Box.createHorizontalGlue());
        restoreSubjectButton.setText(tr("Restore"));
        jPanel5.add(restoreSubjectButton);
        jPanel5.add(Box.createRigidArea(new Dimension(5, 0)));
        freezeSubjectButton.setText(tr("Freeze"));
        jPanel5.add(freezeSubjectButton);
        jPanel5.add(Box.createHorizontalGlue());
        subjectPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        subjectPanel.add(jPanel5);
        pnl.add(subjectPanel);

        pnl.add(Box.createRigidArea(new Dimension(0, 10)));

        matchFinderPanel = new MatchFinderPanel();
        matchFinderPanel.setAlignmentX(LEFT_ALIGNMENT);
        pnl.add(matchFinderPanel);

        setContent(pnl);
        setupDialog();
    }

    /**
     * Matches are actually generated in windowClosed event in ConflationToggleDialog
     *
     * @param buttonIndex
     * @param evt
     */
    @Override
    protected void buttonAction(int buttonIndex, ActionEvent evt) {
        // "Generate matches" as clicked
        if (buttonIndex == 0) {
            if (referenceSelection.isEmpty() || subjectSelection.isEmpty()) {
                JOptionPane.showMessageDialog(Main.parent, tr("Selections must be made for both reference and subject."), tr("Incomplete selections"), JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        super.buttonAction(buttonIndex, evt);
    }

    /**
     * @return the settings
     */
    public SimpleMatchSettings getSettings() {
        SimpleMatchSettings settings = new SimpleMatchSettings();
        settings.setReferenceDataSet(referenceDataSet);
        settings.setReferenceLayer(referenceLayer);
        settings.setReferenceSelection(referenceSelection);
        settings.setSubjectDataSet(subjectDataSet);
        settings.setSubjectLayer(subjectLayer);
        settings.setSubjectSelection(subjectSelection);
        settings.setMatchFinder(matchFinderPanel.getMatchFinder());

        return settings;
    }

    //    /**
    //     * @param settings the settings to set
    //     */
    //    public void setSettings(SimpleMatchSettings settings) {
    //        referenceDataSet = settings.getReferenceDataSet();
    //        referenceLayer = settings.getReferenceLayer();
    //        referenceSelection = settings.getReferenceSelection();
    //        subjectDataSet = settings.getSubjectDataSet();
    //        subjectLayer = settings.getSubjectLayer();
    //        subjectSelection = settings.getSubjectSelection();
    //        update();
    //        //matchFinderPanel.matchFinder = settings.getMatchFinder();
    //    }

    class RestoreSubjectAction extends JosmAction {

        public RestoreSubjectAction() {
            super(tr("Restore"), null, tr("Restore subject selection"), null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (subjectLayer != null && subjectDataSet != null && subjectSelection != null && !subjectSelection.isEmpty()) {
                Main.map.mapView.setActiveLayer(subjectLayer);
                subjectLayer.setVisible(true);
                subjectDataSet.setSelected(subjectSelection);
            }
        }
    }

    class RestoreReferenceAction extends JosmAction {

        public RestoreReferenceAction() {
            super(tr("Restore"), null, tr("Restore reference selection"), null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (referenceLayer != null && referenceDataSet != null && referenceSelection != null && !referenceSelection.isEmpty()) {
                Main.map.mapView.setActiveLayer(referenceLayer);
                referenceLayer.setVisible(true);
                referenceDataSet.setSelected(referenceSelection);
            }
        }
    }

    class FreezeSubjectAction extends JosmAction {

        public FreezeSubjectAction() {
            super(tr("Freeze"), null, tr("Freeze subject selection"), null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (subjectDataSet != null && subjectDataSet == Main.main.getCurrentDataSet()) {
                //                subjectDataSet.removeDataSetListener(this); FIXME:
                //                subjectDataSet.removeDataSetListener(this); FIXME:
            }
            subjectDataSet = Main.main.getCurrentDataSet();
            //            subjectDataSet.addDataSetListener(tableModel); FIXME:
            //            subjectDataSet.addDataSetListener(tableModel); FIXME:
            subjectLayer = Main.main.getEditLayer();
            if (subjectDataSet == null || subjectLayer == null) {
                JOptionPane.showMessageDialog(Main.parent, tr("No valid OSM data layer present."), tr("Error freezing selection"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            subjectSelection.clear();
            subjectSelection.addAll(subjectDataSet.getSelected());
            if (subjectSelection.isEmpty()) {
                JOptionPane.showMessageDialog(Main.parent, tr("Nothing is selected, please try again."), tr("Empty selection"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            update();
        }
    }

    class FreezeReferenceAction extends JosmAction {

        public FreezeReferenceAction() {
            super(tr("Freeze"), null, tr("Freeze reference selection"), null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (referenceDataSet != null && referenceDataSet == Main.main.getCurrentDataSet()) {
                //                referenceDataSet.removeDataSetListener(this); FIXME:
                //                referenceDataSet.removeDataSetListener(this); FIXME:
            }
            referenceDataSet = Main.main.getCurrentDataSet();
            //            referenceDataSet.addDataSetListener(this); FIXME:
            //            referenceDataSet.addDataSetListener(this); FIXME:
            referenceLayer = Main.main.getEditLayer();
            if (referenceDataSet == null || referenceLayer == null) {
                JOptionPane.showMessageDialog(Main.parent, tr("No valid OSM data layer present."), tr("Error freezing selection"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            referenceSelection.clear();
            referenceSelection.addAll(referenceDataSet.getSelected());
            if (referenceSelection.isEmpty()) {
                JOptionPane.showMessageDialog(Main.parent, tr("Nothing is selected, please try again."), tr("Empty selection"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            update();
        }
    }

    /**
     * Update GUI elements
     */
    void update() {
        int numNodes = 0;
        int numWays = 0;
        int numRelations = 0;

        if (!subjectSelection.isEmpty()) {
            for (OsmPrimitive p : subjectSelection) {
                if (p instanceof Node) {
                    numNodes++;
                } else if (p instanceof Way) {
                    numWays++;
                } else if (p instanceof Relation) {
                    numRelations++;
                }
            }
            subjectLayerLabel.setText(subjectLayer.getName());
            subjectSelectionLabel.setText(tr("{0}: {1} / {2}: {3} / {4}: {5}",
                    "Relations", numRelations, "Ways", numWays, "Nodes", numNodes));
        }
        numNodes = 0;
        numWays = 0;
        numRelations = 0;
        if (!referenceSelection.isEmpty()) {
            for (OsmPrimitive p : referenceSelection) {
                if (p instanceof Node) {
                    numNodes++;
                } else if (p instanceof Way) {
                    numWays++;
                } else if (p instanceof Relation) {
                    numRelations++;
                }
            }
            referenceLayerLabel.setText(referenceLayer.getName());
            referenceSelectionLabel.setText(tr("{0}: {1} / {2}: {3} / {4}: {5}",
                    "Relations", numRelations, "Ways", numWays, "Nodes", numNodes));
        }

        //FIXME: properly update match finder settings
    }
}
