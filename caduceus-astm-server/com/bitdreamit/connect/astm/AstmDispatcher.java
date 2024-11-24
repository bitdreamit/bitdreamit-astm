package com.bitdreamit.connect.astm;

import com.mirth.connect.donkey.model.channel.ConnectorProperties;
import com.mirth.connect.donkey.model.message.ConnectorMessage;
import com.mirth.connect.donkey.model.message.Response;
import com.mirth.connect.donkey.model.message.Status;
import com.mirth.connect.donkey.server.ConnectorTaskException;
import com.mirth.connect.donkey.server.channel.DestinationConnector;
import com.mirth.connect.server.controllers.ControllerFactory;
import com.mirth.connect.server.controllers.EventController;
import com.mirth.connect.server.util.TemplateValueReplacer;
import com.bitdreamit.astm.asyncastm.AsyncAstmTcpDriver;
import java.rmi.ConnectException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;

public class AstmDispatcher extends DestinationConnector {
    private Logger logger = Logger.getLogger(this.getClass());
    AstmDispatcherProperties connectorProperties;
    EventController eventController = ControllerFactory.getFactory().createEventController();
    TemplateValueReplacer replacer = new TemplateValueReplacer();
    AstmConnectionManager astmMgr;
    Thread receiverService;

    public AstmDispatcher() {
    }

    public void onDeploy() throws ConnectorTaskException {
        this.connectorProperties = (AstmDispatcherProperties)this.getConnectorProperties();
    }

    public void onUndeploy() throws ConnectorTaskException {
    }

    public void onStart() throws ConnectorTaskException {
        this.astmMgr = new AstmConnectionManager(this, this.eventController);
        this.astmMgr.connect();
    }

    public void onStop() throws ConnectorTaskException {
        this.astmMgr.disconnect();
    }

    public void onHalt() throws ConnectorTaskException {
        this.onStop();
    }

    public void replaceConnectorProperties(ConnectorProperties connectorProperties, ConnectorMessage connectorMessage) {
        AstmDispatcherProperties astmProperties = (AstmDispatcherProperties)connectorProperties;
        astmProperties.setSendTimeout(this.replacer.replaceValues(astmProperties.getSendTimeout(), connectorMessage));
        astmProperties.setTemplate(this.replacer.replaceValues(astmProperties.getTemplate(), connectorMessage));
    }

    public Response send(ConnectorProperties connectorProperties, ConnectorMessage connectorMessage) {
        AsyncAstmTcpDriver asyncAstm = this.astmMgr.getAsyncAstm();
        AstmDispatcherProperties astmProperties = (AstmDispatcherProperties)connectorProperties;
        Status responseStatus = Status.PENDING;
        String responseMessage = "";
        String responseError = null;

        try {
            String message = astmProperties.getTemplate();
            long sendTimeout = NumberUtils.toLong(astmProperties.getSendTimeout());
            if (sendTimeout > 0L) {
                asyncAstm.sendMessage(message, sendTimeout, TimeUnit.MILLISECONDS);
            } else {
                asyncAstm.sendMessage(message);
            }

            responseStatus = Status.SENT;
            responseMessage = "ASTM message sent successfully";
        } catch (ConnectException var11) {
            ConnectException e = var11;
            responseStatus = Status.ERROR;
            responseError = "Connection error (" + e + ")";
        } catch (InterruptedException var12) {
            InterruptedException e = var12;
            responseStatus = Status.ERROR;
            responseError = "Interrupted thread exception (" + e + ")";
        } catch (TimeoutException var13) {
            TimeoutException e = var13;
            responseStatus = Status.ERROR;
            responseError = "Timeout exception (" + e + ")";
        }

        return new Response(responseStatus, responseMessage, responseMessage, responseError);
    }
}
