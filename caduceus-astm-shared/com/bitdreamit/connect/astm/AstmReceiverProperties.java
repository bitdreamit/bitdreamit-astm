package com.bitdreamit.connect.astm;

import com.mirth.connect.donkey.model.channel.SourceConnectorProperties;
import com.mirth.connect.donkey.model.channel.SourceConnectorPropertiesInterface;

public class AstmReceiverProperties extends AstmProperties implements SourceConnectorPropertiesInterface {
    public AstmReceiverProperties() {
        this.sourceConnectorProperties = new SourceConnectorProperties();
    }

    public AstmReceiverProperties(AstmProperties props) {
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
}
