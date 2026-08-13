package com.bitdreamit.connect.astm;

import com.mirth.connect.client.ui.panels.connectors.ConnectorSettingsPanel;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AstmListener extends ConnectorSettingsPanel implements ActionListener {

    private JLabel modeLabel;
    private JComboBox<String> modeBox;

    private JPanel tcpPanel;
    private JTextField hostField;
    private JTextField portField;
    private JCheckBox serverModeBox;
    private JTextField connTimeoutField;

    private JPanel serialPanel;
    private JComboBox<String> serialPortBox;
    private JButton refreshPortsBtn;
    private JComboBox<String> baudBox;
    private JComboBox<String> dataBitsBox;
    private JComboBox<String> stopBitsBox;
    private JComboBox<String> parityBox;
    private JComboBox<String> flowBox;
    private JComboBox<String> charsetBox;
    private JTextField readTimeoutField;
    private JTextField writeTimeoutField;

    private JPanel protocolPanel;
    private JComboBox<String> protocolBox;
    private JCheckBox enqAckBox;
    private JCheckBox checksumBox;
    private JTextField maxRetriesField;
    private JTextField frameSizeField;
    private JTextField interFrameDelayField;

    public AstmListener() {
        initComponents();
        refreshPortList();
        updateVisibility();
    }

    @Override
    public String getConnectorName() {
        return "ASTM Listener";
    }

    private void initComponents() {
        setBackground(Color.WHITE);
        setLayout(new MigLayout("insets 8, novisualpadding, hidemode 3, fillx, gap 4", "[][grow]", ""));

        modeLabel = new JLabel("Transport Mode:");
        modeBox = new JComboBox<>(new String[]{"TCP Client", "TCP Server", "Serial (RS-232)"});
        modeBox.addActionListener(this);
        add(modeLabel, "right");
        add(modeBox, "w 200!, wrap");

        tcpPanel = new JPanel(new MigLayout("insets 8, gap 4", "[][grow]", ""));
        tcpPanel.setBackground(Color.WHITE);
        tcpPanel.setBorder(new TitledBorder("TCP Settings"));
        hostField = new JTextField();
        portField = new JTextField();
        serverModeBox = new JCheckBox("Server Mode (Listen)");
        serverModeBox.setBackground(Color.WHITE);
        connTimeoutField = new JTextField();
        tcpPanel.add(new JLabel("Host:"), "right");
        tcpPanel.add(hostField, "w 200!, wrap");
        tcpPanel.add(new JLabel("Port:"), "right");
        tcpPanel.add(portField, "w 100!, wrap");
        tcpPanel.add(serverModeBox, "span 2, wrap");
        tcpPanel.add(new JLabel("Conn Timeout (ms):"), "right");
        tcpPanel.add(connTimeoutField, "w 100!, wrap");
        add(tcpPanel, "span, growx, wrap");

        serialPanel = new JPanel(new MigLayout("insets 8, gap 4", "[][grow]", ""));
        serialPanel.setBackground(Color.WHITE);
        serialPanel.setBorder(new TitledBorder("Serial Settings"));
        serialPortBox = new JComboBox<>();
        serialPortBox.setEditable(true);
        refreshPortsBtn = new JButton("Refresh");
        refreshPortsBtn.addActionListener(this);
        baudBox = new JComboBox<>(new String[]{"9600", "19200", "38400", "57600", "115200"});
        dataBitsBox = new JComboBox<>(new String[]{"5", "6", "7", "8"});
        stopBitsBox = new JComboBox<>(new String[]{"1", "1.5", "2"});
        parityBox = new JComboBox<>(new String[]{"None", "Odd", "Even", "Mark", "Space"});
        flowBox = new JComboBox<>(new String[]{"None", "RTS/CTS", "XON/XOFF", "DSR/DTR"});
        charsetBox = new JComboBox<>(new String[]{"UTF-8", "ISO-8859-1", "US-ASCII", "windows-1252"});
        readTimeoutField = new JTextField();
        writeTimeoutField = new JTextField();
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

        protocolPanel = new JPanel(new MigLayout("insets 8, gap 4", "[][grow]", ""));
        protocolPanel.setBackground(Color.WHITE);
        protocolPanel.setBorder(new TitledBorder("ASTM Protocol"));
        protocolBox = new JComboBox<>(new String[]{"ELECSYS", "COBAS", "GENERIC"});
        enqAckBox = new JCheckBox("Use ENQ/ACK Handshake");
        enqAckBox.setSelected(true);
        enqAckBox.setBackground(Color.WHITE);
        checksumBox = new JCheckBox("Use Checksum Validation");
        checksumBox.setSelected(true);
        checksumBox.setBackground(Color.WHITE);
        maxRetriesField = new JTextField();
        frameSizeField = new JTextField();
        interFrameDelayField = new JTextField();
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
    }

    /**
     * IMPROVED: Uses reflection to dynamically enumerate serial ports via jSerialComm if available.
     * Falls back to a hardcoded list if jSerialComm is not on the client classpath.
     * This prevents NoClassDefFoundError crashes in the Mirth Administrator.
     */
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
            // jSerialComm not available on client classpath — expected in Mirth Administrator
        }

        if (!dynamicSuccess) {
            String[] defaults = {"COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8",
                    "/dev/ttyS0", "/dev/ttyS1", "/dev/ttyUSB0", "/dev/ttyUSB1", "/dev/ttyACM0"};
            for (String p : defaults) serialPortBox.addItem(p);
        }
    }

    private void updateVisibility() {
        int mode = modeBox.getSelectedIndex();
        tcpPanel.setVisible(mode == 0 || mode == 1);
        serialPanel.setVisible(mode == 2);
        revalidate();
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == modeBox) updateVisibility();
        else if (e.getSource() == refreshPortsBtn) refreshPortList();
    }

    private void readFromUI(AstmProperties p) {
        int mode = modeBox.getSelectedIndex();
        p.setTransportMode(mode == 0 ? AstmProperties.TransportMode.TCP_CLIENT :
                mode == 1 ? AstmProperties.TransportMode.TCP_SERVER :
                        AstmProperties.TransportMode.SERIAL);
        p.setHost(hostField.getText());
        try {
            p.setPort(Integer.parseInt(portField.getText()));
        } catch (Exception ignored) {
        }
        p.setServerMode(serverModeBox.isSelected());
        try {
            p.setConnectionTimeout(Integer.parseInt(connTimeoutField.getText()));
        } catch (Exception ignored) {
        }
        // Parse port name from "COM1 - Description" format if present
        String portItem = serialPortBox.getSelectedItem() != null ? serialPortBox.getSelectedItem().toString() : "";
        if (portItem.contains(" - ")) portItem = portItem.substring(0, portItem.indexOf(" - "));
        p.setSerialPort(portItem);
        try {
            p.setBaudRate(Integer.parseInt((String) baudBox.getSelectedItem()));
        } catch (Exception ignored) {
        }
        try {
            p.setDataBits(Integer.parseInt((String) dataBitsBox.getSelectedItem()));
        } catch (Exception ignored) {
        }
        p.setStopBits(stopBitsBox.getSelectedIndex() + 1);
        p.setParity(parityBox.getSelectedIndex());
        p.setFlowControl(flowBox.getSelectedIndex());
        p.setCharsetName((String) charsetBox.getSelectedItem());
        try {
            p.setReadTimeout(Integer.parseInt(readTimeoutField.getText()));
        } catch (Exception ignored) {
        }
        try {
            p.setWriteTimeout(Integer.parseInt(writeTimeoutField.getText()));
        } catch (Exception ignored) {
        }
        p.setAstmProtocol((String) protocolBox.getSelectedItem());
        p.setUseEnqAck(enqAckBox.isSelected());
        p.setUseChecksum(checksumBox.isSelected());
        try {
            p.setMaxRetries(Integer.parseInt(maxRetriesField.getText()));
        } catch (Exception ignored) {
        }
        try {
            p.setMaxFrameSize(Integer.parseInt(frameSizeField.getText()));
        } catch (Exception ignored) {
        }
        try {
            p.setInterFrameDelay(Integer.parseInt(interFrameDelayField.getText()));
        } catch (Exception ignored) {
        }
    }

    private void writeToUI(AstmProperties p) {
        switch (p.getTransportMode()) {
            case TCP_CLIENT:
                modeBox.setSelectedIndex(0);
                break;
            case TCP_SERVER:
                modeBox.setSelectedIndex(1);
                break;
            case SERIAL:
                modeBox.setSelectedIndex(2);
                break;
        }
        hostField.setText(p.getHost());
        portField.setText(String.valueOf(p.getPort()));
        serverModeBox.setSelected(p.isServerMode());
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
        AstmReceiverProperties p = new AstmReceiverProperties();
        readFromUI(p);
        return p;
    }

    @Override
    public void setProperties(ConnectorProperties properties) {
        if (properties instanceof AstmReceiverProperties) {
            writeToUI((AstmReceiverProperties) properties);
        }
    }

    @Override
    public ConnectorProperties getDefaults() {
        return new AstmReceiverProperties();
    }

    @Override
    public boolean checkProperties(ConnectorProperties properties, boolean highlight) {
        return true;
    }

    @Override
    public void resetInvalidProperties() {
    }
}
