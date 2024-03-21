package fpi.paymentrun.enums;

import lombok.Getter;

@Getter
public enum PluginEnvironment {

    QA("PAYMENT_RUN"),
    FB("PAYMENT_RUN_FB"),
    DEV_MULTI("PAYMENT_RUN_DEV");

    private final String pluginCode;

    PluginEnvironment(final String pluginCode) {
        this.pluginCode = pluginCode;
    }
}
