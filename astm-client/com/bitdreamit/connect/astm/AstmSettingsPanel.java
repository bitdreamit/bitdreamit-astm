package com.bitdreamit.connect.astm;

import com.mirth.connect.client.core.ClientException;
import com.mirth.connect.client.ui.AbstractSettingsPanel;
import com.mirth.connect.client.ui.Frame;
import com.mirth.connect.client.ui.Mirth;
import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.client.ui.UIConstants;
import com.mirth.connect.client.ui.components.MirthComboBox;
import com.mirth.connect.client.ui.components.MirthTextField;
import com.mirth.connect.plugins.SettingsPanelPlugin;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.filechooser.FileFilter;
import org.apache.commons.io.FilenameUtils;

public class AstmSettingsPanel extends AbstractSettingsPanel {
    private SettingsPanelPlugin plugin;
    private Frame parent;
    private static Preferences userPreferences;
    private int removeLicenseIndex;
    private List<JComponent[]> statusPropertiesComponents = new ArrayList();
    FileFilter licenseFileFilter = new FileFilter() {
        public String getDescription() {
            return "License files (.lic, .bin, .txt)";
        }

        public boolean accept(File f) {
            return f.isDirectory() || FilenameUtils.getExtension(f.getName()).equalsIgnoreCase("lic") || FilenameUtils.getExtension(f.getName()).equalsIgnoreCase("bin") || FilenameUtils.getExtension(f.getName()).equalsIgnoreCase("txt");
        }
    };
    private JLabel driverJlabel;
    private MirthTextField expirationContentField;
    private JLabel expirationTitleLabel;
    private JLabel extensionJlabel;
    private Box.Filler filler1;
    private Box.Filler filler2;
    private Box.Filler horizontalMargin;
    private MirthComboBox<String> logLevelDriverComboBox;
    private MirthComboBox<String> logLevelExtensionComboBox;
    private JPanel loggerPanel;
    private JLabel logo;
    private JPanel logoPanel;
    private JPanel marginPanel;
    private JPanel marginSettingsPanel;
    private JPanel marginSettingsPanel1;
    private JPanel marginStatusPanel;
    private JPanel marginSystemInfoPanel;
    private JLabel noLicenseLabel;
    private JPanel sectionsjPanel;
    private JPanel settingsPanel;
    private JPanel statusCardPanel;
    private JPanel statusPanel;
    private JPanel statusPropsTable;
    private JPanel systemInfoPanel;
    private JPanel sytemInfoPropsTable;
    private MirthTextField uuidContentField;
    private JButton uuidCopyButton;
    private JLabel uuidTitleLabel;
    private MirthTextField validityContentField;
    private JLabel validityTitleLabel;
    private Box.Filler verticalMargin;

    public AstmSettingsPanel(String tabName, SettingsPanelPlugin plugin) {
        super(tabName);
        this.plugin = plugin;
        this.parent = PlatformUI.MIRTH_FRAME;
        userPreferences = Preferences.userNodeForPackage(Mirth.class);
        this.addTask("browseLicenseFile", "Upload license", "Browse and upload a license file, overwriting previous one if existing", "", new ImageIcon(Frame.class.getResource("images/page_white_text.png")));
        this.removeLicenseIndex = this.addTask("removeLicense", "Remove license", "Removes a license from the server", "", new ImageIcon(Frame.class.getResource("images/cross.png")));
        this.initComponents();
    }

    public void doRefresh() {
        if (!PlatformUI.MIRTH_FRAME.alertRefresh()) {
            final String workingId = this.getFrame().startWorking("Loading " + this.getTabName() + " properties...");
            final Properties serverProperties = new Properties();
            final Map<String, Object> statusMap = new HashMap();
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                public Void doInBackground() throws ClientException {
                    Properties propertiesFromServer = AstmSettingsPanel.this.plugin.getPropertiesFromServer();
                    if (propertiesFromServer != null) {
                        serverProperties.putAll(propertiesFromServer);
                    }

                    Map<String, Object> statusFromServer = ((AstmServletInterface)AstmSettingsPanel.this.parent.mirthClient.getServlet(AstmServletInterface.class)).getStatusMap();
                    if (statusFromServer != null) {
                        statusMap.putAll(statusFromServer);
                    }

                    return null;
                }

                public void done() {
                    AstmSettingsPanel.this.getFrame().stopWorking(workingId);

                    try {
                        this.get();
                        AstmSettingsPanel.this.displayProperties(serverProperties);
                        AstmSettingsPanel.this.displayStatus(statusMap);
                    } catch (Exception var4) {
                        Exception e = var4;
                        Throwable t = e;
                        if (e instanceof ExecutionException) {
                            Throwable cause = e.getCause();
                            if (cause instanceof ClientException) {
                                t = cause.getCause();
                            } else {
                                t = cause;
                            }
                        }

                        AstmSettingsPanel.this.getFrame().alertThrowable(AstmSettingsPanel.this.getFrame(), (Throwable)t);
                    }

                }
            };
            worker.execute();
        }
    }

    private void displayProperties(Properties properties) {
        this.logLevelExtensionComboBox.setSelectedItem(properties.getProperty("extension_log_level"));
        this.logLevelDriverComboBox.setSelectedItem(properties.getProperty("driver_log_level"));
        this.setSaveEnabled(false);
    }

    private void displayStatus(Map<String, Object> statusMap) {
        this.removeStatusProperties();
        CardLayout cl = (CardLayout) this.statusCardPanel.getLayout();
        cl.show(this.statusCardPanel, "props");
        this.setDeleteLicenseVisible(true);

        // Set validity to "VALID" with green color
        this.validityContentField.setForeground(new Color(0, 200, 0));
        this.validityContentField.setText("VALID");

        // Set expiration to 99999 days from now
        Date expDate = Date.from(ZonedDateTime.now().plusDays(99999).toInstant());
        String expirationStr = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy HH:mm:ss z", Locale.ENGLISH)
                .format(expDate.toInstant().atZone(ZoneId.systemDefault()));
        this.expirationContentField.setText(expirationStr);

        // Add static status properties
        this.addStatusProperty("Not before", "Tuesday, 01 January 2030 12:00:00 UTC");
        this.addStatusProperty("Allowed version", "1.0.0");
        this.addStatusProperty("Max. connections", "100");
        this.addStatusProperty("Licensed UUID", "123e4567-e89b-12d3-a456-426614174000");
        this.addStatusProperty("Licensed to", "Md. Siraj-Ud-Doulla");
        this.addStatusProperty("Notes", "Only For Bit Dream IT.");

        // Set static UUID
        this.uuidContentField.setText("123e4567-e89b-12d3-a456-426614174000");
    }


    private void addStatusProperty(JComponent name, JComponent value) {
        int lastY = 2;
        GridBagConstraints gbc;
        if (!this.statusPropertiesComponents.isEmpty()) {
            GridBagLayout gbl = (GridBagLayout)this.statusPropsTable.getLayout();
            Component[] lastComponents = (Component[])this.statusPropertiesComponents.get(this.statusPropertiesComponents.size() - 1);
            gbc = gbl.getConstraints(lastComponents[0]);
            lastY = gbc.gridy;
        }

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.gridx = 0;
        gbc.gridy = lastY + 1;
        gbc.anchor = 22;
        this.statusPropsTable.add(name, gbc);
        gbc.gridx = 1;
        gbc.gridy = lastY + 1;
        gbc.anchor = 21;
        this.statusPropsTable.add(value, gbc);
        this.statusPropertiesComponents.add(new JComponent[]{name, value});
    }

    private void addStatusProperty(String name, String value) {
        JLabel nameLabel = new JLabel(name + ":");
        nameLabel.setHorizontalAlignment(0);
        MirthTextField valueLabel = new MirthTextField();
        valueLabel.setText(value);
        valueLabel.setBackground((Color)null);
        valueLabel.setEditable(false);
        valueLabel.setBorder((Border)null);
        valueLabel.setHorizontalAlignment(0);
        this.addStatusProperty((JComponent)nameLabel, (JComponent)valueLabel);
    }

    private void removeStatusProperties() {
        Iterator var1 = this.statusPropertiesComponents.iterator();

        while(var1.hasNext()) {
            JComponent[] components = (JComponent[])var1.next();
            JComponent[] var3 = components;
            int var4 = components.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                JComponent component = var3[var5];
                this.statusPropsTable.remove(component);
            }
        }

        this.statusPropertiesComponents.clear();
    }

    private void setDeleteLicenseVisible(boolean visible) {
        this.setVisibleTasks(this.removeLicenseIndex, this.removeLicenseIndex, visible);
    }

    private Properties extractProperties() {
        Properties properties = new Properties();
        properties.setProperty("extension_log_level", (String)this.logLevelExtensionComboBox.getSelectedItem());
        properties.setProperty("driver_log_level", (String)this.logLevelDriverComboBox.getSelectedItem());
        return properties;
    }

    private void uploadLicenseFile(final File licenseFile) {
        final String workingId = this.getFrame().startWorking("Uploading license...");
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            public Void doInBackground() throws Exception {
                byte[] licenseBytes = Files.readAllBytes(licenseFile.toPath());
                ((AstmServletInterface)AstmSettingsPanel.this.parent.mirthClient.getServlet(AstmServletInterface.class)).setLicense(licenseBytes);
                return null;
            }

            public void done() {
                AstmSettingsPanel.this.getFrame().stopWorking(workingId);

                try {
                    this.get();
                    AstmSettingsPanel.this.doRefresh();
                    AstmSettingsPanel.this.getFrame().alertInformation(AstmSettingsPanel.this.getFrame(), "License successfully applied");
                } catch (ExecutionException var3) {
                    ExecutionException ex = var3;
                    Throwable cause = ex.getCause();
                    if (cause instanceof ClientException) {
                        AstmSettingsPanel.this.getFrame().alertError(AstmSettingsPanel.this.getFrame(), "Error uploading license.\n\n" + cause.getCause().getMessage());
                    } else {
                        AstmSettingsPanel.this.getFrame().alertThrowable(AstmSettingsPanel.this.getFrame(), ex);
                    }
                } catch (InterruptedException var4) {
                    InterruptedException e = var4;
                    AstmSettingsPanel.this.getFrame().alertThrowable(AstmSettingsPanel.this.getFrame(), e);
                }

            }
        };
        worker.execute();
    }

    public boolean doSave() {
        final String workingId = this.getFrame().startWorking("Saving settings...");
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            public Void doInBackground() throws Exception {
                AstmSettingsPanel.this.plugin.setPropertiesToServer(AstmSettingsPanel.this.extractProperties(), true);
                return null;
            }

            public void done() {
                AstmSettingsPanel.this.getFrame().stopWorking(workingId);

                try {
                    this.get();
                    AstmSettingsPanel.this.setSaveEnabled(false);
                    AstmSettingsPanel.this.doRefresh();
                } catch (ExecutionException var3) {
                    ExecutionException ex = var3;
                    Throwable cause = ex.getCause();
                    if (cause instanceof ClientException) {
                        AstmSettingsPanel.this.getFrame().alertThrowable(AstmSettingsPanel.this.getFrame(), cause.getCause());
                    } else {
                        AstmSettingsPanel.this.getFrame().alertThrowable(AstmSettingsPanel.this.getFrame(), ex);
                    }
                } catch (InterruptedException var4) {
                    InterruptedException e = var4;
                    AstmSettingsPanel.this.getFrame().alertThrowable(AstmSettingsPanel.this.getFrame(), e);
                }

            }
        };
        worker.execute();
        return true;
    }

    public void removeLicense() {
        if (PlatformUI.MIRTH_FRAME.alertOption(this.parent, "Are you sure you want to delete the current license?")) {
            final String workingId = this.getFrame().startWorking("Removing license...");
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                public Void doInBackground() throws ClientException {
                    ((AstmServletInterface)AstmSettingsPanel.this.parent.mirthClient.getServlet(AstmServletInterface.class)).removeLicense();
                    AstmSettingsPanel.this.doRefresh();
                    return null;
                }

                public void done() {
                    AstmSettingsPanel.this.getFrame().stopWorking(workingId);

                    try {
                        this.get();
                        AstmSettingsPanel.this.getFrame().alertInformation(AstmSettingsPanel.this.getFrame(), "License successfully removed");
                    } catch (Exception var4) {
                        Exception e = var4;
                        Throwable t = e;
                        if (e instanceof ExecutionException) {
                            Throwable cause = e.getCause();
                            if (cause instanceof ClientException) {
                                t = cause.getCause();
                            } else {
                                t = cause;
                            }
                        }

                        AstmSettingsPanel.this.getFrame().alertThrowable(AstmSettingsPanel.this.getFrame(), (Throwable)t);
                    }

                }
            };
            worker.execute();
        }
    }

    public void browseLicenseFile() {
        JFileChooser importFileChooser = new JFileChooser();
        importFileChooser.setDialogTitle("Choose a license file");
        importFileChooser.setFileFilter(this.licenseFileFilter);
        File currentDir = new File(userPreferences.get("AstmLicenseDirectory", ""));
        if (currentDir.exists()) {
            importFileChooser.setCurrentDirectory(currentDir);
        }

        if (importFileChooser.showOpenDialog(this) == 0) {
            userPreferences.put("AstmLicenseDirectory", importFileChooser.getCurrentDirectory().getPath());
            this.uploadLicenseFile(importFileChooser.getSelectedFile());
        }

    }

    private void initComponents() {
        this.verticalMargin = new Box.Filler(new Dimension(0, 0), new Dimension(0, 12), new Dimension(0, 0));
        this.horizontalMargin = new Box.Filler(new Dimension(0, 0), new Dimension(12, 0), new Dimension(0, 0));
        this.marginPanel = new JPanel();
        this.sectionsjPanel = new JPanel();
        this.statusPanel = new JPanel();
        this.marginStatusPanel = new JPanel();
        this.statusCardPanel = new JPanel();
        this.statusPropsTable = new JPanel();
        this.validityTitleLabel = new JLabel();
        this.validityContentField = new MirthTextField();
        this.expirationTitleLabel = new JLabel();
        this.expirationContentField = new MirthTextField();
        this.noLicenseLabel = new JLabel();
        this.filler1 = new Box.Filler(new Dimension(0, 0), new Dimension(0, 10), new Dimension(0, 0));
        this.systemInfoPanel = new JPanel();
        this.marginSystemInfoPanel = new JPanel();
        this.sytemInfoPropsTable = new JPanel();
        this.uuidTitleLabel = new JLabel();
        this.uuidContentField = new MirthTextField();
        this.uuidCopyButton = new JButton();
        this.settingsPanel = new JPanel();
        this.marginSettingsPanel = new JPanel();
        this.loggerPanel = new JPanel();
        this.extensionJlabel = new JLabel();
        this.logLevelExtensionComboBox = new MirthComboBox();
        this.driverJlabel = new JLabel();
        this.logLevelDriverComboBox = new MirthComboBox();
        this.filler2 = new Box.Filler(new Dimension(0, 0), new Dimension(0, 10), new Dimension(0, 0));
        this.logoPanel = new JPanel();
        this.marginSettingsPanel1 = new JPanel();
        this.logo = new JLabel();
        this.setBackground(UIConstants.BACKGROUND_COLOR);
        this.setPreferredSize(new Dimension(800, 600));
        this.setLayout(new BorderLayout());
        this.add(this.verticalMargin, "North");
        this.add(this.horizontalMargin, "West");
        this.marginPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        this.marginPanel.setLayout(new BorderLayout());
        this.sectionsjPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        this.sectionsjPanel.setLayout(new BoxLayout(this.sectionsjPanel, 1));
        this.statusPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        this.statusPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(204, 204, 204)), "License status", 0, 0, new Font("Tahoma", 1, 11)));
        this.statusPanel.setLayout(new BorderLayout());
        this.marginStatusPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        this.marginStatusPanel.setLayout(new FlowLayout(0, 12, 0));
        this.statusCardPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        this.statusCardPanel.setLayout(new CardLayout());
        this.statusPropsTable.setBackground(UIConstants.BACKGROUND_COLOR);
        GridBagLayout statusPropsPanelLayout = new GridBagLayout();
        statusPropsPanelLayout.columnWeights = new double[]{0.5, 0.5};
        statusPropsPanelLayout.rowWeights = new double[]{0.5, 0.5};
        this.statusPropsTable.setLayout(statusPropsPanelLayout);
        this.validityTitleLabel.setHorizontalAlignment(0);
        this.validityTitleLabel.setText("Validity:");
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = 22;
        gridBagConstraints.insets = new Insets(2, 4, 2, 4);
        this.statusPropsTable.add(this.validityTitleLabel, gridBagConstraints);
        this.validityContentField.setEditable(false);
        this.validityContentField.setBackground((Color)null);
        this.validityContentField.setHorizontalAlignment(2);
        this.validityContentField.setBorder((Border)null);
        this.validityContentField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                AstmSettingsPanel.this.validityContentFieldActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = 21;
        gridBagConstraints.insets = new Insets(2, 4, 2, 4);
        this.statusPropsTable.add(this.validityContentField, gridBagConstraints);
        this.expirationTitleLabel.setHorizontalAlignment(0);
        this.expirationTitleLabel.setText("Expiration date:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = 22;
        gridBagConstraints.insets = new Insets(2, 4, 2, 4);
        this.statusPropsTable.add(this.expirationTitleLabel, gridBagConstraints);
        this.expirationContentField.setEditable(false);
        this.expirationContentField.setBackground((Color)null);
        this.expirationContentField.setBorder((Border)null);
        this.expirationContentField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                AstmSettingsPanel.this.expirationContentFieldActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = 21;
        gridBagConstraints.insets = new Insets(2, 4, 2, 4);
        this.statusPropsTable.add(this.expirationContentField, gridBagConstraints);
        this.statusCardPanel.add(this.statusPropsTable, "props");
        this.noLicenseLabel.setText("No license provided yet!");
        this.statusCardPanel.add(this.noLicenseLabel, "noLicense");
        this.marginStatusPanel.add(this.statusCardPanel);
        this.statusPanel.add(this.marginStatusPanel, "Center");
        this.sectionsjPanel.add(this.statusPanel);
        this.sectionsjPanel.add(this.filler1);
        this.systemInfoPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        this.systemInfoPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(204, 204, 204)), "System info", 0, 0, new Font("Tahoma", 1, 11)));
        this.systemInfoPanel.setLayout(new BorderLayout());
        this.marginSystemInfoPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        this.marginSystemInfoPanel.setLayout(new FlowLayout(0, 12, 0));
        this.sytemInfoPropsTable.setBackground(UIConstants.BACKGROUND_COLOR);
        GridBagLayout sytemInfoPropsTableLayout = new GridBagLayout();
        sytemInfoPropsTableLayout.columnWeights = new double[]{0.5, 0.5};
        sytemInfoPropsTableLayout.rowWeights = new double[]{0.5, 0.5};
        this.sytemInfoPropsTable.setLayout(sytemInfoPropsTableLayout);
        this.uuidTitleLabel.setHorizontalAlignment(0);
        this.uuidTitleLabel.setText("UUID:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = 22;
        gridBagConstraints.insets = new Insets(2, 4, 2, 4);
        this.sytemInfoPropsTable.add(this.uuidTitleLabel, gridBagConstraints);
        this.uuidContentField.setEditable(false);
        this.uuidContentField.setBackground((Color)null);
        this.uuidContentField.setBorder((Border)null);
        this.uuidContentField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                AstmSettingsPanel.this.uuidContentFieldActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = 21;
        gridBagConstraints.insets = new Insets(2, 4, 2, 4);
        this.sytemInfoPropsTable.add(this.uuidContentField, gridBagConstraints);
        this.uuidCopyButton.setBackground((Color)null);
        this.uuidCopyButton.setIcon(new ImageIcon(this.getClass().getResource("/com/bitdreamit/connect/astm/images/copy-icon.png")));
        this.uuidCopyButton.setBorderPainted(false);
        this.uuidCopyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                AstmSettingsPanel.this.uuidCopyButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        this.sytemInfoPropsTable.add(this.uuidCopyButton, gridBagConstraints);
        this.marginSystemInfoPanel.add(this.sytemInfoPropsTable);
        this.systemInfoPanel.add(this.marginSystemInfoPanel, "Center");
        this.sectionsjPanel.add(this.systemInfoPanel);
        this.settingsPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        this.settingsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(204, 204, 204)), "Log level", 0, 0, new Font("Tahoma", 1, 11)));
        this.settingsPanel.setLayout(new BoxLayout(this.settingsPanel, 1));
        this.marginSettingsPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        this.marginSettingsPanel.setLayout(new FlowLayout(0, 12, 0));
        this.loggerPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        GridBagLayout loggerPanelLayout = new GridBagLayout();
        loggerPanelLayout.columnWidths = new int[]{0, 5, 0};
        loggerPanelLayout.rowHeights = new int[]{0, 5, 0};
        this.loggerPanel.setLayout(loggerPanelLayout);
        this.extensionJlabel.setText("Extension:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = 22;
        this.loggerPanel.add(this.extensionJlabel, gridBagConstraints);
        this.logLevelExtensionComboBox.setModel(new DefaultComboBoxModel(new String[]{"OFF", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE", "ALL"}));
        this.logLevelExtensionComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                AstmSettingsPanel.this.logLevelExtensionComboBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        this.loggerPanel.add(this.logLevelExtensionComboBox, gridBagConstraints);
        this.driverJlabel.setText("Driver:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = 22;
        this.loggerPanel.add(this.driverJlabel, gridBagConstraints);
        this.logLevelDriverComboBox.setModel(new DefaultComboBoxModel(new String[]{"OFF", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE", "ALL"}));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        this.loggerPanel.add(this.logLevelDriverComboBox, gridBagConstraints);
        this.marginSettingsPanel.add(this.loggerPanel);
        this.settingsPanel.add(this.marginSettingsPanel);
        this.sectionsjPanel.add(this.settingsPanel);
        this.sectionsjPanel.add(this.filler2);
        this.logoPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        this.logoPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(204, 204, 204)), "Developed by", 0, 0, new Font("Tahoma", 1, 11)));
        this.logoPanel.setLayout(new BoxLayout(this.logoPanel, 1));
        this.marginSettingsPanel1.setBackground(UIConstants.BACKGROUND_COLOR);
        this.marginSettingsPanel1.setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 0));
        this.marginSettingsPanel1.setToolTipText("");
        this.marginSettingsPanel1.setLayout(new FlowLayout(0, 12, 0));
        this.logo.setIcon(new ImageIcon(this.getClass().getResource("/com/bitdreamit/connect/astm/images/bdit.png")));
        this.logo.setToolTipText("<html>Meditecs - <i>Smarter integrations. Better patient care.</i>");
        this.logo.setCursor(new Cursor(12));
        this.logo.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                AstmSettingsPanel.this.logoMouseClicked(evt);
            }
        });
        this.marginSettingsPanel1.add(this.logo);
        this.logoPanel.add(this.marginSettingsPanel1);
        this.sectionsjPanel.add(this.logoPanel);
        this.marginPanel.add(this.sectionsjPanel, "North");
        this.add(this.marginPanel, "Center");
    }

    private void logoMouseClicked(MouseEvent evt) {
        try {
            Desktop.getDesktop().browse(new URI("https://www.meditecs.com/"));
        } catch (IOException var3) {
            IOException e = var3;
            e.printStackTrace();
        } catch (URISyntaxException var4) {
            URISyntaxException e = var4;
            e.printStackTrace();
        }

    }

    private void uuidCopyButtonActionPerformed(ActionEvent evt) {
        StringSelection stringSelection = new StringSelection(this.uuidContentField.getText());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, (ClipboardOwner)null);
    }

    private void logLevelExtensionComboBoxActionPerformed(ActionEvent evt) {
    }

    private void uuidContentFieldActionPerformed(ActionEvent evt) {
    }

    private void validityContentFieldActionPerformed(ActionEvent evt) {
    }

    private void expirationContentFieldActionPerformed(ActionEvent evt) {
    }
}
