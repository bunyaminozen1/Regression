package fpi.initialisation;

import fpi.paymentrun.enums.PluginEnvironment;
import opc.junit.helpers.innovator.InnovatorHelper;

import java.io.IOException;

public class DataInitialisation {

    public static void main(String[] args) throws IOException {
        InnovatorHelper.createPluginsInnovator("PluginsAutomation", "plugins@automation.io", PluginEnvironment.QA);
        InnovatorHelper.createPluginsInnovator("PluginsAutomationFB", "pluginsfb@automation.io", PluginEnvironment.FB);
        InnovatorHelper.createPluginsInnovator("PluginsAutomationDEV", "pluginsdev@automation.io", PluginEnvironment.DEV_MULTI);
    }
}
