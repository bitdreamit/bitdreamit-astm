package com.bitdreamit.connect.astm;

import com.mirth.connect.client.ui.AbstractSettingsPanel;
import com.mirth.connect.client.ui.Frame;
import com.mirth.connect.client.ui.PlatformUI;
import com.mirth.connect.plugins.SettingsPanelPlugin;

public class AstmSettingsClient extends SettingsPanelPlugin {
    private AbstractSettingsPanel settingsPanel = null;
    Frame mirthFrame;

    public AstmSettingsClient(String name) {
        super(name);
        this.mirthFrame = PlatformUI.MIRTH_FRAME;
        this.settingsPanel = new AstmSettingsPanel(false);
    }

    public AbstractSettingsPanel getSettingsPanel() {
        return this.settingsPanel;
    }

    public String getPluginPointName() {
        return "ASTM Settings";
    }

    public void start() {}
    public void stop() {}
    public void reset() {}

    static {
        AstmWhitelist.whiteListClasses();
    }
}