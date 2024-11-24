package com.bitdreamit.connect.astm;

import com.mirth.connect.client.ui.components.MirthRadioButton;
import com.mirth.connect.client.ui.components.MirthTextField;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

public class AstmConnectorPanel extends JPanel {
    private JLabel AstmDataStructureLabel;
    private MirthTextField addressBindField;
    private MirthRadioButton allInterfacesRadio;
    private MirthRadioButton clientRadio;
    private MirthRadioButton cobasRadio;
    private JLabel conectionModeLabel;
    private ButtonGroup connectionModeGroup;
    private ButtonGroup dataStructureGroup;
    private MirthRadioButton elecsysRadio;
    private ButtonGroup listeningAddressGroup;
    private MirthTextField listeningPortField;
    private JLabel listeningPortLabel;
    private MirthTextField remoteAddressField;
    private JLabel remoteAddressLabel;
    private MirthTextField remotePortField;
    private JLabel remotePortLabel;
    private MirthRadioButton serverRadio;
    private MirthRadioButton specificInterfaceRadio;

    public AstmConnectorPanel() {
        this.initComponents();
    }

    public ConnectorProperties getProperties(AstmProperties properties) {
        if (this.serverRadio.isSelected()) {
            properties.setServerMode(true);
            if (this.allInterfacesRadio.isSelected()) {
                properties.setAllInterfaces(true);
            } else {
                properties.setAllInterfaces(false);
                properties.setAddressBind(this.addressBindField.getText());
            }

            properties.setLocalPort(this.listeningPortField.getText());
        } else if (this.clientRadio.isSelected()) {
            properties.setServerMode(false);
            properties.setRemoteAddress(this.remoteAddressField.getText());
            properties.setRemotePort(this.remotePortField.getText());
        }

        if (this.elecsysRadio.isSelected()) {
            properties.setAstmProtocol("ELECSYS");
        } else if (this.cobasRadio.isSelected()) {
            properties.setAstmProtocol("COBAS");
        }

        return properties;
    }

    public void setProperties(ConnectorProperties properties) {
        AstmProperties props = (AstmProperties)properties;
        this.setServerMode(props.isServerMode(), props.isAllInterfaces());
        this.addressBindField.setText(props.getAddressBind());
        this.listeningPortField.setText(props.getLocalPort());
        this.remoteAddressField.setText(props.getRemoteAddress());
        this.remotePortField.setText(props.getRemotePort());
        if (props.getAstmProtocol().toUpperCase().equals("ELECSYS")) {
            this.elecsysRadio.setSelected(true);
            this.cobasRadio.setSelected(false);
        } else if (props.getAstmProtocol().toUpperCase().equals("COBAS")) {
            this.elecsysRadio.setSelected(false);
            this.cobasRadio.setSelected(true);
        }

    }

    public void setServerMode(boolean server, boolean allInterfaces) {
        if (server) {
            this.setAllInterfacesMode(allInterfaces);
        }

        this.serverRadio.setSelected(server);
        this.listeningPortLabel.setEnabled(server);
        this.listeningPortField.setEnabled(server);
        this.clientRadio.setSelected(!server);
        this.remoteAddressLabel.setEnabled(!server);
        this.remoteAddressField.setEnabled(!server);
        this.remotePortLabel.setEnabled(!server);
        this.remotePortField.setEnabled(!server);
    }

    public void setAllInterfacesMode(boolean selected) {
        this.allInterfacesRadio.setSelected(selected);
        this.specificInterfaceRadio.setSelected(!selected);
        this.addressBindField.setEnabled(!selected);
    }

    public boolean checkProperties(ConnectorProperties properties, boolean highlight) {
        AstmProperties props = (AstmProperties)properties;
        boolean valid = true;
        return valid;
    }

    public void resetInvalidProperties() {
    }

    private void initComponents() {
        this.connectionModeGroup = new ButtonGroup();
        this.listeningAddressGroup = new ButtonGroup();
        this.dataStructureGroup = new ButtonGroup();
        this.listeningPortLabel = new JLabel();
        this.listeningPortField = new MirthTextField();
        this.conectionModeLabel = new JLabel();
        this.serverRadio = new MirthRadioButton();
        this.clientRadio = new MirthRadioButton();
        this.addressBindField = new MirthTextField();
        this.remoteAddressLabel = new JLabel();
        this.remoteAddressField = new MirthTextField();
        this.remotePortField = new MirthTextField();
        this.remotePortLabel = new JLabel();
        this.AstmDataStructureLabel = new JLabel();
        this.elecsysRadio = new MirthRadioButton();
        this.cobasRadio = new MirthRadioButton();
        this.specificInterfaceRadio = new MirthRadioButton();
        this.allInterfacesRadio = new MirthRadioButton();
        this.setBackground(new Color(255, 255, 255));
        this.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        this.setToolTipText("");
        this.setMaximumSize((Dimension)null);
        this.setName("");
        this.listeningPortLabel.setText("Listening port:");
        this.listeningPortField.setToolTipText("<html>Tcp inbound port that server will be listening on.</html>");
        this.conectionModeLabel.setText("Connection mode:");
        this.serverRadio.setBackground(new Color(255, 255, 255));
        this.serverRadio.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        this.connectionModeGroup.add(this.serverRadio);
        this.serverRadio.setText("Server");
        this.serverRadio.setToolTipText("<html>If checked, a tcp server socket will be created or reused using specified configuration.</html>");
        this.serverRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                AstmConnectorPanel.this.serverRadioActionPerformed(evt);
            }
        });
        this.clientRadio.setBackground(new Color(255, 255, 255));
        this.clientRadio.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        this.connectionModeGroup.add(this.clientRadio);
        this.clientRadio.setText("Client");
        this.clientRadio.setToolTipText("<html>If checked, a tcp client socket will be created or reused using specified configuration.</html>");
        this.clientRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                AstmConnectorPanel.this.clientRadioActionPerformed(evt);
            }
        });
        this.addressBindField.setToolTipText("<html>When you run a server on a machine it listens for incoming client connections.<br>Administrators can selectively pick which IP addresses a server process listens on.<br/>This selective picking is called binding.</html>");
        this.remoteAddressLabel.setText("Remote Address:");
        this.remoteAddressField.setToolTipText("<html>The DNS domain name or IP address on which to connect.</html>");
        this.remotePortField.setToolTipText("<html>The port on which to connect.</html>");
        this.remotePortLabel.setText("Remote port:");
        this.AstmDataStructureLabel.setText("ASTM data structure:");
        this.elecsysRadio.setBackground(new Color(255, 255, 255));
        this.elecsysRadio.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        this.dataStructureGroup.add(this.elecsysRadio);
        this.elecsysRadio.setText("Elecsys");
        this.elecsysRadio.setToolTipText("<html>A message consists of multiple records. A record consists of one or more frames.<br>A frame comprises <b>not more than one record</b>. In case a record exceeds 240 bytes,<br>a frame is divided into middle frames and a last frame.<br>[ETB] is used for the middle frame and [ETX] is used for the last frame.</html>");
        this.elecsysRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                AstmConnectorPanel.this.elecsysRadioActionPerformed(evt);
            }
        });
        this.cobasRadio.setBackground(new Color(255, 255, 255));
        this.cobasRadio.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        this.dataStructureGroup.add(this.cobasRadio);
        this.cobasRadio.setText("Cobas");
        this.cobasRadio.setToolTipText("<html>A message consists of several records. A record consists of one or more frames.<br>A frame may comprise <b>multiple records</b>. In case of a record exceeds 240 bytes,<br>a frame is divided into middle frames and a last frame.<br>[ETB] is used for the middle frame and [ETX] is used for the last frame.</html>");
        this.specificInterfaceRadio.setBackground(new Color(255, 255, 255));
        this.specificInterfaceRadio.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        this.listeningAddressGroup.add(this.specificInterfaceRadio);
        this.specificInterfaceRadio.setText("Specific interface:");
        this.specificInterfaceRadio.setToolTipText("<html>If checked, the connector will listen on the specific interface address defined.</html>");
        this.specificInterfaceRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                AstmConnectorPanel.this.specificInterfaceRadioActionPerformed(evt);
            }
        });
        this.allInterfacesRadio.setBackground(new Color(255, 255, 255));
        this.allInterfacesRadio.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        this.listeningAddressGroup.add(this.allInterfacesRadio);
        this.allInterfacesRadio.setText("All interfaces");
        this.allInterfacesRadio.setToolTipText("<html>If checked, the connector will listen on all interfaces, using address 0.0.0.0.</html>");
        this.allInterfacesRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                AstmConnectorPanel.this.allInterfacesRadioActionPerformed(evt);
            }
        });
        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING).addGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(Alignment.LEADING).addGroup(layout.createSequentialGroup().addGap(13, 13, 13).addGroup(layout.createParallelGroup(Alignment.LEADING).addGroup(layout.createSequentialGroup().addGap(12, 12, 12).addGroup(layout.createParallelGroup(Alignment.LEADING).addGroup(Alignment.TRAILING, layout.createSequentialGroup().addGap(42, 42, 42).addGroup(layout.createParallelGroup(Alignment.LEADING).addGroup(layout.createSequentialGroup().addComponent(this.listeningPortLabel).addPreferredGap(ComponentPlacement.RELATED).addComponent(this.listeningPortField, -2, 64, -2)).addGroup(layout.createSequentialGroup().addComponent(this.allInterfacesRadio, -2, -1, -2).addPreferredGap(ComponentPlacement.RELATED).addComponent(this.specificInterfaceRadio, -2, -1, -2).addPreferredGap(ComponentPlacement.RELATED).addComponent(this.addressBindField, -2, 176, -2)))).addGroup(layout.createSequentialGroup().addGroup(layout.createParallelGroup(Alignment.LEADING).addComponent(this.clientRadio, -2, -1, -2).addComponent(this.serverRadio, -2, -1, -2).addGroup(layout.createSequentialGroup().addGap(42, 42, 42).addGroup(layout.createParallelGroup(Alignment.LEADING, false).addComponent(this.remoteAddressLabel, -1, -1, 32767).addComponent(this.remotePortLabel, -2, 107, -2)).addPreferredGap(ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(Alignment.LEADING).addComponent(this.remotePortField, -2, 64, -2).addComponent(this.remoteAddressField, -2, 176, -2)))).addGap(133, 133, 133)))).addComponent(this.conectionModeLabel))).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(this.AstmDataStructureLabel).addGap(18, 18, 18).addComponent(this.elecsysRadio, -2, -1, -2).addPreferredGap(ComponentPlacement.UNRELATED).addComponent(this.cobasRadio, -2, -1, -2))).addContainerGap(101, 32767)));
        layout.setVerticalGroup(layout.createParallelGroup(Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(this.conectionModeLabel).addPreferredGap(ComponentPlacement.RELATED).addComponent(this.serverRadio, -2, -1, -2).addPreferredGap(ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(this.addressBindField, -2, -1, -2).addComponent(this.specificInterfaceRadio, -2, -1, -2).addComponent(this.allInterfacesRadio, -2, -1, -2)).addPreferredGap(ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(this.listeningPortLabel).addComponent(this.listeningPortField, -2, -1, -2)).addGap(25, 25, 25).addComponent(this.clientRadio, -2, -1, -2).addGap(11, 11, 11).addGroup(layout.createParallelGroup(Alignment.LEADING).addComponent(this.remoteAddressField, -2, -1, -2).addComponent(this.remoteAddressLabel, -2, 16, -2)).addPreferredGap(ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(this.remotePortLabel).addComponent(this.remotePortField, -2, -1, -2)).addGap(18, 18, 18).addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(this.AstmDataStructureLabel).addComponent(this.elecsysRadio, -2, -1, -2).addComponent(this.cobasRadio, -2, -1, -2)).addContainerGap()));
        this.listeningPortField.getAccessibleContext().setAccessibleName("");
        this.listeningPortField.getAccessibleContext().setAccessibleDescription("<html>The port on which the ASTM server should listen for connections.</html>");
    }

    private void serverRadioActionPerformed(ActionEvent evt) {
        this.setServerMode(true, this.allInterfacesRadio.isSelected());
    }

    private void elecsysRadioActionPerformed(ActionEvent evt) {
    }

    private void clientRadioActionPerformed(ActionEvent evt) {
        this.setServerMode(false, false);
    }

    private void specificInterfaceRadioActionPerformed(ActionEvent evt) {
        this.setAllInterfacesMode(false);
    }

    private void allInterfacesRadioActionPerformed(ActionEvent evt) {
        this.setAllInterfacesMode(true);
    }

    private void ackOnNewConnectionNoActionPerformed(ActionEvent evt) {
    }

    private void ackOnNewConnectionYesActionPerformed(ActionEvent evt) {
    }
}
