package com.bitdreamit.connect.astm;

import com.mirth.connect.client.ui.Frame;
import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.client.ui.panels.connectors.ConnectorSettingsPanel;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.BoxLayout;

public class AstmListener extends ConnectorSettingsPanel {
    private Frame parent;
    private com.bitdreamit.connect.astm.AstmConnectorPanel astmConnectorPanel;

    public AstmListener() {
        this.parent = PlatformUI.MIRTH_FRAME;
        this.initComponents();
    }

    public ConnectorProperties getProperties() {
        AstmReceiverProperties properties = new AstmReceiverProperties();
        return this.astmConnectorPanel.getProperties(properties);
    }

    public void setProperties(ConnectorProperties properties) {
        this.astmConnectorPanel.setProperties(properties);
    }

    public ConnectorProperties getDefaults() {
        return new AstmReceiverProperties();
    }

    public boolean checkProperties(ConnectorProperties properties, boolean highlight) {
        AstmReceiverProperties props = (AstmReceiverProperties)properties;
        boolean valid = true;
        if (valid) {
            valid = this.astmConnectorPanel.checkProperties(properties, highlight);
        }

        return valid;
    }

    public void resetInvalidProperties() {
    }

    public String getConnectorName() {
        return (new AstmReceiverProperties()).getName();
    }

    private void initComponents() {
        this.astmConnectorPanel = new AstmConnectorPanel();
        this.setBackground(Color.white);
        this.setLayout(new BoxLayout(this, 3));
        this.astmConnectorPanel.setMaximumSize(new Dimension(33352, 325));
        this.astmConnectorPanel.setMinimumSize(new Dimension(0, 0));
        this.astmConnectorPanel.setPreferredSize(new Dimension(597, 325));
        this.add(this.astmConnectorPanel);
    }
}
