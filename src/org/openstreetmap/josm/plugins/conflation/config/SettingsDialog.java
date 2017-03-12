// License: GPL. For details, see LICENSE file.
// Copyright 2012 by Josh Doe and others.
package org.openstreetmap.josm.plugins.conflation.config;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.LayoutStyle;
import javax.swing.border.MatteBorder;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.plugins.conflation.SimpleMatchSettings;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Dialog for selecting objects and configuring conflation settings
 */
public class SettingsDialog extends ExtendedDialog {

    private JButton freezeReferenceButton;
    private JButton freezeSubjectButton;
    private JButton restoreReferenceButton;
    private JButton restoreSubjectButton;
    private JLabel referenceLayerLabel;
    private JLabel subjectLayerLabel;
    private JLabel nbReferenceNodesLabel;
    private JLabel nbReferenceWaysLabel;
    private JLabel nbReferenceRelationsLabel;
    private JLabel nbSubjectNodesLabel;
    private JLabel nbSubjectWaysLabel;
    private JLabel nbSubjectRelationsLabel;
    private SimpleMatchFinderPanel simpleMatchFinderPanel;
    private AdvancedMatchFinderPanel advancedMatchFinderPanel;
    private ProgrammingMatchFinderPanel programmingMatchFinderPanel;
    private Box selectedMatchFinderBox;

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
        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.PAGE_AXIS));
        pnl.setAlignmentX(LEFT_ALIGNMENT);
        pnl.add(createLayersPanel());
        pnl.add(createMatchFinderBox());
        setContent(pnl);
        setupDialog();
    }

    public JPanel createLayersPanel() {
        JPanel panel = new JPanel();

        JLabel referenceLabel = new JLabel(tr("Reference:"));
        JLabel subjectLabel = new JLabel(tr("Subject:"));
        JLabel layerLabel = new JLabel(tr("Layer"));
        JLabel waysLabel = new JLabel("W");
        JLabel relationsLabel = new JLabel("R");
        JLabel nodesLabel = new JLabel("N");
        restoreReferenceButton = new JButton(new RestoreReferenceAction());
        freezeReferenceButton = new JButton(new FreezeReferenceAction());
        restoreSubjectButton = new JButton(new RestoreSubjectAction());
        freezeSubjectButton = new JButton(new FreezeSubjectAction());
        nbReferenceNodesLabel = new JLabel("0");
        nbReferenceWaysLabel = new JLabel("0");
        nbReferenceRelationsLabel = new JLabel("0");
        nbSubjectNodesLabel = new JLabel("0");
        nbSubjectWaysLabel = new JLabel("0");
        nbSubjectRelationsLabel = new JLabel("0");
        referenceLayerLabel = new JLabel(tr("?"));
        subjectLayerLabel = new JLabel(tr("?"));
        JLabel empty1 = new JLabel();
        JLabel empty2 = new JLabel();
        JLabel empty3 = new JLabel();

        Font light = new Font(referenceLabel.getFont().getName(), Font.PLAIN, referenceLabel.getFont().getSize());
        nodesLabel.setFont(light);
        waysLabel.setFont(light);
        relationsLabel.setFont(light);
        nbReferenceNodesLabel.setFont(light);
        nbReferenceWaysLabel.setFont(light);
        nbReferenceRelationsLabel.setFont(light);
        nbSubjectNodesLabel.setFont(light);
        nbSubjectWaysLabel.setFont(light);
        nbSubjectRelationsLabel.setFont(light);

        layerLabel.setBorder(new MatteBorder(0, 0, 2, 0, Color.DARK_GRAY));;

        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                   .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 5, 5)
                   .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(empty1)
                        .addComponent(referenceLabel)
                        .addComponent(subjectLabel))
                   .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 10, 10)
                   .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(empty2)
                        .addComponent(freezeReferenceButton)
                        .addComponent(freezeSubjectButton))
                   .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(empty3)
                        .addComponent(restoreReferenceButton)
                        .addComponent(restoreSubjectButton))
                   .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 10, 10)
                   .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(layerLabel)
                        .addComponent(referenceLayerLabel, 200, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE)
                        .addComponent(subjectLayerLabel))
                   .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 30, 30)
                   .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(nodesLabel)
                        .addComponent(nbReferenceNodesLabel)
                        .addComponent(nbSubjectNodesLabel))
                   .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 5, 5)
                   .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(waysLabel)
                        .addComponent(nbReferenceWaysLabel)
                        .addComponent(nbSubjectWaysLabel))
                   .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 5, 5)
                   .addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(relationsLabel)
                        .addComponent(nbReferenceRelationsLabel)
                        .addComponent(nbSubjectRelationsLabel))
                   .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 5, Short.MAX_VALUE)
             );
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                   .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(empty1)
                        .addComponent(empty2)
                        .addComponent(empty3)
                        .addComponent(layerLabel)
                        .addComponent(nodesLabel)
                        .addComponent(waysLabel)
                        .addComponent(relationsLabel))
                   .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(referenceLabel)
                        .addComponent(freezeReferenceButton)
                        .addComponent(restoreReferenceButton)
                        .addComponent(referenceLayerLabel)
                        .addComponent(nbReferenceNodesLabel)
                        .addComponent(nbReferenceWaysLabel)
                        .addComponent(nbReferenceRelationsLabel))
                   .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(subjectLabel)
                        .addComponent(freezeSubjectButton)
                        .addComponent(restoreSubjectButton)
                        .addComponent(subjectLayerLabel)
                        .addComponent(nbSubjectNodesLabel)
                        .addComponent(nbSubjectWaysLabel)
                        .addComponent(nbSubjectRelationsLabel))
                   .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 10, 10)
        );
        return panel;
    }

    private Box createMatchFinderBox() {
        simpleMatchFinderPanel = new SimpleMatchFinderPanel();
        advancedMatchFinderPanel = new AdvancedMatchFinderPanel();
        if (ExpertToggleAction.isExpert()) {
            programmingMatchFinderPanel = new ProgrammingMatchFinderPanel();
        }

        JRadioButton simpleRadioButton = new JRadioButton(tr("Simple"));
        JRadioButton advancedRadioButton = new JRadioButton(tr("Advanced"));
        JRadioButton programmimgRadioButton = new JRadioButton(tr("Programming"));
        Font light = new Font(simpleRadioButton.getFont().getName(), Font.PLAIN, simpleRadioButton.getFont().getSize());
        simpleRadioButton.setFont(light);
        advancedRadioButton.setFont(light);
        programmimgRadioButton.setFont(light);
        ButtonGroup complexitySelectionBGroup = new ButtonGroup();
        complexitySelectionBGroup.add(simpleRadioButton);
        complexitySelectionBGroup.add(advancedRadioButton);
        complexitySelectionBGroup.add(programmimgRadioButton);
        simpleRadioButton.setSelected(true);

        Box complexitySelectionBox = Box.createHorizontalBox();
        complexitySelectionBox.setBorder(BorderFactory.createLoweredBevelBorder());
        complexitySelectionBox.add(simpleRadioButton);
        complexitySelectionBox.add(advancedRadioButton);
        if (programmingMatchFinderPanel != null) {
            complexitySelectionBox.add(programmimgRadioButton);
        }

        selectedMatchFinderBox = Box.createHorizontalBox();
        selectedMatchFinderBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        selectedMatchFinderBox.add(simpleMatchFinderPanel);
        ActionListener modeChangedLiseter = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                selectedMatchFinderBox.remove(0);
                if (event.getSource() == simpleRadioButton) {
                    selectedMatchFinderBox.add(simpleMatchFinderPanel);
                    simpleMatchFinderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                } else if (event.getSource() == advancedRadioButton) {
                    selectedMatchFinderBox.add(advancedMatchFinderPanel);
                } else if (event.getSource() == programmimgRadioButton) {
                    selectedMatchFinderBox.add(programmingMatchFinderPanel);
                }
                selectedMatchFinderBox.revalidate();
                SettingsDialog.this.pack();
                selectedMatchFinderBox.repaint();
            }
        };
        simpleRadioButton.addActionListener(modeChangedLiseter);
        advancedRadioButton.addActionListener(modeChangedLiseter);
        programmimgRadioButton.addActionListener(modeChangedLiseter);

        Box box = Box.createVerticalBox();
        box.add(complexitySelectionBox);
        box.add(Box.createRigidArea(new Dimension(1, 5)));
        box.add(selectedMatchFinderBox);

        return box;
    }


    /**
     * Matches are actually generated in windowClosed event in ConflationToggleDialog
     */
    @Override
    protected void buttonAction(int buttonIndex, ActionEvent evt) {
        // "Generate matches" as clicked
        if (buttonIndex == 0) {
            if (referenceSelection.isEmpty() || subjectSelection.isEmpty()) {
                JOptionPane.showMessageDialog(Main.parent,
                        tr("Selections must be made for both reference and subject."), tr("Incomplete selections"),
                        JOptionPane.ERROR_MESSAGE);
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
        settings.setMatchFinder(getSelectedMatchFinderPanel().getMatchFinder());
        System.out.println(settings.getMatchFinder());
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

    private MatchFinderPanel getSelectedMatchFinderPanel() {
        return (MatchFinderPanel) selectedMatchFinderBox.getComponent(0);
    }

    public void savePreferences() {
        simpleMatchFinderPanel.savePreferences();
        //advancedMatchFinderPanel.savePreferences();
        if (programmingMatchFinderPanel != null) {
            programmingMatchFinderPanel.savePreferences();
        }
    }

    class RestoreSubjectAction extends JosmAction {

        RestoreSubjectAction() {
            super(tr("Restore"), null, tr("Restore subject selection"), null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (subjectLayer != null && subjectDataSet != null && subjectSelection != null && !subjectSelection.isEmpty()) {
                Main.getLayerManager().setActiveLayer(subjectLayer);
                subjectLayer.setVisible(true);
                subjectDataSet.setSelected(subjectSelection);
            }
        }
    }

    class RestoreReferenceAction extends JosmAction {

        RestoreReferenceAction() {
            super(tr("Restore"), null, tr("Restore reference selection"), null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (referenceLayer != null && referenceDataSet != null && referenceSelection != null && !referenceSelection.isEmpty()) {
                Main.getLayerManager().setActiveLayer(referenceLayer);
                referenceLayer.setVisible(true);
                referenceDataSet.setSelected(referenceSelection);
            }
        }
    }

    class FreezeSubjectAction extends JosmAction {

        FreezeSubjectAction() {
            super(tr("Freeze"), null, tr("Freeze subject selection"), null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            subjectDataSet = Main.getLayerManager().getEditDataSet();
            subjectLayer = Main.getLayerManager().getEditLayer();
            if (subjectDataSet == null || subjectLayer == null) {
                JOptionPane.showMessageDialog(Main.parent,
                    tr("No valid OSM data layer present."), tr("Error freezing selection"),
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            subjectSelection.clear();
            subjectSelection.addAll(subjectDataSet.getSelected());
            if (subjectSelection.isEmpty()) {
                JOptionPane.showMessageDialog(Main.parent,
                        tr("Nothing is selected, please try again."), tr("Empty selection"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            update();
        }
    }

    class FreezeReferenceAction extends JosmAction {

        FreezeReferenceAction() {
            super(tr("Freeze"), null, tr("Freeze reference selection"), null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            referenceDataSet = Main.getLayerManager().getEditDataSet();
            referenceLayer = Main.getLayerManager().getEditLayer();
            if (referenceDataSet == null || referenceLayer == null) {
                JOptionPane.showMessageDialog(Main.parent,
                        tr("No valid OSM data layer present."), tr("Error freezing selection"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            referenceSelection.clear();
            referenceSelection.addAll(referenceDataSet.getSelected());
            if (referenceSelection.isEmpty()) {
                JOptionPane.showMessageDialog(Main.parent,
                        tr("Nothing is selected, please try again."), tr("Empty selection"),
                        JOptionPane.ERROR_MESSAGE);
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

            nbSubjectNodesLabel.setText("" + numNodes);
            nbSubjectWaysLabel.setText("" + numWays);
            nbSubjectRelationsLabel.setText("" + numRelations);
        } else {
            subjectLayerLabel.setText(tr("?"));
            nbSubjectNodesLabel.setText("0");
            nbSubjectWaysLabel.setText("0");
            nbSubjectRelationsLabel.setText("0");
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
            nbReferenceNodesLabel.setText("" + numNodes);
            nbReferenceWaysLabel.setText("" + numWays);
            nbReferenceRelationsLabel.setText("" + numRelations);
        } else {
            referenceLayerLabel.setText(tr("?"));
            nbReferenceNodesLabel.setText("0");
            nbReferenceWaysLabel.setText("0");
            nbReferenceRelationsLabel.setText("0");
        }
        //FIXME: properly update match finder settings
    }

    /**
     * To be called when a layer is removed.
     * Clear any reference to the removed layer.
     * @param e the layer remove event.
     */
    public void layerRemoving(LayerRemoveEvent e) {
        Layer removedLayer = e.getRemovedLayer();
        this.clear(removedLayer == referenceLayer, removedLayer == subjectLayer);
    }

    /**
     * Clear some settings.
     * @param shouldClearReference if "Reference" settings should be cleared.
     * @param shouldClearSubject if "Subject" settings should be cleared.
     */
    public void clear(boolean shouldClearReference, boolean shouldClearSubject) {
        if (shouldClearReference || shouldClearSubject) {
            if (shouldClearReference) {
                referenceLayer = null;
                referenceDataSet = null;
                referenceSelection.clear();
            }
            if (shouldClearSubject) {
                subjectLayer = null;
                subjectDataSet = null;
                subjectSelection.clear();
            }
            update();
        }
    }

}
