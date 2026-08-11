package com.bitdreamit.connect.astm;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.channel.SourceConnectorProperties;
import com.mirth.connect.donkey.model.channel.SourceConnectorPropertiesInterface;
import com.mirth.connect.donkey.util.DonkeyElement;
import java.util.Map;
import java.util.Objects;

public class AstmReceiverProperties extends AstmProperties implements SourceConnectorPropertiesInterface {

    private SourceConnectorProperties sourceConnectorProperties;

    public AstmReceiverProperties() {
        super();
        this.sourceConnectorProperties = new SourceConnectorProperties();
    }

    public AstmReceiverProperties(AstmReceiverProperties props) {
        super(props);
        this.sourceConnectorProperties = new SourceConnectorProperties();
    }

    public String getName() {
        return "ASTM Listener";
    }

    public SourceConnectorProperties getSourceConnectorProperties() {
        return this.sourceConnectorProperties;
    }

    public boolean canBatch() {
        return false;
    }

    @Override
    public Map<String, Object> getPurgedProperties() {
        Map<String, Object> purgedProperties = super.getPurgedProperties();
        return purgedProperties;
    }

    @Override
    public ConnectorProperties clone() {
        return new AstmReceiverProperties(this);
    }

    @Override
    public void migrate3_0_1(DonkeyElement donkeyElement) {}

    @Override
    public void migrate3_0_2(DonkeyElement donkeyElement) {}

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
        AstmReceiverProperties other = (AstmReceiverProperties) obj;
        return Objects.equals(this.sourceConnectorProperties, other.sourceConnectorProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sourceConnectorProperties);
    }
}