package com.bitdreamit.connect.astm;

import com.mirth.connect.client.ui.AbstractSettingsPanel;
import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.util.DonkeyElement;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Bit Dream IT — ASTM Extension Global Settings Panel
 * Professional information panel with dialect guide and feature overview.
 */
public class AstmSettingsPanel extends AbstractSettingsPanel {

    private static final Color BRAND_BLUE = new Color(0x1E, 0x5A, 0xA8);
    private static final Color BRAND_LIGHT = new Color(0xE8, 0xF0, 0xFA);
    private static final Color SECTION_BG = new Color(0xFA, 0xFA, 0xFA);

    public AstmSettingsPanel(boolean isSender) {
        super("ASTM Settings");
        initComponents();
    }

    private void initComponents() {
        setBackground(Color.WHITE);
        setLayout(new MigLayout("insets 12, fillx, gap 8", "[grow]", ""));

        // ========== HEADER / BRANDING ==========
        JPanel headerPanel = new JPanel(new MigLayout("insets 16, gap 8", "[][grow]", ""));
        headerPanel.setBackground(BRAND_BLUE);
        headerPanel.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel lblLogo = new JLabel("\u2695"); // Medical symbol
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 36));
        lblLogo.setForeground(Color.WHITE);

        JLabel lblTitle = new JLabel("<html><b style=\"font-size:18px;\">Bit Dream IT</b><br/><span style=\"font-size:13px;\">ASTM Extension for Mirth Connect / BridgeLink</span></html>");
        lblTitle.setForeground(Color.WHITE);

        JLabel lblVer = new JLabel("v2.4.2");
        lblVer.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblVer.setForeground(new Color(0xCC, 0xDD, 0xFF));

        headerPanel.add(lblLogo, "cell 0 0, spany 2, aligny top");
        headerPanel.add(lblTitle, "cell 1 0");
        headerPanel.add(lblVer, "cell 1 1, gaptop 4");
        add(headerPanel, "growx, wrap");

        // ========== HOW IT WORKS ==========
        add(createSection("How It Works",
                "<html>"
                        + "<p style=\"margin-bottom:8px;\">"
                        + "This extension adds <b>ASTM Listener</b> (Source) and <b>ASTM Sender</b> (Destination) connectors to Mirth Connect. "
                        + "Each channel configures its own transport mode independently — choose <b>TCP Client</b>, <b>TCP Server</b>, or <b>Serial (RS-232)</b> directly in the channel connector settings.</p>"
                        + "<p style=\"margin-bottom:8px;\">"
                        + "<b>Combined Use:</b> The Listener and Sender can share the same ASTM connection to communicate bidirectionally with the device.</p>"
                        + "<p>Configure transport parameters (host, port, serial port, baud rate, etc.) inside each channel. This global panel shows extension info only.</p>"
                        + "</html>"), "growx, wrap");

        // ========== DIALECT GUIDE ==========
        add(createSection("Dialect Selection Guide",
                "<html>"
                        + "<table cellspacing=\"6\" cellpadding=\"0\">"
                        + "<tr><td><b>ELECSYS</b></td><td>—</td><td>Roche / Elecsys analysers. Uses standard ASTM E1381 with ENQ/ACK handshake and checksum.</td></tr>"
                        + "<tr><td><b>COBAS</b></td><td>—</td><td>Roche / Cobas series. Similar to ELECSYS with slight frame timing differences.</td></tr>"
                        + "<tr><td><b>GENERIC</b></td><td>—</td><td>Non-Roche devices or custom implementations. Most compatible fallback option.</td></tr>"
                        + "</table>"
                        + "<p style=\"margin-top:8px;\"><i>Tip: If your device is Snibe, Mindray, or Erba — start with GENERIC, then switch to ELECSYS or COBAS if the device manual specifies it.</i></p>"
                        + "</html>"), "growx, wrap");

        // ========== FEATURES ==========
        add(createSection("Features & Capabilities",
                "<html><ul style=\"margin-left:16px;\">"
                        + "<li><b>New ASTM Connectors:</b> ASTM Listener (Source) and ASTM Sender (Destination)</li>"
                        + "<li><b>Graphical Configuration:</b> Clear per-channel GUI for TCP Client / TCP Server / Serial setup</li>"
                        + "<li><b>Template Support:</b> Outgoing message templates for Destination connectors</li>"
                        + "<li><b>High Performance:</b> Asynchronous message reception and dispatching</li>"
                        + "<li><b>Special Character Encoding:</b> CP-1252 (Windows-1252) and UTF-8 support</li>"
                        + "<li><b>Versatile TCP:</b> Operate as client or server; multiple connections on same port</li>"
                        + "<li><b>Serial Communication:</b> Native RS-232 via jSerialComm (COM ports, /dev/ttyUSB, etc.)</li>"
                        + "<li><b>Protocol Handling:</b> ASTM E1381-91, E1381-95, E1381-02 compliant framing</li>"
                        + "<li><b>Memory Efficient:</b> Optimized for minimal memory consumption</li>"
                        + "<li><b>Robust Error Management:</b> Error detection and reporting for incoming messages</li>"
                        + "</ul></html>"), "growx, wrap");

        // ========== SERIAL NOTE ==========
        JPanel serialNote = new JPanel(new MigLayout("insets 10, gap 8", "[][grow]", ""));
        serialNote.setBackground(new Color(0xFF, 0xFB, 0xE6));
        serialNote.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xF0, 0xC0, 0x40), 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        JLabel lblIcon = new JLabel("\u26A0"); // Warning symbol
        lblIcon.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblIcon.setForeground(new Color(0xC0, 0x80, 0x10));
        JLabel lblSerial = new JLabel("<html><b>Serial Mode Note</b><br/>"
                + "When using RS-232, ensure the <b>jSerialComm</b> library (lib/jSerialComm-2.10.4.jar) is present in the plugin lib folder. "
                + "Set the correct COM port (Windows) or /dev/tty device (Linux) in the channel connector settings.</html>");
        serialNote.add(lblIcon, "aligny top");
        serialNote.add(lblSerial, "growx");
        add(serialNote, "growx, wrap");

        // ========== FOOTER ==========
        JLabel lblFooter = new JLabel("\u00A9 2026 Bit Dream IT  —  https://www.bitdreamit.com");
        lblFooter.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblFooter.setForeground(Color.GRAY);
        lblFooter.setHorizontalAlignment(SwingConstants.CENTER);
        add(lblFooter, "growx, gaptop 10");
    }

    private JPanel createSection(String title, String htmlContent) {
        JPanel panel = new JPanel(new MigLayout("insets 12, gap 6, fillx", "[grow]", ""));
        panel.setBackground(SECTION_BG);
        panel.setBorder(new CompoundBorder(
                new LineBorder(new Color(0xDD, 0xDD, 0xDD), 1),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(BRAND_BLUE);
        panel.add(lblTitle, "wrap, gapbottom 6");

        JLabel lblContent = new JLabel(htmlContent);
        lblContent.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblContent.setForeground(new Color(0x33, 0x33, 0x33));
        panel.add(lblContent, "growx");

        return panel;
    }

    // ===== Required AbstractSettingsPanel methods =====
    public ConnectorProperties getProperties() {
        return new AstmProperties() {
            @Override public void migrate3_0_1(DonkeyElement e) {}
            @Override public void migrate3_0_2(DonkeyElement e) {}
            @Override public void migrate4_4_0(DonkeyElement e) { super.migrate4_4_0(e); }
            @Override public void migrate4_5_0(DonkeyElement e) { super.migrate4_5_0(e); }
        };
    }

    public void setProperties(ConnectorProperties properties) {}
    public ConnectorProperties getDefaults() { return getProperties(); }
    public boolean checkProperties(ConnectorProperties properties, boolean highlight) { return true; }
    public void resetInvalidProperties() {}
    public void doRefresh() {}
    public boolean doSave() { return false; }
}