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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.LayoutStyle;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.AbstractPrimitive;
import org.openstreetmap.josm.data.osm.AbstractPrimitive.KeyValueVisitor;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionItemPriority;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;
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
    private JCheckBox replaceGeometryCheckBox;
    private JCheckBox mergeTagsCheckBox;
    private JCheckBox mergeAllCheckBox;
    private DefaultPromptTextField mergeTagsField;
    private DefaultPromptTextField mergeTagsExceptField;
    private JCheckBox overwriteTagsCheckbox; // may be null
    private DefaultPromptTextField overwriteTagsField; // may be null
    private AutoCompletionList referenceTagsAutoCompletionList = new AutoCompletionList();

    private static final Font italicLabelFont = UIManager.getFont("Label.font").deriveFont(Font.ITALIC);
    private static final Font plainLabelFont = UIManager.getFont("Label.font").deriveFont(Font.PLAIN);
    private static final String UNSELECTED_LAYER_NAME = tr("<Please select data>");

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
        restoreFromPreferences();
        initListeners();
        update();
    }

    /**
     * Build GUI components
     */
    private void initComponents() {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.PAGE_AXIS));
        pnl.setAlignmentX(LEFT_ALIGNMENT);
        pnl.add(createDataLayersPanel());
        pnl.add(createMatchFinderBox());
        pnl.add(createMergingPanel());
        setContent(pnl);
        setupDialog();
    }

    private void initListeners() {
        final SelectionChangedListener selectionChangeListener = new SelectionChangedListener() {
            @Override
            public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
                updateFreezeButtons(!newSelection.isEmpty());
            }
        };
        final MainLayerManager.ActiveLayerChangeListener layerChangeListener = new MainLayerManager.ActiveLayerChangeListener() {
            @Override
            public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
                updateFreezeButtons();
            }

        };
        this.addComponentListener(new ComponentAdapter() {
            private boolean listenersAdded = false;

            @Override
            public void componentHidden(ComponentEvent e) {
                if (listenersAdded) {
                    DataSet.removeSelectionListener(selectionChangeListener);
                    Main.getLayerManager().removeActiveLayerChangeListener(layerChangeListener);
                    listenersAdded = false;
                }
            }

            @Override
            public void componentShown(ComponentEvent e) {
                if (!listenersAdded) {
                    DataSet.addSelectionListener(selectionChangeListener);
                    Main.getLayerManager().addActiveLayerChangeListener(layerChangeListener);
                    listenersAdded = true;
                }
                updateFreezeButtons();
            }
        });
    }

    public JPanel createDataLayersPanel() {
        JPanel panel = new JPanel();
        //panel.setBorder(createLightTitleBorder(tr("Data")));
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
        referenceLayerLabel = new JLabel();
        subjectLayerLabel = new JLabel();
        referenceLayerLabel.setOpaque(true);
        subjectLayerLabel.setOpaque(true);
        JLabel empty1 = new JLabel();
        JLabel empty2 = new JLabel();
        JLabel empty3 = new JLabel();

        nodesLabel.setFont(italicLabelFont);
        waysLabel.setFont(italicLabelFont);
        relationsLabel.setFont(italicLabelFont);
        nbReferenceNodesLabel.setFont(italicLabelFont);
        nbReferenceWaysLabel.setFont(italicLabelFont);
        nbReferenceRelationsLabel.setFont(italicLabelFont);
        nbSubjectNodesLabel.setFont(italicLabelFont);
        nbSubjectWaysLabel.setFont(italicLabelFont);
        nbSubjectRelationsLabel.setFont(italicLabelFont);

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
                        .addComponent(subjectLayerLabel, 200, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE))
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
        simpleMatchFinderPanel = new SimpleMatchFinderPanel(referenceTagsAutoCompletionList);
        advancedMatchFinderPanel = new AdvancedMatchFinderPanel(referenceTagsAutoCompletionList);
        if (ExpertToggleAction.isExpert()) {
            programmingMatchFinderPanel = new ProgrammingMatchFinderPanel();
        }

        JRadioButton simpleRadioButton = new JRadioButton(tr("Simple"));
        JRadioButton advancedRadioButton = new JRadioButton(tr("Advanced"));
        JRadioButton programmimgRadioButton = new JRadioButton(tr("Programming"));
        simpleRadioButton.setFont(plainLabelFont);
        advancedRadioButton.setFont(plainLabelFont);
        programmimgRadioButton.setFont(plainLabelFont);
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
                Component componentToSelect = null;
                if (event.getSource() == simpleRadioButton) {
                    componentToSelect = simpleMatchFinderPanel;
                } else if (event.getSource() == advancedRadioButton) {
                    componentToSelect = advancedMatchFinderPanel;
                } else if (event.getSource() == programmimgRadioButton) {
                    componentToSelect = programmingMatchFinderPanel;
                }
                Component currentComponent = (selectedMatchFinderBox.getComponentCount() > 0) ? selectedMatchFinderBox.getComponent(0) : null;
                if ((componentToSelect != currentComponent) && (componentToSelect != null)) {
                    selectedMatchFinderBox.remove(0);
                    selectedMatchFinderBox.add(componentToSelect);
                    selectedMatchFinderBox.revalidate();
                    SettingsDialog.this.pack();
                    selectedMatchFinderBox.repaint();
                }
            }
        };
        simpleRadioButton.addActionListener(modeChangedLiseter);
        advancedRadioButton.addActionListener(modeChangedLiseter);
        programmimgRadioButton.addActionListener(modeChangedLiseter);

        Box box = Box.createVerticalBox();
        box.setBorder(createLightTitleBorder(tr("Matching")));
        box.add(complexitySelectionBox);
        box.add(Box.createRigidArea(new Dimension(1, 5)));
        box.add(selectedMatchFinderBox);

        return box;
    }

    private JPanel createMergingPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(createLightTitleBorder(tr("Merging")));
        replaceGeometryCheckBox = new JCheckBox(tr("Replace Geometry"));
        mergeTagsCheckBox = new JCheckBox(tr("Merge Tags"));
        mergeAllCheckBox = new JCheckBox(tr("All"));
        mergeTagsField = new DefaultPromptTextField(20, tr("List of tags to merge"));
        mergeTagsField.setAutoCompletionList(referenceTagsAutoCompletionList);
        mergeTagsExceptField = new DefaultPromptTextField(20, tr("List of tags to NOT merge"));
        mergeTagsExceptField.setAutoCompletionList(referenceTagsAutoCompletionList);
        if (ExpertToggleAction.isExpert()) {
            overwriteTagsCheckbox = new JCheckBox(tr("Overwrite tags without confirmation"));
            overwriteTagsField = new DefaultPromptTextField(20, tr("none"));
            overwriteTagsField.setToolTipText(tr("List of tags to overwrite without confirmation"));
            overwriteTagsField.setAutoCompletionList(referenceTagsAutoCompletionList);
            overwriteTagsCheckbox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    overwriteTagsField.setEnabled(overwriteTagsCheckbox.isSelected());
                }
            });
        }
        mergeTagsCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mergeAllCheckBox.setEnabled(mergeTagsCheckBox.isSelected());
                mergeTagsField.setEnabled(mergeTagsCheckBox.isSelected());
                mergeTagsExceptField.setEnabled(mergeTagsCheckBox.isSelected());
                if (mergeTagsCheckBox.isSelected()) {
                    mergeTagsField.setText("");
                    mergeTagsExceptField.setText("");
                    mergeAllCheckBox.setSelected(true);
                }
            } });
        mergeAllCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mergeAllCheckBox.isSelected()) {
                    mergeTagsField.setText("");
                    mergeTagsExceptField.setText("");
                }
            }
        });
        DocumentListener documentListener = new DocumentListener() {
            private void checkMergeAllCheckBox() {
                boolean noTags = SimpleMatchFinderPanel.splitBySpaceComaOrSemicolon(mergeTagsField.getText()).isEmpty();
                boolean noExceptTags = SimpleMatchFinderPanel.splitBySpaceComaOrSemicolon(mergeTagsExceptField.getText()).isEmpty();
                mergeAllCheckBox.setSelected(noTags && noExceptTags);
            }
            @Override
            public void insertUpdate(DocumentEvent e) {
                checkMergeAllCheckBox();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                checkMergeAllCheckBox();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                checkMergeAllCheckBox();
            }
        };

        mergeTagsField.getDocument().addDocumentListener(documentListener);
        mergeTagsExceptField.getDocument().addDocumentListener(documentListener);

        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);
        ParallelGroup horizonatGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(replaceGeometryCheckBox)
                .addGroup(layout.createSequentialGroup()
                        .addComponent(mergeTagsCheckBox)
                        .addComponent(mergeAllCheckBox)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 5, Short.MAX_VALUE)
                        .addComponent(mergeTagsField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 5, Short.MAX_VALUE)
                        .addComponent(mergeTagsExceptField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        );
        SequentialGroup verticalGroup = layout.createSequentialGroup()
                .addComponent(replaceGeometryCheckBox)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(mergeTagsCheckBox)
                    .addComponent(mergeAllCheckBox)
                    .addComponent(mergeTagsField)
                    .addComponent(mergeTagsExceptField));
        if (ExpertToggleAction.isExpert()) {
            horizonatGroup.addGroup(layout.createSequentialGroup()
                    .addComponent(overwriteTagsCheckbox)
                    .addComponent(overwriteTagsField,
                            GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 5, Short.MAX_VALUE));
            verticalGroup.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(overwriteTagsCheckbox)
                    .addComponent(overwriteTagsField));
        }
        layout.setHorizontalGroup(horizonatGroup);
        layout.setVerticalGroup(verticalGroup);
        return panel;
    }

    private Border createLightTitleBorder(String title) {
        TitledBorder tileBorder = BorderFactory.createTitledBorder(title);
        tileBorder.setTitleFont(italicLabelFont);
        Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        return new CompoundBorder(tileBorder, emptyBorder);
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
        settings.referenceDataSet = referenceDataSet;
        settings.referenceLayer = referenceLayer;
        settings.referenceSelection = referenceSelection;
        settings.subjectDataSet = subjectDataSet;
        settings.subjectLayer = subjectLayer;
        settings.subjectSelection = subjectSelection;
        settings.matchFinder = getSelectedMatchFinderPanel().getMatchFinder();
        settings.isReplacingGeometry = replaceGeometryCheckBox.isSelected();
        if (mergeTagsCheckBox.isSelected()) {
            List<String> tagsList = SimpleMatchFinderPanel.splitBySpaceComaOrSemicolon(mergeTagsField.getText());
            List<String> tagsExceptList = SimpleMatchFinderPanel.splitBySpaceComaOrSemicolon(mergeTagsExceptField.getText());
            if (!tagsList.isEmpty()) {
                settings.mergeTags = tagsList;
            } else {
                settings.mergeTags = SimpleMatchSettings.ALL;
            }
            if (!tagsExceptList.isEmpty()) {
                settings.mergeTags.removeAll(tagsExceptList);
            }
        } else {
            settings.mergeTags = new ArrayList<>(0);
        }
        if ((overwriteTagsField != null) && (overwriteTagsCheckbox != null) && overwriteTagsCheckbox.isSelected()) {
            settings.overwriteTags = SimpleMatchFinderPanel.splitBySpaceComaOrSemicolon(overwriteTagsField.getText());
        } else {
            settings.overwriteTags = new ArrayList<>(0);
        }
        return settings;
    }

    private MatchFinderPanel getSelectedMatchFinderPanel() {
        return (MatchFinderPanel) selectedMatchFinderBox.getComponent(0);
    }

    public void savePreferences() {
        simpleMatchFinderPanel.savePreferences();
        advancedMatchFinderPanel.savePreferences();
        if (programmingMatchFinderPanel != null) {
            programmingMatchFinderPanel.savePreferences();
        }
        Main.pref.put(getClass().getName() + ".replaceGeometryCheckBox", replaceGeometryCheckBox.isSelected());
        Main.pref.put(getClass().getName() + ".mergeTagsCheckBox", mergeTagsCheckBox.isSelected());
        Main.pref.put(getClass().getName() + ".mergeAllCheckBox", mergeAllCheckBox.isSelected());
        Main.pref.put(getClass().getName() + ".mergeTagsField", mergeTagsField.getText());
        Main.pref.put(getClass().getName() + ".mergeTagsExceptField", mergeTagsExceptField.getText());
        if (overwriteTagsCheckbox != null) {
            Main.pref.put(getClass().getName() + ".overwriteTagsCheckbox", overwriteTagsCheckbox.isSelected());
            Main.pref.put(getClass().getName() + ".overwriteTagsField", overwriteTagsField.getText());
        }
    }

    public void restoreFromPreferences() {
        simpleMatchFinderPanel.restoreFromPreferences();
        advancedMatchFinderPanel.restoreFromPreferences();
        if (programmingMatchFinderPanel != null) {
            programmingMatchFinderPanel.restoreFromPreferences();
        }
        replaceGeometryCheckBox.setSelected(Main.pref.getBoolean(getClass().getName() + ".replaceGeometryCheckBox", true));
        mergeTagsField.setText(Main.pref.get(getClass().getName() + ".mergeTagsField", ""));
        mergeTagsExceptField.setText(Main.pref.get(getClass().getName() + ".mergeTagsExceptField", ""));
        mergeAllCheckBox.setSelected(Main.pref.getBoolean(getClass().getName() + ".mergeAllCheckBox", true));
        mergeTagsCheckBox.setSelected(Main.pref.getBoolean(getClass().getName() + ".mergeTagsCheckBox", true));
        if (overwriteTagsCheckbox != null) {
            overwriteTagsField.setText(Main.pref.get(getClass().getName() + ".overwriteTagsField", ""));
            overwriteTagsCheckbox.setSelected(Main.pref.getBoolean(getClass().getName() + ".overwriteTagsCheckbox", false));
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
        int totalRelations = 0;

        // if subject and reference sets are the same, hint user that this must be wrong
        if (subjectLayer != null && subjectLayer == referenceLayer && !subjectSelection.isEmpty()) {
            boolean identicalSet = (subjectSelection.size() == referenceSelection.size()
                    && new HashSet<>(subjectSelection).containsAll(referenceSelection));
            if (identicalSet) {
                JOptionPane.showMessageDialog(Main.parent,
                        tr("Reference and subject sets should better be different."), tr("Warning"),
                        JOptionPane.WARNING_MESSAGE);
            }
        }

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
            subjectLayerLabel.setOpaque(false);

            nbSubjectNodesLabel.setText("" + numNodes);
            nbSubjectWaysLabel.setText("" + numWays);
            nbSubjectRelationsLabel.setText("" + numRelations);
            restoreSubjectButton.setEnabled(true);
        } else {
            subjectLayerLabel.setText(UNSELECTED_LAYER_NAME);
            subjectLayerLabel.setBackground(Color.YELLOW);
            subjectLayerLabel.setOpaque(true);
            nbSubjectNodesLabel.setText("0");
            nbSubjectWaysLabel.setText("0");
            nbSubjectRelationsLabel.setText("0");
            restoreSubjectButton.setEnabled(false);
        }
        totalRelations += numRelations;
        numNodes = 0;
        numWays = 0;
        numRelations = 0;
        if (!referenceSelection.isEmpty()) {
            HashSet<String> referenceKeys = new HashSet<>();
            KeyValueVisitor referenceKeysVisitor = new KeyValueVisitor() {
                @Override
                public void visitKeyValue(AbstractPrimitive primitive, String key, String value) {
                    referenceKeys.add(key);
                }
            };
            for (OsmPrimitive p : referenceSelection) {
                if (p instanceof Node) {
                    numNodes++;
                } else if (p instanceof Way) {
                    numWays++;
                } else if (p instanceof Relation) {
                    numRelations++;
                }
                p.visitKeys(referenceKeysVisitor);
            }
            referenceKeys.removeAll(OsmPrimitive.getDiscardableKeys());
            referenceTagsAutoCompletionList.clear();
            referenceTagsAutoCompletionList.add(referenceKeys, AutoCompletionItemPriority.IS_IN_DATASET);
            referenceLayerLabel.setText(referenceLayer.getName());
            referenceLayerLabel.setOpaque(false);
            nbReferenceNodesLabel.setText("" + numNodes);
            nbReferenceWaysLabel.setText("" + numWays);
            nbReferenceRelationsLabel.setText("" + numRelations);
            restoreReferenceButton.setEnabled(true);
        } else {
            referenceTagsAutoCompletionList.clear();
            referenceLayerLabel.setText(UNSELECTED_LAYER_NAME);
            referenceLayerLabel.setBackground(Color.YELLOW);
            referenceLayerLabel.setOpaque(true);
            nbReferenceNodesLabel.setText("0");
            nbReferenceWaysLabel.setText("0");
            nbReferenceRelationsLabel.setText("0");
            restoreReferenceButton.setEnabled(false);
        }
        totalRelations += numRelations;
        if (totalRelations != 0) {
            JOptionPane.showMessageDialog(Main.parent,
                    tr("Relations are not supported yet, please do not select them."), tr("Error"),
                    JOptionPane.ERROR_MESSAGE);
        }
        updateFreezeButtons();
    }

    public void updateFreezeButtons() {
        DataSet dataSet = Main.getLayerManager().getEditDataSet();
        updateFreezeButtons((dataSet == null) ? false : !dataSet.getSelected().isEmpty());
    }

    public void updateFreezeButtons(boolean enabled) {
        freezeReferenceButton.setEnabled(enabled);
        freezeSubjectButton.setEnabled(enabled);
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
