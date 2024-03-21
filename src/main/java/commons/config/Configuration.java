package commons.config;

import commons.enums.PaymentModel;
import opc.enums.opc.UrlType;

public interface Configuration {

    String getBaseUrl();
    String getSecureServiceOrigin();
    String getSimulatorUrl();
    String getOldSimulatorUrl();
    String getMailhogUrl();
    String getSumsubUrl();
    String getSumsubToken();
    String getSumsubSecret();
    String getDatabaseUrl();
    String getDatabaseUsername();
    String getDatabasePassword();
    String getMainTestEnvironment();
    String getWebhookUrl();
    String getVerificationCode();
    String getOtpVerificationCode();
    String getBaseUrl(final UrlType urlType);
    int getPaymentModelId(final PaymentModel paymentModel);
    String getWebhookCallbackUrl();
    String getPaymentRunBaseUrl();
    String getPluginId();
    String getPaymentRunWebhookUrl();
    String getOpenBankingBaseUrl();
    String getTestRunEnvironment();
    String getFpiKey();
    String getSweepingJobApiKey();
    String getOpenBankingAuthKey();
}
