package com.bitdreamit.connect.astm;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.channel.DestinationConnectorProperties;
import com.mirth.connect.donkey.model.channel.SourceConnectorProperties;
import com.mirth.connect.donkey.util.DonkeyElement;
import com.mirth.connect.donkey.util.purge.PurgeUtil;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Map;
import java.util.Objects;

@XStreamAlias("astmProperties")
public abstract class AstmProperties extends ConnectorProperties {
    private static final long serialVersionUID = 1L;

    // ========== TRANSPORT MODE ==========
    public enum TransportMode { TCP_CLIENT, TCP_SERVER, SERIAL }

    // FIX (Bug #8): Default transportMode was TCP_CLIENT. The v2.4.2 default
    // behavior was serverMode=true (TCP server). Existing channels upgrading
    // from v2.4.2 expect to be TCP servers by default. Changed to TCP_SERVER.
    private TransportMode transportMode = TransportMode.TCP_SERVER;

    // ========== OLD TCP FIELDS (BACKWARD COMPATIBILITY — used by migration only) ==========
    protected boolean serverMode = true;
    protected boolean allInterfaces = true;
    protected String addressBind = "0.0.0.0";
    protected String localPort = "3600";
    protected String remoteAddress = "127.0.0.1";
    protected String remotePort = "3600";

    // ========== NEW TCP FIELDS ==========
    // FIX (Bug #8): Default port was 5004 (matching Mirth's default HL7 port),
    // but v2.4.2 used 3600. Restored to 3600 to match user expectations.
    // FIX (Bug #8): Default host was 127.0.0.1. For TCP_SERVER mode this is the
    // bind address — 0.0.0.0 (all interfaces) is the safer default and matches
    // v2.4.2's allInterfaces=true default.
    private String host = "0.0.0.0";
    private int port = 3600;
    private int connectionTimeout = 30000;

    // ========== SERIAL FIELDS ==========
    private String serialPort = "COM1";
    private int baudRate = 9600;
    private int dataBits = 8;
    private int stopBits = 1;
    private int parity = 0;
    private int flowControl = 0;
    private String charsetName = "UTF-8";
    private int readTimeout = 5000;
    private int writeTimeout = 5000;

    // ========== PROTOCOL SETTINGS ==========
    protected String astmProtocol = "ELECSYS";
    protected String protocol = "ASTM";
    private boolean useEnqAck = true;
    private boolean useChecksum = true;
    private int maxRetries = 3;
    private int maxFrameSize = 240;
    private int interFrameDelay = 100;

    // ========== CONNECTOR PROPERTIES ==========
    protected SourceConnectorProperties sourceConnectorProperties;
    protected DestinationConnectorProperties destinationConnectorProperties;

    // ========== CONSTRUCTORS ==========
    public AstmProperties() {
        super();
    }

    public AstmProperties(AstmProperties props) {
        super(props);
        if (props != null) {
            this.serverMode = props.serverMode;
            this.allInterfaces = props.allInterfaces;
            this.addressBind = props.addressBind;
            this.localPort = props.localPort;
            this.remoteAddress = props.remoteAddress;
            this.remotePort = props.remotePort;
            this.astmProtocol = props.astmProtocol;
            this.protocol = props.protocol;
            this.sourceConnectorProperties = props.sourceConnectorProperties;
            this.destinationConnectorProperties = props.destinationConnectorProperties;
            this.transportMode = props.transportMode;
            this.host = props.host;
            this.port = props.port;
            this.connectionTimeout = props.connectionTimeout;
            this.serialPort = props.serialPort;
            this.baudRate = props.baudRate;
            this.dataBits = props.dataBits;
            this.stopBits = props.stopBits;
            this.parity = props.parity;
            this.flowControl = props.flowControl;
            this.charsetName = props.charsetName;
            this.readTimeout = props.readTimeout;
            this.writeTimeout = props.writeTimeout;
            this.useEnqAck = props.useEnqAck;
            this.useChecksum = props.useChecksum;
            this.maxRetries = props.maxRetries;
            this.maxFrameSize = props.maxFrameSize;
            this.interFrameDelay = props.interFrameDelay;
        }
    }

    // ========== GETTERS & SETTERS — OLD ==========
    public boolean isServerMode() { return this.serverMode; }
    public void setServerMode(boolean serverMode) { this.serverMode = serverMode; }

    public boolean isAllInterfaces() { return this.allInterfaces; }
    public void setAllInterfaces(boolean allInterfaces) { this.allInterfaces = allInterfaces; }

    public String getAddressBind() { return this.addressBind; }
    public void setAddressBind(String addressBind) { this.addressBind = addressBind; }

    public String getLocalPort() { return this.localPort; }
    public void setLocalPort(String localPort) { this.localPort = localPort; }

    public String getRemoteAddress() { return this.remoteAddress; }
    public void setRemoteAddress(String remoteAddress) { this.remoteAddress = remoteAddress; }

    public String getRemotePort() { return this.remotePort; }
    public void setRemotePort(String remotePort) { this.remotePort = remotePort; }

    public void setProtocol(String protocol) { this.protocol = protocol; }
    public String getAstmProtocol() { return this.astmProtocol; }
    public void setAstmProtocol(String astmProtocol) { this.astmProtocol = astmProtocol; }

    // ========== GETTERS & SETTERS — NEW ==========
    public TransportMode getTransportMode() { return transportMode; }
    public void setTransportMode(TransportMode transportMode) { this.transportMode = transportMode; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }

    public String getSerialPort() { return serialPort; }
    public void setSerialPort(String serialPort) { this.serialPort = serialPort; }
    public int getBaudRate() { return baudRate; }
    public void setBaudRate(int baudRate) { this.baudRate = baudRate; }
    public int getDataBits() { return dataBits; }
    public void setDataBits(int dataBits) { this.dataBits = dataBits; }
    public int getStopBits() { return stopBits; }
    public void setStopBits(int stopBits) { this.stopBits = stopBits; }
    public int getParity() { return parity; }
    public void setParity(int parity) { this.parity = parity; }
    public int getFlowControl() { return flowControl; }
    public void setFlowControl(int flowControl) { this.flowControl = flowControl; }
    public String getCharsetName() { return charsetName; }
    public void setCharsetName(String charsetName) { this.charsetName = charsetName; }
    public int getReadTimeout() { return readTimeout; }
    public void setReadTimeout(int readTimeout) { this.readTimeout = readTimeout; }
    public int getWriteTimeout() { return writeTimeout; }
    public void setWriteTimeout(int writeTimeout) { this.writeTimeout = writeTimeout; }

    public boolean isUseEnqAck() { return useEnqAck; }
    public void setUseEnqAck(boolean useEnqAck) { this.useEnqAck = useEnqAck; }
    public boolean isUseChecksum() { return useChecksum; }
    public void setUseChecksum(boolean useChecksum) { this.useChecksum = useChecksum; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public int getMaxFrameSize() { return maxFrameSize; }
    public void setMaxFrameSize(int maxFrameSize) { this.maxFrameSize = maxFrameSize; }
    public int getInterFrameDelay() { return interFrameDelay; }
    public void setInterFrameDelay(int interFrameDelay) { this.interFrameDelay = interFrameDelay; }

    // ========== Mirth REQUIRED METHODS ==========
    @Override
    public String getProtocol() { return "ASTM"; }

    @Override
    public String getName() { return "ASTM (TCP/Serial)"; }

    @Override
    public String toFormattedString() {
        if (transportMode == TransportMode.SERIAL) {
            return "ASTM Serial [" + serialPort + " @ " + baudRate + "]";
        } else if (transportMode == TransportMode.TCP_SERVER) {
            return "ASTM TCP Server [" + host + ":" + port + "]";
        } else {
            return "ASTM TCP Client [" + host + ":" + port + "]";
        }
    }

    @Override
    public Map<String, Object> getPurgedProperties() {
        Map<String, Object> purgedProperties = super.getPurgedProperties();
        if (sourceConnectorProperties != null) {
            purgedProperties.put("sourceConnectorProperties", sourceConnectorProperties.getPurgedProperties());
        }
        purgedProperties.put("serverMode", this.serverMode);
        purgedProperties.put("allInterfaces", this.allInterfaces);
        purgedProperties.put("addressBind", this.addressBind);
        purgedProperties.put("localPort", PurgeUtil.getNumericValue(this.localPort));
        purgedProperties.put("remoteAddress", this.remoteAddress);
        purgedProperties.put("remotePort", PurgeUtil.getNumericValue(this.remotePort));
        purgedProperties.put("transportMode", this.transportMode.name());
        purgedProperties.put("host", this.host);
        purgedProperties.put("port", this.port);
        purgedProperties.put("serialPort", this.serialPort);
        purgedProperties.put("baudRate", this.baudRate);
        purgedProperties.put("charsetName", this.charsetName);
        purgedProperties.put("astmProtocol", this.astmProtocol);
        return purgedProperties;
    }

    // ========== MIGRATION METHODS ==========
    public void migrate3_0_1(DonkeyElement element) {}
    public void migrate3_0_2(DonkeyElement element) {}
    public void migrate3_1_0(DonkeyElement element) { super.migrate3_1_0(element); }
    public void migrate3_2_0(DonkeyElement element) {}
    public void migrate3_3_0(DonkeyElement element) {}
    public void migrate3_4_0(DonkeyElement element) {}
    public void migrate3_5_0(DonkeyElement element) {}
    public void migrate3_6_0(DonkeyElement element) {}
    public void migrate3_7_0(DonkeyElement element) {}

    /**
     * FIX (Bug #1): v2.4.2 channels store TCP config as serverMode/localPort/
     * remoteAddress/remotePort. v3.0.2 introduced transportMode/host/port.
     * Without this migration, every existing channel silently resets to
     * TCP_CLIENT mode pointing at the default host/port — completely wrong.
     *
     * This method reads the old XML elements (if present) and writes the new
     * ones (if missing). Idempotent — channels already on v3.0.2 format are
     * not affected.
     */
    public void migrate4_4_0(DonkeyElement element) {
        super.migrate4_4_0(element);
    }

    public void migrate4_5_0(DonkeyElement element) {
        super.migrate4_5_0(element);
        migrateV2ToV3Fields(element);
    }

    /**
     * Core migration logic — also called from migrate4_4_0 to be safe.
     * Reads v2.4.2 fields and writes v3.0.2 fields if missing.
     *
     * IMPORTANT: This uses the standard org.w3c.dom.Element API (via
     * DonkeyElement.getElement()) rather than DonkeyElement's own helper
     * methods (getChild, getStringValue, addChild, setStringValue, etc.)
     * because the DonkeyElement API surface has changed across Mirth Connect
     * versions. The DOM API is part of the JVM standard library and is
     * stable across all Mirth versions (3.8.0 -> 26.x).
     */
    protected void migrateV2ToV3Fields(DonkeyElement element) {
        // Get the underlying DOM Element — DonkeyElement.getElement() exists in all Mirth versions.
        Element root = element.getElement();
        Document doc = root.getOwnerDocument();

        // 1. transportMode
        Element tmEl = getChildElement(root, "transportMode");
        if (tmEl == null || isBlank(tmEl.getTextContent())) {
            String serverModeStr = getChildText(root, "serverMode", "true");
            boolean wasServerMode = Boolean.parseBoolean(serverModeStr);
            String newTransportMode = wasServerMode ? "TCP_SERVER" : "TCP_CLIENT";
            if (tmEl == null) {
                tmEl = doc.createElement("transportMode");
                root.appendChild(tmEl);
            }
            tmEl.setTextContent(newTransportMode);
            this.transportMode = wasServerMode ? TransportMode.TCP_SERVER : TransportMode.TCP_CLIENT;
        }

        // 2. host
        Element hostEl = getChildElement(root, "host");
        if (hostEl == null || isBlank(hostEl.getTextContent())) {
            boolean serverMode = Boolean.parseBoolean(getChildText(root, "serverMode", "true"));
            String newHost;
            if (serverMode) {
                // For server mode: use addressBind (or "0.0.0.0" if allInterfaces)
                boolean allInterfaces = Boolean.parseBoolean(getChildText(root, "allInterfaces", "true"));
                if (allInterfaces) {
                    newHost = "0.0.0.0";
                } else {
                    newHost = getChildText(root, "addressBind", "0.0.0.0");
                }
            } else {
                // For client mode: use remoteAddress
                newHost = getChildText(root, "remoteAddress", "127.0.0.1");
            }
            if (hostEl == null) {
                hostEl = doc.createElement("host");
                root.appendChild(hostEl);
            }
            hostEl.setTextContent(newHost);
            this.host = newHost;
        }

        // 3. port
        Element portEl = getChildElement(root, "port");
        if (portEl == null || isBlank(portEl.getTextContent())) {
            boolean serverMode = Boolean.parseBoolean(getChildText(root, "serverMode", "true"));
            String portStr = serverMode
                ? getChildText(root, "localPort", "3600")
                : getChildText(root, "remotePort", "3600");
            int newPort = 3600;
            try {
                if (!isBlank(portStr)) {
                    newPort = Integer.parseInt(portStr.trim());
                }
            } catch (NumberFormatException e) {
                // keep default
            }
            if (portEl == null) {
                portEl = doc.createElement("port");
                root.appendChild(portEl);
            }
            portEl.setTextContent(String.valueOf(newPort));
            this.port = newPort;
        }

        // 4. charsetName
        //    v2.4.2 hardcoded windows-1252 / CP-1252 — preserve that behavior
        //    for upgraded channels so character decoding doesn't break.
        Element charsetEl = getChildElement(root, "charsetName");
        if (charsetEl == null || isBlank(charsetEl.getTextContent())) {
            String defaultCharset = "windows-1252";
            if (charsetEl == null) {
                charsetEl = doc.createElement("charsetName");
                root.appendChild(charsetEl);
            }
            charsetEl.setTextContent(defaultCharset);
            this.charsetName = defaultCharset;
        }
    }

    // ========== DOM HELPER METHODS (standard org.w3c.dom API — works on every JVM) ==========

    /**
     * Find the first direct child Element with the given name. Returns null
     * if no such child exists.
     */
    private static Element getChildElement(Element parent, String name) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals(name)) {
                return (Element) n;
            }
        }
        return null;
    }

    /**
     * Get the trimmed text content of the first direct child Element with the
     * given name. Returns the provided defaultValue if the child doesn't exist
     * or has empty/blank text.
     */
    private static String getChildText(Element parent, String name, String defaultValue) {
        Element child = getChildElement(parent, name);
        if (child == null) {
            return defaultValue;
        }
        String text = child.getTextContent();
        return isBlank(text) ? defaultValue : text.trim();
    }

    /** True if s is null, empty, or contains only whitespace. */
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // ========== EQUALS & HASHCODE ==========
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        AstmProperties other = (AstmProperties) obj;
        return this.serverMode == other.serverMode
            && this.allInterfaces == other.allInterfaces
            && Objects.equals(this.addressBind, other.addressBind)
            && Objects.equals(this.localPort, other.localPort)
            && Objects.equals(this.remoteAddress, other.remoteAddress)
            && Objects.equals(this.remotePort, other.remotePort)
            && Objects.equals(this.astmProtocol, other.astmProtocol)
            && this.transportMode == other.transportMode
            && Objects.equals(this.host, other.host)
            && this.port == other.port
            && this.connectionTimeout == other.connectionTimeout
            && Objects.equals(this.serialPort, other.serialPort)
            && this.baudRate == other.baudRate
            && this.dataBits == other.dataBits
            && this.stopBits == other.stopBits
            && this.parity == other.parity
            && this.flowControl == other.flowControl
            && Objects.equals(this.charsetName, other.charsetName)
            && this.readTimeout == other.readTimeout
            && this.writeTimeout == other.writeTimeout
            && this.useEnqAck == other.useEnqAck
            && this.useChecksum == other.useChecksum
            && this.maxRetries == other.maxRetries
            && this.maxFrameSize == other.maxFrameSize
            && this.interFrameDelay == other.interFrameDelay;
    }

    @Override
    public int hashCode() {
        return Objects.hash(serverMode, allInterfaces, addressBind, localPort, remoteAddress, remotePort,
            astmProtocol, transportMode, host, port, connectionTimeout, serialPort, baudRate,
            dataBits, stopBits, parity, flowControl, charsetName, readTimeout, writeTimeout,
            useEnqAck, useChecksum, maxRetries, maxFrameSize, interFrameDelay);
    }
}
