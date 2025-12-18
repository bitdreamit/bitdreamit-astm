package com.bitdreamit.connect.astm;

import com.mirth.connect.client.ui.Frame;
import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.client.ui.UIConstants;
import com.mirth.connect.client.ui.components.MirthSyntaxTextArea;
import com.mirth.connect.client.ui.components.MirthTextField;
import com.mirth.connect.client.ui.panels.connectors.ConnectorSettingsPanel;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.Border;

public class AstmSender extends ConnectorSettingsPanel {
    private Frame parent;
    private AstmConnectorPanel astmConnectorPanel;
    private MirthTextField sendTimeoutField;
    private JLabel sendTimeoutLabel;
    private JPanel senderPanel;
    private JLabel templateLabel;
    private MirthSyntaxTextArea templateTextArea;

    public AstmSender() {
        this.parent = PlatformUI.MIRTH_FRAME;
        this.initComponents();
    }

    public ConnectorProperties getProperties() {
        AstmDispatcherProperties properties = new AstmDispatcherProperties();
        properties.setTemplate(this.templateTextArea.getText());
        properties.setSendTimeout(this.sendTimeoutField.getText());
        return this.astmConnectorPanel.getProperties(properties);
    }

    public void setProperties(ConnectorProperties properties) {
        AstmDispatcherProperties props = (AstmDispatcherProperties)properties;
        this.astmConnectorPanel.setProperties(properties);
        this.templateTextArea.setText(props.getTemplate());
        this.sendTimeoutField.setText(props.getSendTimeout());
    }

    public ConnectorProperties getDefaults() {
        return new AstmDispatcherProperties();
    }

    public boolean checkProperties(ConnectorProperties properties, boolean highlight) {
        AstmDispatcherProperties props = (AstmDispatcherProperties)properties;
        boolean valid = true;
        if (valid) {
            valid = this.astmConnectorPanel.checkProperties(properties, highlight);
        }

        if (props.getTemplate().length() == 0) {
            valid = false;
            if (highlight) {
                this.templateTextArea.setBackground(UIConstants.INVALID_COLOR);
            }
        }

        return valid;
    }

    public void resetInvalidProperties() {
        this.templateTextArea.setBackground((Color)null);
    }

    public String getConnectorName() {
        return (new AstmDispatcherProperties()).getName();
    }

    private void initComponents() {
        this.astmConnectorPanel = new AstmConnectorPanel();
        this.senderPanel = new JPanel();
        this.sendTimeoutLabel = new JLabel();
        this.sendTimeoutField = new MirthTextField();
        this.templateLabel = new JLabel();
        this.templateTextArea = new MirthSyntaxTextArea();
        this.setBackground(new Color(255, 255, 255));
        this.setLayout(new BoxLayout(this, 3));
        this.astmConnectorPanel.setBorder((Border)null);
        this.astmConnectorPanel.setAlignmentX(0.0F);
        this.astmConnectorPanel.setMaximumSize((Dimension)null);
        this.astmConnectorPanel.setMinimumSize((Dimension)null);
        this.astmConnectorPanel.setPreferredSize((Dimension)null);
        this.add(this.astmConnectorPanel);
        this.senderPanel.setBackground(Color.white);
        this.senderPanel.setAlignmentX(0.0F);
        this.senderPanel.setAutoscrolls(true);
        this.senderPanel.setCursor(new Cursor(0));
        this.senderPanel.setMaximumSize((Dimension)null);
        this.sendTimeoutLabel.setText("Send Timeout (ms):");
        this.sendTimeoutField.setToolTipText("<html>Sets the message send timeout in milliseconds.<br>If the message is already consumed by the driver, the timeout will be ignored.<br>A timeout value of zero is interpreted as an infinite timeout.</html>");
        this.templateLabel.setText("Template:");
        this.templateLabel.setAlignmentX(0.0F);
        this.templateTextArea.setBorder(BorderFactory.createEtchedBorder());
        this.templateTextArea.setAlignmentX(0.0F);
        this.templateTextArea.setMaximumSize((Dimension)null);
        this.templateTextArea.setMinimumSize(new Dimension(500, 100));
        this.templateTextArea.setName("");
        this.templateTextArea.setPreferredSize(new Dimension(500, 100));
        GroupLayout senderPanelLayout = new GroupLayout(this.senderPanel);
        this.senderPanel.setLayout(senderPanelLayout);
        senderPanelLayout.setHorizontalGroup(senderPanelLayout.createParallelGroup(Alignment.LEADING).addGroup(senderPanelLayout.createSequentialGroup().addContainerGap().addGroup(senderPanelLayout.createParallelGroup(Alignment.TRAILING).addComponent(this.templateLabel).addComponent(this.sendTimeoutLabel)).addPreferredGap(ComponentPlacement.RELATED).addGroup(senderPanelLayout.createParallelGroup(Alignment.LEADING).addComponent(this.templateTextArea, -1, -1, 32767).addGroup(senderPanelLayout.createSequentialGroup().addComponent(this.sendTimeoutField, -2, 75, -2).addGap(0, 0, 0))).addContainerGap()));
        senderPanelLayout.setVerticalGroup(senderPanelLayout.createParallelGroup(Alignment.LEADING).addGroup(senderPanelLayout.createSequentialGroup().addContainerGap().addGroup(senderPanelLayout.createParallelGroup(Alignment.BASELINE).addComponent(this.sendTimeoutLabel).addComponent(this.sendTimeoutField, -2, -1, -2)).addPreferredGap(ComponentPlacement.UNRELATED).addGroup(senderPanelLayout.createParallelGroup(Alignment.LEADING).addComponent(this.templateTextArea, -1, -1, 32767).addComponent(this.templateLabel)).addContainerGap()));
        this.sendTimeoutField.getAccessibleContext().setAccessibleDescription("<html>Sets the message send timeout in milliseconds.<br>If the message is already consumed by the driver, the timeout will be ignored.<br>A timeout value of zero is interpreted as an infinite timeout.</html>");
        this.add(this.senderPanel);
        this.senderPanel.getAccessibleContext().setAccessibleName("");
        this.senderPanel.getAccessibleContext().setAccessibleDescription("");
    }
}
