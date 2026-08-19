package com.bitdreamit.connect.astm;

import com.mirth.connect.client.ui.components.MirthButton;
import com.mirth.connect.client.ui.components.MirthCheckBox;
import com.mirth.connect.client.ui.components.MirthComboBox;
import com.mirth.connect.client.ui.components.MirthRadioButton;
import com.mirth.connect.client.ui.components.MirthTextArea;
import com.mirth.connect.client.ui.components.MirthTextField;
import com.mirth.connect.client.ui.panels.connectors.ConnectorSettingsPanel;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * ASTM Sender — Destination connector settings panel.
 *
 * FIX (Bug #17 — Restore "All interfaces" / "Specific interface" option):
 * Restored radio buttons for TCP Server mode binding.
 *
 * FIX (Bug #18 — Use Mirth UI components so changes are saved):
 * Replaced standard javax.swing components with Mirth equivalents.
 *
 * NOTE on MirthComboBox: only has a no-arg constructor. Use addItem() to populate.
 * NOTE on MirthTextArea: only has a no-arg constructor. Use setRows/setColumns.
 */
public class AstmSender extends ConnectorSettingsPanel implements ActionListener {

    private MirthComboBox<String> modeBox;

    private JPanel tcpPanel;
    private MirthTextField hostField;
    private MirthTextField portField;
    private MirthCheckBox serverModeBox;
    private MirthTextField connTimeoutField;
    private MirthRadioButton allInterfacesRadio;
    private MirthRadioButton specificInterfaceRadio;
    private ButtonGroup interfaceGroup;
    private MirthTextField bindAddressField;

    private JPanel serialPanel;
    private MirthComboBox<String> serialPortBox;
    private MirthButton refreshPortsBtn;
    private MirthComboBox<String> baudBox;
    private MirthComboBox<String> dataBitsBox;
    private MirthComboBox<String> stopBitsBox;
    private MirthComboBox<String> parityBox;
    private MirthComboBox<String> flowBox;
    private MirthComboBox<String> charsetBox;
    private MirthTextField readTimeoutField;
    private MirthTextField writeTimeoutField;

    private JPanel protocolPanel;
    private MirthComboBox<String> protocolBox;
    private MirthCheckBox enqAckBox;
    private MirthCheckBox checksumBox;
    private MirthTextField maxRetriesField;
    private MirthTextField frameSizeField;
    private MirthTextField interFrameDelayField;

    private JPanel templatePanel;
    private MirthTextArea templateArea;
    private MirthTextField sendTimeoutField;

    public AstmSender() {
        initComponents();
        refreshPortList();
        updateVisibility();
    }

    @Override
    public String getConnectorName() {
        return "ASTM Sender";
    }

    private void initComponents() {
        setBackground(Color.WHITE);
        setLayout(new MigLayout("insets 8, novisualpadding, hidemode 3, fillx, gap 4", "[][grow]", ""));

        add(new JLabel("Transport Mode:"), "right");
        modeBox = new MirthComboBox<>();
        modeBox.addItem("TCP Client");
        modeBox.addItem("TCP Server");
        modeBox.addItem("Serial (RS-232)");
        modeBox.addActionListener(this);
        add(modeBox, "w 200!, wrap");

        // ============ TCP PANEL ============
        tcpPanel = new JPanel(new MigLayout("insets 8, gap 4", "[][grow]", ""));
        tcpPanel.setBackground(Color.WHITE);
        tcpPanel.setBorder(new TitledBorder("TCP Settings"));
        hostField = new MirthTextField();
        portField = new MirthTextField();
        serverModeBox = new MirthCheckBox("Server Mode (Listen)");
        serverModeBox.setBackground(Color.WHITE);
        serverModeBox.addActionListener(this);
        connTimeoutField = new MirthTextField();

        allInterfacesRadio = new MirthRadioButton("All interfaces");
        allInterfacesRadio.setBackground(Color.WHITE);
        allInterfacesRadio.setToolTipText("<html>If selected, the connector will listen on all interfaces, using address 0.0.0.0.</html>");
        allInterfacesRadio.addActionListener(this);
        specificInterfaceRadio = new MirthRadioButton("Specific interface:");
        specificInterfaceRadio.setBackground(Color.WHITE);
        specificInterfaceRadio.setToolTipText("<html>If selected, the connector will listen only on the specific interface address defined.</html>");
        specificInterfaceRadio.addActionListener(this);
        interfaceGroup = new ButtonGroup();
        interfaceGroup.add(allInterfacesRadio);
        interfaceGroup.add(specificInterfaceRadio);
        allInterfacesRadio.setSelected(true);
        bindAddressField = new MirthTextField();
        bindAddressField.setToolTipText("<html>IP address of the network interface to bind to (e.g., 192.168.1.10).<br>Only used when 'Specific interface' is selected.</html>");
        bindAddressField.setEnabled(false);

        tcpPanel.add(new JLabel("Host:"), "right");
        tcpPanel.add(hostField, "w 200!, wrap");
        tcpPanel.add(new JLabel("Port:"), "right");
        tcpPanel.add(portField, "w 100!, wrap");
        tcpPanel.add(serverModeBox, "span 2, wrap");
        tcpPanel.add(new JLabel("Listen on:"), "right");
        tcpPanel.add(allInterfacesRadio, "split 3");
        tcpPanel.add(specificInterfaceRadio, "");
        tcpPanel.add(bindAddressField, "w 150!, wrap");
        tcpPanel.add(new JLabel("Conn Timeout (ms):"), "right");
        tcpPanel.add(connTimeoutField, "w 100!, wrap");
        add(tcpPanel, "span, growx, wrap");

        // ============ SERIAL PANEL ============
        serialPanel = new JPanel(new MigLayout("insets 8, gap 4", "[][grow]", ""));
        serialPanel.setBackground(Color.WHITE);
        serialPanel.setBorder(new TitledBorder("Serial Settings"));
        serialPortBox = new MirthComboBox<>();
        serialPortBox.setEditable(true);
        refreshPortsBtn = new MirthButton("Refresh");
        refreshPortsBtn.addActionListener(this);
        baudBox = new MirthComboBox<>();
        for (String s : new String[]{"9600", "19200", "38400", "57600", "115200"}) baudBox.addItem(s);
        dataBitsBox = new MirthComboBox<>();
        for (String s : new String[]{"5", "6", "7", "8"}) dataBitsBox.addItem(s);
        stopBitsBox = new MirthComboBox<>();
        for (String s : new String[]{"1", "1.5", "2"}) stopBitsBox.addItem(s);
        parityBox = new MirthComboBox<>();
        for (String s : new String[]{"None", "Odd", "Even", "Mark", "Space"}) parityBox.addItem(s);
        flowBox = new MirthComboBox<>();
        for (String s : new String[]{"None", "RTS/CTS", "XON/XOFF", "DSR/DTR"}) flowBox.addItem(s);
        charsetBox = new MirthComboBox<>();
        for (String s : new String[]{"UTF-8", "ISO-8859-1", "US-ASCII", "windows-1252"}) charsetBox.addItem(s);
        readTimeoutField = new MirthTextField();
        writeTimeoutField = new MirthTextField();
        serialPanel.add(new JLabel("Port:"), "right");
        serialPanel.add(serialPortBox, "split 2, w 180!");
        serialPanel.add(refreshPortsBtn, "w 80!, wrap");
        serialPanel.add(new JLabel("Baud:"), "right");
        serialPanel.add(baudBox, "w 120!, wrap");
        serialPanel.add(new JLabel("Data Bits:"), "right");
        serialPanel.add(dataBitsBox, "w 80!, wrap");
        serialPanel.add(new JLabel("Stop Bits:"), "right");
        serialPanel.add(stopBitsBox, "w 80!, wrap");
        serialPanel.add(new JLabel("Parity:"), "right");
        serialPanel.add(parityBox, "w 100!, wrap");
        serialPanel.add(new JLabel("Flow Ctrl:"), "right");
        serialPanel.add(flowBox, "w 120!, wrap");
        serialPanel.add(new JLabel("Charset:"), "right");
        serialPanel.add(charsetBox, "w 120!, wrap");
        serialPanel.add(new JLabel("Read T/O (ms):"), "right");
        serialPanel.add(readTimeoutField, "w 100!, wrap");
        serialPanel.add(new JLabel("Write T/O (ms):"), "right");
        serialPanel.add(writeTimeoutField, "w 100!, wrap");
        add(serialPanel, "span, growx, wrap");

        // ============ PROTOCOL PANEL ============
        protocolPanel = new JPanel(new MigLayout("insets 8, gap 4", "[][grow]", ""));
        protocolPanel.setBackground(Color.WHITE);
        protocolPanel.setBorder(new TitledBorder("ASTM Protocol"));
        protocolBox = new MirthComboBox<>();
        for (String s : new String[]{"ELECSYS", "COBAS", "GENERIC"}) protocolBox.addItem(s);
        enqAckBox = new MirthCheckBox("Use ENQ/ACK Handshake");
        enqAckBox.setSelected(true);
        enqAckBox.setBackground(Color.WHITE);
        checksumBox = new MirthCheckBox("Use Checksum Validation");
        checksumBox.setSelected(true);
        checksumBox.setBackground(Color.WHITE);
        maxRetriesField = new MirthTextField();
        frameSizeField = new MirthTextField();
        interFrameDelayField = new MirthTextField();
        protocolPanel.add(new JLabel("Dialect:"), "right");
        protocolPanel.add(protocolBox, "w 150!, wrap");
        protocolPanel.add(enqAckBox, "span 2, wrap");
        protocolPanel.add(checksumBox, "span 2, wrap");
        protocolPanel.add(new JLabel("Max Retries:"), "right");
        protocolPanel.add(maxRetriesField, "w 100!, wrap");
        protocolPanel.add(new JLabel("Frame Size:"), "right");
        protocolPanel.add(frameSizeField, "w 100!, wrap");
        protocolPanel.add(new JLabel("Inter-frame (ms):"), "right");
        protocolPanel.add(interFrameDelayField, "w 100!, wrap");
        add(protocolPanel, "span, growx, wrap");

        // ============ TEMPLATE PANEL ============
        templatePanel = new JPanel(new MigLayout("insets 8, gap 4", "[][grow]", ""));
        templatePanel.setBackground(Color.WHITE);
        templatePanel.setBorder(new TitledBorder("Message Template"));
        // FIX: MirthTextArea only has a no-arg constructor — set rows/columns via setters
        templateArea = new MirthTextArea();
        templateArea.setRows(5);
        templateArea.setColumns(40);
        templateArea.setLineWrap(true);
        sendTimeoutField = new MirthTextField();
        templatePanel.add(new JLabel("Template:"), "right, top");
        templatePanel.add(new JScrollPane(templateArea), "growx, wrap");
        templatePanel.add(new JLabel("Send Timeout (ms):"), "right");
        templatePanel.add(sendTimeoutField, "w 100!, wrap");
        add(templatePanel, "span, growx, wrap");
    }

    private void refreshPortList() {
        serialPortBox.removeAllItems();
        serialPortBox.addItem("");

        boolean dynamicSuccess = false;
        try {
            Class<?> serialPortClass = Class.forName("com.fazecast.jSerialComm.SerialPort");
            Object[] ports = (Object[]) serialPortClass.getMethod("getCommPorts").invoke(null);
            for (Object port : ports) {
                String name = (String) port.getClass().getMethod("getSystemPortName").invoke(port);
                String desc = (String) port.getClass().getMethod("getDescriptivePortName").invoke(port);
                serialPortBox.addItem(name + " - " + desc);
            }
            dynamicSuccess = ports.length > 0;
        } catch (Throwable t) {
            // jSerialComm not available on client classpath
        }

        if (!dynamicSuccess) {
            String[] defaults = {"COM1","COM2","COM3","COM4","COM5","COM6","COM7","COM8",
                    "/dev/ttyS0","/dev/ttyS1","/dev/ttyUSB0","/dev/ttyUSB1","/dev/ttyACM0"};
            for (String p : defaults) serialPortBox.addItem(p);
        }
    }

    private void updateVisibility() {
        int mode = modeBox.getSelectedIndex();
        tcpPanel.setVisible(mode == 0 || mode == 1);
        serialPanel.setVisible(mode == 2);
        boolean isServer = (mode == 1) || (mode == 0 && serverModeBox.isSelected());
        allInterfacesRadio.setVisible(isServer);
        specificInterfaceRadio.setVisible(isServer);
        bindAddressField.setEnabled(isServer && specificInterfaceRadio.isSelected());
        revalidate(); repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == modeBox) {
            updateVisibility();
        } else if (e.getSource() == serverModeBox) {
            updateVisibility();
        } else if (e.getSource() == allInterfacesRadio) {
            bindAddressField.setEnabled(false);
        } else if (e.getSource() == specificInterfaceRadio) {
            bindAddressField.setEnabled(true);
            bindAddressField.requestFocusInWindow();
        } else if (e.getSource() == refreshPortsBtn) {
            refreshPortList();
        }
    }

    private void readFromUI(AstmProperties p) {
        int mode = modeBox.getSelectedIndex();
        p.setTransportMode(mode == 0 ? AstmProperties.TransportMode.TCP_CLIENT :
                mode == 1 ? AstmProperties.TransportMode.TCP_SERVER :
                        AstmProperties.TransportMode.SERIAL);
        if (mode == 1) {
            p.setServerMode(true);
            if (allInterfacesRadio.isSelected()) {
                p.setAllInterfaces(true);
                p.setAddressBind("0.0.0.0");
                p.setHost("0.0.0.0");
            } else {
                p.setAllInterfaces(false);
                String bind = bindAddressField.getText().trim();
                if (bind.isEmpty()) bind = "0.0.0.0";
                p.setAddressBind(bind);
                p.setHost(bind);
            }
        } else if (mode == 0) {
            p.setServerMode(false);
            p.setHost(hostField.getText());
        } else {
            p.setHost(hostField.getText());
        }
        try { p.setPort(Integer.parseInt(portField.getText())); } catch (Exception ignored) {}
        p.setServerMode(serverModeBox.isSelected());
        try { p.setConnectionTimeout(Integer.parseInt(connTimeoutField.getText())); } catch (Exception ignored) {}
        String portItem = serialPortBox.getSelectedItem() != null ? serialPortBox.getSelectedItem().toString() : "";
        if (portItem.contains(" - ")) portItem = portItem.substring(0, portItem.indexOf(" - "));
        p.setSerialPort(portItem);
        try { p.setBaudRate(Integer.parseInt((String) baudBox.getSelectedItem())); } catch (Exception ignored) {}
        try { p.setDataBits(Integer.parseInt((String) dataBitsBox.getSelectedItem())); } catch (Exception ignored) {}
        p.setStopBits(stopBitsBox.getSelectedIndex() + 1);
        p.setParity(parityBox.getSelectedIndex());
        p.setFlowControl(flowBox.getSelectedIndex());
        p.setCharsetName((String) charsetBox.getSelectedItem());
        try { p.setReadTimeout(Integer.parseInt(readTimeoutField.getText())); } catch (Exception ignored) {}
        try { p.setWriteTimeout(Integer.parseInt(writeTimeoutField.getText())); } catch (Exception ignored) {}
        p.setAstmProtocol((String) protocolBox.getSelectedItem());
        p.setUseEnqAck(enqAckBox.isSelected());
        p.setUseChecksum(checksumBox.isSelected());
        try { p.setMaxRetries(Integer.parseInt(maxRetriesField.getText())); } catch (Exception ignored) {}
        try { p.setMaxFrameSize(Integer.parseInt(frameSizeField.getText())); } catch (Exception ignored) {}
        try { p.setInterFrameDelay(Integer.parseInt(interFrameDelayField.getText())); } catch (Exception ignored) {}
    }

    private void writeToUI(AstmProperties p) {
        switch (p.getTransportMode()) {
            case TCP_CLIENT: modeBox.setSelectedIndex(0); break;
            case TCP_SERVER: modeBox.setSelectedIndex(1); break;
            case SERIAL: modeBox.setSelectedIndex(2); break;
        }
        hostField.setText(p.getHost());
        portField.setText(String.valueOf(p.getPort()));
        serverModeBox.setSelected(p.isServerMode());
        if (p.isAllInterfaces()) {
            allInterfacesRadio.setSelected(true);
            bindAddressField.setEnabled(false);
        } else {
            specificInterfaceRadio.setSelected(true);
            bindAddressField.setEnabled(true);
        }
        bindAddressField.setText(p.getAddressBind());
        connTimeoutField.setText(String.valueOf(p.getConnectionTimeout()));
        serialPortBox.setSelectedItem(p.getSerialPort());
        baudBox.setSelectedItem(String.valueOf(p.getBaudRate()));
        dataBitsBox.setSelectedItem(String.valueOf(p.getDataBits()));
        stopBitsBox.setSelectedIndex(Math.max(0, p.getStopBits() - 1));
        parityBox.setSelectedIndex(p.getParity());
        flowBox.setSelectedIndex(p.getFlowControl());
        charsetBox.setSelectedItem(p.getCharsetName());
        readTimeoutField.setText(String.valueOf(p.getReadTimeout()));
        writeTimeoutField.setText(String.valueOf(p.getWriteTimeout()));
        protocolBox.setSelectedItem(p.getAstmProtocol());
        enqAckBox.setSelected(p.isUseEnqAck());
        checksumBox.setSelected(p.isUseChecksum());
        maxRetriesField.setText(String.valueOf(p.getMaxRetries()));
        frameSizeField.setText(String.valueOf(p.getMaxFrameSize()));
        interFrameDelayField.setText(String.valueOf(p.getInterFrameDelay()));
        updateVisibility();
    }

    @Override
    public ConnectorProperties getProperties() {
        AstmDispatcherProperties p = new AstmDispatcherProperties();
        readFromUI(p);
        p.setTemplate(templateArea.getText());
        p.setSendTimeout(sendTimeoutField.getText());
        return p;
    }

    @Override
    public void setProperties(ConnectorProperties properties) {
        if (properties instanceof AstmDispatcherProperties) {
            AstmDispatcherProperties p = (AstmDispatcherProperties) properties;
            writeToUI(p);
            templateArea.setText(p.getTemplate());
            sendTimeoutField.setText(p.getSendTimeout());
        }
    }

    @Override
    public ConnectorProperties getDefaults() {
        AstmDispatcherProperties p = new AstmDispatcherProperties();
        p.setTemplate("${message.encodedData}");
        p.setSendTimeout("20000");
        return p;
    }

    @Override
    public boolean checkProperties(ConnectorProperties properties, boolean highlight) {
        return true;
    }

    @Override
    public void resetInvalidProperties() {}
}
