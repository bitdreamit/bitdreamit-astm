package com.bitdreamit.connect.astm;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.channel.DestinationConnectorProperties;
import com.mirth.connect.donkey.model.channel.SourceConnectorProperties;
import com.mirth.connect.donkey.util.DonkeyElement;
import com.mirth.connect.donkey.util.purge.PurgeUtil;
import java.util.Map;
import org.apache.commons.lang3.builder.EqualsBuilder;

public abstract class AstmProperties extends ConnectorProperties {
    protected SourceConnectorProperties sourceConnectorProperties;
    protected DestinationConnectorProperties destinationConnectorProperties;
    protected String astmProtocol;
    protected boolean serverMode;
    protected boolean allInterfaces;
    protected String addressBind;
    protected String localPort;
    protected String remoteAddress;
    protected String remotePort;

    public AstmProperties() {
        this.astmProtocol = "ELECSYS";
        this.serverMode = true;
        this.allInterfaces = true;
        this.addressBind = "0.0.0.0";
        this.localPort = "3600";
        this.remoteAddress = "127.0.0.1";
        this.remotePort = "3600";
    }

    public AstmProperties(AstmProperties props) {
        super(props);
        this.astmProtocol = props.getAstmProtocol();
        this.serverMode = props.isServerMode();
        this.allInterfaces = props.isAllInterfaces();
        this.addressBind = props.getAddressBind();
        this.localPort = props.getLocalPort();
        this.remoteAddress = props.getRemoteAddress();
        this.remotePort = props.getRemotePort();
    }

    public String getProtocol() {
        return "ASTM";
    }

    public boolean isServerMode() {
        return this.serverMode;
    }

    public void setServerMode(boolean serverMode) {
        this.serverMode = serverMode;
    }

    public boolean isAllInterfaces() {
        return this.allInterfaces;
    }

    public void setAllInterfaces(boolean allInterfaces) {
        this.allInterfaces = allInterfaces;
    }

    public String getAddressBind() {
        return this.addressBind;
    }

    public void setAddressBind(String addressBind) {
        this.addressBind = addressBind;
    }

    public String getLocalPort() {
        return this.localPort;
    }

    public void setLocalPort(String localPort) {
        this.localPort = localPort;
    }

    public String getRemoteAddress() {
        return this.remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public String getRemotePort() {
        return this.remotePort;
    }

    public void setRemotePort(String remotePort) {
        this.remotePort = remotePort;
    }

    public String getAstmProtocol() {
        return this.astmProtocol;
    }

    public void setAstmProtocol(String astmProtocol) {
        this.astmProtocol = astmProtocol;
    }

    public String toFormattedString() {
        return null;
    }

    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, new String[0]);
    }

    public void migrate3_0_1(DonkeyElement element) {
    }

    public void migrate3_0_2(DonkeyElement element) {
    }

    public void migrate3_1_0(DonkeyElement element) {
        super.migrate3_1_0(element);
    }

    public void migrate3_2_0(DonkeyElement element) {
    }

    public void migrate3_3_0(DonkeyElement element) {
    }

    public void migrate3_4_0(DonkeyElement element) {
    }

    public void migrate3_5_0(DonkeyElement element) {
    }

    public void migrate3_6_0(DonkeyElement element) {
    }

    public void migrate3_7_0(DonkeyElement element) {
    }

    public Map<String, Object> getPurgedProperties() {
        Map<String, Object> purgedProperties = super.getPurgedProperties();
        purgedProperties.put("sourceConnectorProperties", this.sourceConnectorProperties.getPurgedProperties());
        purgedProperties.put("serverMode", this.serverMode);
        purgedProperties.put("addressBind", this.addressBind);
        purgedProperties.put("localPort", PurgeUtil.getNumericValue(this.localPort));
        purgedProperties.put("remoteAddress", this.remoteAddress);
        purgedProperties.put("remotePort", PurgeUtil.getNumericValue(this.remotePort));
        return purgedProperties;
    }
}
