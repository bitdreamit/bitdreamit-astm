package com.bitdreamit.connect.astm;

import com.mirth.connect.client.core.api.util.OperationUtil;
import com.mirth.connect.model.ExtensionPermission;
import com.mirth.connect.plugins.ServicePlugin;
import com.mirth.connect.server.controllers.ConfigurationController;
import com.mirth.connect.server.controllers.ControllerFactory;
import com.mirth.connect.server.controllers.ExtensionController;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class AstmService implements ServicePlugin {
    public static final String PLUGINPOINT = "ASTM Settings";
    protected static final String VERSION = "2.4.2";
    private static final ExtensionController extensionController;
    ConfigurationController configurationController = ControllerFactory.getFactory().createConfigurationController();
    private Logger logger = Logger.getLogger(this.getClass());
    private String driverPackage = "es.bitdreamit.astm.asyncastm";
    private String extensionPackage = "es.bitdreamit.connect.astm";
    private Logger driverLogger;
    private Logger extensionLogger;
    Appender astmAppender;

    public AstmService() {
        this.driverLogger = LogManager.getLogger(this.driverPackage);
        this.extensionLogger = LogManager.getLogger(this.extensionPackage);
        this.astmAppender = new ConsoleAppender(new PatternLayout("%-5p [%t]: %m%n"));
    }

    public String getPluginPointName() {
        return "ASTM Settings";
    }

    public void start() {
    }

    public void stop() {
    }

    public void init(Properties properties) {
        this.driverLogger.addAppender(this.astmAppender);
        this.extensionLogger.addAppender(this.astmAppender);
        this.update(properties);
    }

    public void update(Properties properties) {
        String serverUUID = this.configurationController.getServerId();

        String driverLevelStr = properties.getProperty("driver_log_level");
        String extensionLevelStr = properties.getProperty("extension_log_level");

        try {
            Class<?> log4j2ConfiguratorClass = Class.forName("org.apache.logging.log4j.core.config.Configurator");
            Class<?> log4j2LevelClass = Class.forName("org.apache.logging.log4j.Level");
            Method log4j2LevelToLevelMethod = log4j2LevelClass.getMethod("toLevel", String.class);
            Object log4j2DriverLevel = log4j2LevelToLevelMethod.invoke((Object)null, driverLevelStr);
            Object log4j2ExtensionLevel = log4j2LevelToLevelMethod.invoke((Object)null, extensionLevelStr);
            Method log4j2ConfiguratorSetLevelMethod = log4j2ConfiguratorClass.getMethod("setLevel", String.class, log4j2LevelClass);
            log4j2ConfiguratorSetLevelMethod.invoke((Object)null, this.driverPackage, log4j2DriverLevel);
            log4j2ConfiguratorSetLevelMethod.invoke((Object)null, this.extensionPackage, log4j2ExtensionLevel);
            System.out.println("Log4j 2 levels for ASTM extension were set");
        } catch (ClassNotFoundException var20) {
            this.driverLogger.setLevel(Level.toLevel(driverLevelStr));
            this.extensionLogger.setLevel(Level.toLevel(extensionLevelStr));
            System.out.println("Log4j 2 not found, Log4j log levels were set");
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException var21) {
            Exception e = var21;
            ((Exception)e).printStackTrace();
        } catch (SecurityException var22) {
            SecurityException e = var22;
            e.printStackTrace();
        }

    }

    public Properties getDefaultProperties() {
        Properties properties = new Properties();
        properties.setProperty("driver_log_level", Level.INFO.toString());
        properties.setProperty("extension_log_level", Level.INFO.toString());
        return properties;
    }

    public ExtensionPermission[] getExtensionPermissions() {
        ExtensionPermission statusPermission = new ExtensionPermission("ASTM Settings", "Driver Status", "Retrieves the ASTM driver status.", OperationUtil.getOperationNamesForPermission("Driver Status", AstmServletInterface.class, new String[]{"getPluginProperties"}), new String[]{"doShowSettings"});
        ExtensionPermission setLicensePermission = new ExtensionPermission("ASTM Settings", "Change License", "Sets a new license.", OperationUtil.getOperationNamesForPermission("Change License", AstmServletInterface.class, new String[]{"setPluginProperties"}), new String[]{"doSave"});
        return new ExtensionPermission[]{statusPermission, setLicensePermission};
    }

    static {
        AstmWhitelist.whiteListClasses();
        extensionController = ControllerFactory.getFactory().createExtensionController();
    }
}
