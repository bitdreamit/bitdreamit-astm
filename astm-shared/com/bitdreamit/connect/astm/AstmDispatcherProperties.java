package com.bitdreamit.connect.astm;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.channel.DestinationConnectorProperties;
import com.mirth.connect.donkey.model.channel.DestinationConnectorPropertiesInterface;
import com.mirth.connect.donkey.util.purge.PurgeUtil;
import java.util.Map;

public class AstmDispatcherProperties extends AstmProperties implements DestinationConnectorPropertiesInterface {
    private String template;
    private String sendTimeout;

    public AstmDispatcherProperties() {
        this.destinationConnectorProperties = new DestinationConnectorProperties();
        this.template = "${message.encodedData}";
        this.sendTimeout = "20000";
    }

    public AstmDispatcherProperties(AstmDispatcherProperties props) {
        super(props);
        this.destinationConnectorProperties = new DestinationConnectorProperties(props.getDestinationConnectorProperties());
        this.template = props.getTemplate();
        this.sendTimeout = props.getSendTimeout();
    }

    public String getTemplate() {
        return this.template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getSendTimeout() {
        return this.sendTimeout;
    }

    public void setSendTimeout(String sendTimeout) {
        this.sendTimeout = sendTimeout;
    }

    public String getName() {
        return "ASTM Sender";
    }

    public DestinationConnectorProperties getDestinationConnectorProperties() {
        return this.destinationConnectorProperties;
    }

    public boolean canValidateResponse() {
        return false;
    }

    public Map<String, Object> getPurgedProperties() {
        Map<String, Object> purgedProperties = super.getPurgedProperties();
        purgedProperties.put("sendTimeout", PurgeUtil.getNumericValue(this.sendTimeout));
        purgedProperties.put("templateLines", PurgeUtil.countLines(this.template));
        return purgedProperties;
    }

    public ConnectorProperties clone() {
        return new AstmDispatcherProperties(this);
    }

    public String toFormattedString() {
        StringBuilder builder = new StringBuilder();
        String newLine = "\n";
        builder.append("MODE: ");
        if (this.serverMode) {
            builder.append("SERVER");
            builder.append(newLine);
            builder.append("BIND: ");
            builder.append(this.addressBind != null ? this.addressBind : "0.0.0.0");
            builder.append(":");
            builder.append(this.localPort);
        } else {
            builder.append("CLIENT");
            builder.append(newLine);
            builder.append("ADDRESS: ");
            builder.append(this.remoteAddress);
            builder.append(":");
            builder.append(this.remotePort);
        }

        builder.append(newLine);
        builder.append("PROTOCOL: ");
        builder.append(this.getProtocol());
        builder.append(" ");
        builder.append(this.astmProtocol);
        builder.append(newLine);
        builder.append(newLine);
        builder.append("[CONTENT]");
        builder.append(newLine);
        builder.append(this.getTemplate());
        return builder.toString();
    }
}
