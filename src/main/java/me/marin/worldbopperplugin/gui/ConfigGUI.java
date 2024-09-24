package me.marin.worldbopperplugin.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import me.marin.worldbopperplugin.io.SavesFolderWatcher;
import me.marin.worldbopperplugin.io.WorldBopperSettings;
import me.marin.worldbopperplugin.util.UpdateUtil;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.text.NumberFormat;

public class ConfigGUI extends JPanel {

    private JCheckBox enableWorldbopper;
    private JFormattedTextField savesBuffer;
    private JButton saveButton;
    private JPanel mainPanel;
    private JPanel checkForUpdatesPanel;
    private JButton checkForUpdatesButton;
    private JButton addPrefixButton;
    private JLabel noPrefixesText;
    private JPanel prefixesPanel;
    private JScrollPane scrollPane;

    private GridBagConstraints gbc;

    public ConfigGUI() {
        $$$setupUI$$$();

        this.setVisible(true);
        this.add(mainPanel);

        updateGUI();

        enableWorldbopper.addActionListener(e -> {
            WorldBopperSettings settings = WorldBopperSettings.getInstance();
            settings.worldbopperEnabled = enableWorldbopper.isSelected();
            WorldBopperSettings.save();
            Jingle.log(Level.INFO, "(WorldBopper) " + (settings.worldbopperEnabled ? "WorldBopper is now active." : "WorldBopper is no longer active."));
        });

        saveButton.addActionListener(e -> {
            Long number = (Long) savesBuffer.getValue();

            WorldBopperSettings settings = WorldBopperSettings.getInstance();

            if (number == null) {
                savesBuffer.setValue(settings.savesBuffer);
                JOptionPane.showMessageDialog(null, "Invalid number: '" + savesBuffer.getText() + "'.");
                return;
            }
            // number has to be between 50-5000
            number = Math.min(5000, number);
            number = Math.max(50, number);

            settings.savesBuffer = number.longValue();
            WorldBopperSettings.save();

            // update visually if number was too small/big
            savesBuffer.setValue(settings.savesBuffer);

            JOptionPane.showMessageDialog(null, "Set world buffer to " + number + ".");
        });

        checkForUpdatesButton.addActionListener(a -> {
            UpdateUtil.checkForUpdatesAndUpdate(false);
        });

        addPrefixButton.addActionListener(a -> {
            WorldBopperSettings.getInstance().worldsToKeep.add(new WorldBopperSettings.KeepWorldInfo("", WorldBopperSettings.KeepCondition.ALWAYS_DELETE));
            WorldBopperSettings.save();
            updateGUI();
            revalidate();
            repaint();
        });
    }

    public void updateGUI() {
        enableWorldbopper.setSelected(WorldBopperSettings.getInstance().worldbopperEnabled);
        savesBuffer.setValue(WorldBopperSettings.getInstance().savesBuffer);

        noPrefixesText.setVisible(WorldBopperSettings.getInstance().worldsToKeep.isEmpty());
        scrollPane.setVisible(!WorldBopperSettings.getInstance().worldsToKeep.isEmpty());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        prefixesPanel.removeAll();

        if (!WorldBopperSettings.getInstance().worldsToKeep.isEmpty()) {
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 100;
            gbc.insets = new Insets(0, 0, 3, 0);
            JLabel label = new JLabel("World prefix:");
            label.setHorizontalAlignment(SwingConstants.LEFT);
            //gbc.ipadx = 150 - label.getWidth();
            prefixesPanel.add(label, gbc);

            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.weightx = 70;
            gbc.ipadx = 0;
            gbc.insets = new Insets(0, 5, 3, 0);
            JLabel label2 = new JLabel("Keep world if:");
            label.setHorizontalAlignment(SwingConstants.LEFT);
            prefixesPanel.add(label2, gbc);

            gbc.gridx = 2;
            gbc.gridy = 0;
            gbc.weightx = 30;
            gbc.ipadx = 0;
            gbc.insets = new Insets(0, 5, 3, 0);
            Component box = Box.createRigidArea(new Dimension(0, 0));
            prefixesPanel.add(box, gbc);
        }
        int row = 1;
        for (WorldBopperSettings.KeepWorldInfo keepWorldInfo : WorldBopperSettings.getInstance().worldsToKeep) {
            JTextField field = new JTextField(keepWorldInfo.getPrefix());
            field.getDocument().addDocumentListener(new DocumentListener() {
                public void changedUpdate(DocumentEvent e) {
                    onChange();
                }
                public void removeUpdate(DocumentEvent e) {
                    onChange();
                }
                public void insertUpdate(DocumentEvent e) {
                    onChange();
                }

                public void onChange() {
                    keepWorldInfo.setPrefix(field.getText());
                    WorldBopperSettings.save();
                }
            });

            JComboBox<String> keepConditionComboBox = new JComboBox<>();
            DefaultComboBoxModel<String> cbModel = new DefaultComboBoxModel<>();
            cbModel.addElement("Always delete");
            cbModel.addElement("Reached Nether");
            cbModel.addElement("Reached Bastion");
            cbModel.addElement("Reached Fortress");
            cbModel.addElement("Reached one structure");
            cbModel.addElement("Reached both structures");
            cbModel.addElement("Reached Nether Exit");
            cbModel.addElement("Reached Stronghold");
            cbModel.addElement("Reached End");
            cbModel.addElement("Completed");
            keepConditionComboBox.setModel(cbModel);
            keepConditionComboBox.setSelectedItem(keepWorldInfo.getCondition().getDisplay());

            JButton deletePrefix = new JButton();
            deletePrefix.setText("Remove prefix");

            keepConditionComboBox.addActionListener(a -> {
                WorldBopperSettings.KeepCondition kc = WorldBopperSettings.KeepCondition.match((String) keepConditionComboBox.getSelectedItem());
                keepWorldInfo.setCondition(kc);
                WorldBopperSettings.save();
            });

            deletePrefix.addActionListener(a -> {
                int choice = JOptionPane.showConfirmDialog(null, "Are you sure you want to remove the prefix '" + keepWorldInfo.getPrefix() + "'?", "Remove prefix?", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                if (choice == JOptionPane.YES_OPTION) {
                    WorldBopperSettings.getInstance().worldsToKeep.remove(keepWorldInfo);
                    WorldBopperSettings.save();
                    updateGUI();
                    revalidate();
                    repaint();
                }
            });

            int top = row == 1 ? 0 : 10;

            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 100;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(top, 0, 0, 0);
            prefixesPanel.add(field, gbc);
            gbc.fill = GridBagConstraints.NONE;

            gbc.gridx = 1;
            gbc.gridy = row;
            gbc.weightx = 70;
            gbc.ipadx = 0;
            gbc.insets = new Insets(top, 5, 0, 0);
            prefixesPanel.add(keepConditionComboBox, gbc);
            gbc.gridx = 2;
            gbc.gridy = row;
            gbc.weightx = 30;
            gbc.ipadx = 0;
            gbc.insets = new Insets(top, 5, 0, 0);
            prefixesPanel.add(deletePrefix, gbc);

            row += 1;
        }

        SavesFolderWatcher.clearWorldsToKeepCache();
    }

    private void createUIComponents() {
        NumberFormat numberFormat = NumberFormat.getNumberInstance();
        numberFormat.setParseIntegerOnly(true);
        savesBuffer = new JFormattedTextField(numberFormat);

        GridBagLayout gbl = new GridBagLayout();
        gbl.columnWidths = new int[]{150, -1, -1};
        prefixesPanel = new JPanel(gbl);
        prefixesPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));

        gbc = new GridBagConstraints();
    }

    // Run this to force IntelliJ to generate GUI code
    public static void main(String[] args) {
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        checkForUpdatesPanel = new JPanel();
        checkForUpdatesPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        mainPanel.add(checkForUpdatesPanel, gbc);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        checkForUpdatesPanel.add(panel1);
        enableWorldbopper = new JCheckBox();
        enableWorldbopper.setText("Enable WorldBopper?");
        panel1.add(enableWorldbopper, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Max worlds folder size:");
        panel1.add(label1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridBagLayout());
        panel1.add(panel2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 5.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 5);
        panel2.add(savesBuffer, gbc);
        saveButton = new JButton();
        saveButton.setText("Update size");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel2.add(saveButton, gbc);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FormLayout("fill:d:grow", "center:d:grow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        panel1.add(panel3, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        scrollPane = new JScrollPane();
        scrollPane.setHorizontalScrollBarPolicy(31);
        CellConstraints cc = new CellConstraints();
        panel3.add(scrollPane, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
        scrollPane.setViewportView(prefixesPanel);
        prefixesPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        noPrefixesText = new JLabel();
        noPrefixesText.setEnabled(true);
        noPrefixesText.setText("No custom prefixes defined. No worlds will be bopped.");
        noPrefixesText.setVisible(false);
        panel3.add(noPrefixesText, cc.xy(1, 3, CellConstraints.CENTER, CellConstraints.DEFAULT));
        addPrefixButton = new JButton();
        addPrefixButton.setText("Add prefix");
        panel3.add(addPrefixButton, cc.xy(1, 5, CellConstraints.CENTER, CellConstraints.DEFAULT));
        checkForUpdatesButton = new JButton();
        checkForUpdatesButton.setText("Check for updates");
        panel1.add(checkForUpdatesButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
