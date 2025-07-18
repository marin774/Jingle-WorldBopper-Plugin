package me.marin.worldbopperplugin.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;
import me.marin.worldbopperplugin.io.FileWatcher;
import me.marin.worldbopperplugin.io.InstanceManagerRunnable;
import me.marin.worldbopperplugin.io.SavesFolderWatcher;
import me.marin.worldbopperplugin.io.WorldBopperSettings;
import me.marin.worldbopperplugin.util.UpdateUtil;
import org.apache.logging.log4j.Level;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

import static me.marin.worldbopperplugin.WorldBopperPlugin.log;

public class ConfigGUI extends JPanel {

    @Getter
    private JPanel mainPanel;

    private JCheckBox enableWorldbopper;
    private JButton checkForUpdatesButton;
    private JButton addPrefixButton;
    private JLabel noPrefixesText;
    private JPanel prefixesPanel;
    private JButton clearWorldsNowButton;
    private JPanel boppableWorldsPanel;

    private final GridBagConstraints gbc = new GridBagConstraints();

    public ConfigGUI() {
        $$$setupUI$$$();

        add(mainPanel);
        setVisible(true);

        updateGUI();

        enableWorldbopper.addActionListener(e -> {
            WorldBopperSettings.getInstance().worldbopperEnabled = enableWorldbopper.isSelected();
            WorldBopperSettings.save();
            updateGUI();
        });

        clearWorldsNowButton.addActionListener(a -> {
            clearWorldsNowButton.setEnabled(false);
            AtomicInteger total = new AtomicInteger(0);
            InstanceManagerRunnable.instanceWatchersMap.forEach((i, fws) -> {
                for (FileWatcher fw : fws) {
                    if (fw instanceof SavesFolderWatcher) { // ugly
                        total.addAndGet(((SavesFolderWatcher) fw).clearWorlds());
                    }
                }
            });
            JOptionPane.showMessageDialog(null, String.format("Cleared %d worlds.\n(New worlds will be bopped automatically)", total.get()), "Cleared Worlds", JOptionPane.INFORMATION_MESSAGE);
            clearWorldsNowButton.setEnabled(true);
        });

        checkForUpdatesButton.addActionListener(a -> {
            UpdateUtil.checkForUpdatesAndUpdate(false);
        });

        addPrefixButton.addActionListener(a -> {
            WorldBopperSettings.getInstance().worldsToKeep.add(new WorldBopperSettings.KeepWorldInfo("", WorldBopperSettings.KeepCondition.ALWAYS_DELETE, false));
            WorldBopperSettings.save();
            updateGUI();
            invalidateCache();
        });
    }

    private void createUIComponents() {
        GridBagLayout gbl = new GridBagLayout();
        gbl.columnWidths = new int[]{150, -1, -1, -1};
        prefixesPanel = new JPanel(gbl);
    }

    public void updateGUI() {
        enableWorldbopper.setSelected(WorldBopperSettings.getInstance().worldbopperEnabled);
        boppableWorldsPanel.setVisible(WorldBopperSettings.getInstance().worldbopperEnabled);

        noPrefixesText.setVisible(WorldBopperSettings.getInstance().worldsToKeep.isEmpty());

        prefixesPanel.removeAll();

        gbc.insets = new Insets(0, 0, 0, 0);
        if (!WorldBopperSettings.getInstance().worldsToKeep.isEmpty()) {
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 100;
            JLabel label = new JLabel("World name starts with:");
            label.setHorizontalAlignment(JLabel.LEFT);
            prefixesPanel.add(label, gbc);

            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.weightx = 70;
            JLabel label2 = new JLabel("Keep world if:");
            label.setHorizontalAlignment(JLabel.LEFT);
            prefixesPanel.add(label2, gbc);

            gbc.gridx = 2;
            gbc.gridy = 0;
            gbc.weightx = 30;
            Component box = Box.createRigidArea(new Dimension(0, 0));
            prefixesPanel.add(box, gbc);
        }
        int row = 1;
        for (WorldBopperSettings.KeepWorldInfo keepWorldInfo : WorldBopperSettings.getInstance().worldsToKeep) {
            JTextField field = new JTextField(keepWorldInfo.getPrefix());
            field.setEnabled(!keepWorldInfo.isSpecial());
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
                    invalidateCache();
                }
            });

            JComboBox<String> keepConditionComboBox = getConditionsComboBox();
            keepConditionComboBox.setSelectedItem(keepWorldInfo.getCondition().getDisplay());
            keepConditionComboBox.addActionListener(a -> {
                WorldBopperSettings.KeepCondition kc = WorldBopperSettings.KeepCondition.match((String) keepConditionComboBox.getSelectedItem());
                keepWorldInfo.setCondition(kc);
                WorldBopperSettings.save();
                invalidateCache();
            });

            JButton deletePrefix = new JButton();
            deletePrefix.setText("Remove prefix");
            deletePrefix.setEnabled(!keepWorldInfo.isSpecial());
            deletePrefix.addActionListener(a -> {
                int choice = JOptionPane.showConfirmDialog(null, "Are you sure you want to remove the prefix '" + keepWorldInfo.getPrefix() + "'?", "Remove prefix?", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                if (choice == JOptionPane.YES_OPTION) {
                    WorldBopperSettings.getInstance().worldsToKeep.remove(keepWorldInfo);
                    WorldBopperSettings.save();
                    updateGUI();
                    invalidateCache();
                }
            });

            // Prefix text field
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 100;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 0, 0, 0);
            prefixesPanel.add(field, gbc);
            gbc.fill = GridBagConstraints.NONE;

            // Condition combobox (dropdown)
            gbc.gridx = 1;
            gbc.gridy = row;
            gbc.weightx = 70;
            gbc.ipadx = 0;
            gbc.insets = new Insets(5, 5, 0, 0);
            prefixesPanel.add(keepConditionComboBox, gbc);

            // Remove prefix button
            gbc.gridx = 2;
            gbc.gridy = row;
            gbc.weightx = 30;
            gbc.ipadx = 0;
            gbc.insets = new Insets(5, 5, 0, 1);
            prefixesPanel.add(deletePrefix, gbc);

            row += 1;
        }

        //scrollPane.revalidate();
        prefixesPanel.revalidate();

        revalidate();
        repaint();
    }

    public void invalidateCache() {
        log(Level.DEBUG, "Invalidating cache...");
        InstanceManagerRunnable.instanceWatchersMap.values().forEach(fws -> {
            fws.forEach(fw -> {
                if (fw instanceof SavesFolderWatcher) { // ugly
                    ((SavesFolderWatcher) fw).invalidateCache();
                }
            });
        });
    }

    private static JComboBox<String> getConditionsComboBox() {
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
        return keepConditionComboBox;
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
        mainPanel.setLayout(new GridLayoutManager(4, 1, new Insets(5, 5, 5, 5), -1, -1));
        final Spacer spacer1 = new Spacer();
        mainPanel.add(spacer1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        enableWorldbopper = new JCheckBox();
        enableWorldbopper.setText("Enable WorldBopper");
        mainPanel.add(enableWorldbopper, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkForUpdatesButton = new JButton();
        checkForUpdatesButton.setText("Check for updates");
        mainPanel.add(checkForUpdatesButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        boppableWorldsPanel = new JPanel();
        boppableWorldsPanel.setLayout(new GridLayoutManager(3, 1, new Insets(5, 5, 5, 5), -1, -1));
        mainPanel.add(boppableWorldsPanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        boppableWorldsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Boppable worlds", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        boppableWorldsPanel.add(prefixesPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        noPrefixesText = new JLabel();
        noPrefixesText.setEnabled(true);
        noPrefixesText.setText("No custom prefixes defined. No worlds will be bopped.");
        noPrefixesText.setVisible(false);
        boppableWorldsPanel.add(noPrefixesText, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(10, 0, 0, 0), -1, -1));
        boppableWorldsPanel.add(panel1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        addPrefixButton = new JButton();
        addPrefixButton.setActionCommand("Add prefix");
        addPrefixButton.setText("Add new prefix");
        panel1.add(addPrefixButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        clearWorldsNowButton = new JButton();
        clearWorldsNowButton.setText("Clear worlds now");
        panel1.add(clearWorldsNowButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel1.add(spacer2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
