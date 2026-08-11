package com.bitdreamit.connect.astm;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.channel.DestinationConnectorProperties;
import com.mirth.connect.donkey.model.channel.DestinationConnectorPropertiesInterface;
import com.mirth.connect.donkey.util.DonkeyElement;
import com.mirth.connect.donkey.util.purge.PurgeUtil;
import java.util.Map;
import java.util.Objects;

public class AstmDispatcherProperties extends AstmProperties implements DestinationConnectorPropertiesInterface {

    private DestinationConnectorProperties destinationConnectorProperties;
    private String template;
    private String sendTimeout;

    public AstmDispatcherProperties() {
        super();
        this.destinationConnectorProperties = new DestinationConnectorProperties();
        this.template = "${message.encodedData}";
        this.sendTimeout = "20000";
    }

    public AstmDispatcherProperties(AstmDispatcherProperties props) {
        super(props);
        this.destinationConnectorProperties = new DestinationConnectorProperties();
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

    @Override
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

    @Override
    public void migrate3_0_1(DonkeyElement element) {}

    @Override
    public void migrate3_0_2(DonkeyElement element) {}

    @Override
    public void migrate4_4_0(DonkeyElement element) {
        super.migrate4_4_0(element);
    }

    @Override
    public void migrate4_5_0(DonkeyElement element) {
        super.migrate4_5_0(element);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;

        AstmDispatcherProperties other = (AstmDispatcherProperties) obj;
        return Objects.equals(this.template, other.template)
                && Objects.equals(this.sendTimeout, other.sendTimeout)
                && Objects.equals(this.destinationConnectorProperties, other.destinationConnectorProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), template, sendTimeout, destinationConnectorProperties);
    }

    @Override
    public String toFormattedString() {
        StringBuilder builder = new StringBuilder();
        String newLine = "\n";
        builder.append("MODE: ");
        if (this.isServerMode()) {
            builder.append("SERVER");
            builder.append(newLine);
            builder.append("BIND: ");
            builder.append(this.getHost() != null ? this.getHost() : "0.0.0.0");
            builder.append(":");
            builder.append(this.getPort());
        } else {
            builder.append("CLIENT");
            builder.append(newLine);
            builder.append("ADDRESS: ");
            builder.append(this.getHost());
            builder.append(":");
            builder.append(this.getPort());
        }

        builder.append(newLine);
        builder.append("PROTOCOL: ");
        builder.append(this.getProtocol());
        builder.append(" ");
        builder.append(this.getAstmProtocol());
        builder.append(newLine);
        builder.append(newLine);
        builder.append("[CONTENT]");
        builder.append(newLine);
        builder.append(this.getTemplate());
        return builder.toString();
    }
}