package com.bitdreamit.connect.astm;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.channel.DestinationConnectorProperties;
import com.mirth.connect.donkey.model.channel.SourceConnectorProperties;
import com.mirth.connect.donkey.util.DonkeyElement;
import com.mirth.connect.donkey.util.purge.PurgeUtil;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.util.Map;
import java.util.Objects;

@XStreamAlias("astmProperties")
public abstract class AstmProperties extends ConnectorProperties {
    private static final long serialVersionUID = 1L;

    // ========== TRANSPORT MODE (NEW) ==========
    public enum TransportMode { TCP_CLIENT, TCP_SERVER, SERIAL }
    private TransportMode transportMode = TransportMode.TCP_CLIENT;

    // ========== OLD TCP FIELDS (KEEP FOR BACKWARD COMPATIBILITY) ==========
    protected boolean serverMode = true;
    protected boolean allInterfaces = true;
    protected String addressBind = "0.0.0.0";
    protected String localPort = "3600";
    protected String remoteAddress = "127.0.0.1";
    protected String remotePort = "3600";

    // ========== NEW TCP FIELDS ==========
    private String host = "127.0.0.1";
    private int port = 5004;
    private int connectionTimeout = 30000;

    // ========== SERIAL FIELDS (NEW) ==========
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

    // ========== CONNECTOR PROPERTIES (OLD — KEEP) ==========
    protected SourceConnectorProperties sourceConnectorProperties;
    protected DestinationConnectorProperties destinationConnectorProperties;

    // ========== CONSTRUCTORS ==========
    public AstmProperties() {
        super();
    }

    public AstmProperties(AstmProperties props) {
        super(props);
        if (props != null) {
            // Old fields
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

            // New fields
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


    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

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
        } else {
            return "ASTM TCP [" + host + ":" + port + "]";
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
        return purgedProperties;
    }

    // ========== MIGRATION METHODS (OLD — KEEP) ==========
    public void migrate3_0_1(DonkeyElement element) {}
    public void migrate3_0_2(DonkeyElement element) {}
    public void migrate3_1_0(DonkeyElement element) { super.migrate3_1_0(element); }
    public void migrate3_2_0(DonkeyElement element) {}
    public void migrate3_3_0(DonkeyElement element) {}
    public void migrate3_4_0(DonkeyElement element) {}
    public void migrate3_5_0(DonkeyElement element) {}
    public void migrate3_6_0(DonkeyElement element) {}
    public void migrate3_7_0(DonkeyElement element) {}

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