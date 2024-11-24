package com.bitdreamit.connect.astm;

import com.mirth.connect.donkey.server.ConnectorTaskException;
import com.mirth.connect.donkey.server.channel.DispatchResult;
import com.mirth.connect.donkey.server.channel.SourceConnector;
import com.mirth.connect.server.controllers.ControllerFactory;
import com.mirth.connect.server.controllers.EventController;
import org.apache.log4j.Logger;

public class AstmReceiver extends SourceConnector {
    private Logger logger = Logger.getLogger(this.getClass());
    AstmProperties connectorProperties;
    EventController eventController = ControllerFactory.getFactory().createEventController();
    AstmConnectionManager astmMgr;
    Thread receiverService;

    public AstmReceiver() {
    }

    public void onDeploy() throws ConnectorTaskException {
        this.connectorProperties = (AstmProperties)this.getConnectorProperties();
    }

    public void onUndeploy() throws ConnectorTaskException {
    }

    public void onStart() throws ConnectorTaskException {
        this.astmMgr = new AstmConnectionManager(this, this.eventController);
        this.astmMgr.connect();
        this.receiverService = new Thread(new AstmReceiverService(this, this.astmMgr.getAsyncAstm()));
        this.receiverService.start();
    }

    public void onStop() throws ConnectorTaskException {
        if (this.receiverService != null) {
            this.receiverService.interrupt();
        }

        if (this.astmMgr != null) {
            this.astmMgr.disconnect();
        }

    }

    public void onHalt() throws ConnectorTaskException {
        this.onStop();
    }

    public void handleRecoveredResponse(DispatchResult dispatchResult) {
        this.finishDispatch(dispatchResult);
    }
}
