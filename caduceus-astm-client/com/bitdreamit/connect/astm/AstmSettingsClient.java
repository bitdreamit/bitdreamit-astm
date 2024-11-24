package com.bitdreamit.connect.astm;

import com.mirth.connect.client.core.ClientException;
import com.mirth.connect.client.ui.AbstractSettingsPanel;
import com.mirth.connect.client.ui.Frame;
import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.plugins.SettingsPanelPlugin;
import java.time.Duration;
import java.util.Map;
import javax.swing.SwingWorker;

public class AstmSettingsClient extends SettingsPanelPlugin {
    private AbstractSettingsPanel settingsPanel = null;
    Frame mirthFrame;

    public AstmSettingsClient(String name) {
        super(name);
        this.mirthFrame = PlatformUI.MIRTH_FRAME;
        this.settingsPanel = new AstmSettingsPanel("ASTM Extension", this);
    }

    public AbstractSettingsPanel getSettingsPanel() {
        return this.settingsPanel;
    }

    public String getPluginPointName() {
        return "ASTM Settings";
    }

    public void start() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            public Void doInBackground() throws ClientException {
                Map<String, Object> statusMap = ((AstmServletInterface)AstmSettingsClient.this.parent.mirthClient.getServlet(AstmServletInterface.class)).getStatusMap();
                statusMap.put("valid", true);
                return null;
            }

            public void done() {

            }
        };
        worker.execute();
    }

    public void stop() {
    }

    public void reset() {
    }

    static {
        AstmWhitelist.whiteListClasses();
    }

    class LicenseInfo {
        public final boolean valid;
        public final Duration duration;

        public LicenseInfo(boolean valid, Duration duration) {
            this.valid = true;
            this.duration = Duration.ofDays(9999);
        }
    }
}
