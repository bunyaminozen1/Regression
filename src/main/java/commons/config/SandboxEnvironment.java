package commons.config;

import commons.enums.PaymentModel;
import opc.enums.opc.UrlType;

public class SandboxEnvironment extends BaseEnvironment implements Configuration {

    private final String baseUrl;

    public SandboxEnvironment(final String baseUrl) {
        this.baseUrl = baseUrl;
    }
    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public String getSecureServiceOrigin() {
        return baseUrl;
    }

    @Override
    public String getSimulatorUrl() {
        return baseUrl;
    }

    @Override
    public String getOldSimulatorUrl() {
        return String.join("", baseUrl, "/old_simulate/test");
    }

    @Override
    public String getMailhogUrl() {
        return null;
    }

    @Override
    public String getSumsubUrl() {
        return SUMSUB_URL;
    }

    @Override
    public String getSumsubToken() {
        return "sbx:GFIMoa221OXpLEbzUS7LAaAu.Etq7nAbdX78ppWEuO0AxMbIct3R62fMV";
    }

    @Override
    public String getSumsubSecret() {
        return "cLvDEpHfZfxefDqzWSD2McuE8LPexJGj";
    }

    @Override
    public String getDatabaseUrl() {
        return null;
    }

    @Override
    public String getDatabaseUsername() {
        return null;
    }

    @Override
    public String getDatabasePassword() {
        return null;
    }

    @Override
    public String getMainTestEnvironment() {
        return "sandbox";
    }

    @Override
    public String getWebhookUrl() {
        return WEBHOOOK_URL;
    }

    @Override
    public String getVerificationCode() {
        return DEFAULT_VERIFICATION_CODE;
    }

    @Override
    public String getOtpVerificationCode() {
        return DEFAULT_VERIFICATION_CODE;
    }

    @Override
    public String getBaseUrl(UrlType urlType) {
        switch(urlType) {
            case BASE:
                return getBaseUrl();
            case SECURE_SERVICE:
                return getSecureServiceOrigin();
            case SIMULATOR:
                return getSimulatorUrl();
            case OLD_SIMULATOR:
                return getOldSimulatorUrl();
            case MAILHOG:
                return getMailhogUrl();
            case WEBHOOK:
                return getWebhookUrl();
            case SUMSUB:
                return getSumsubUrl();
            case WEBHOOK_CALLBACK:
                return getWebhookCallbackUrl();
            case PAYMENT_RUN:
                return getPaymentRunBaseUrl();
            case OPEN_BANKING:
                return getOpenBankingBaseUrl();
            default: throw new IllegalArgumentException("unknown url type");
        }
    }

    @Override
    public int getPaymentModelId(final PaymentModel paymentModel) {
        switch(paymentModel) {
            case DEFAULT_QA:
                return 34;
            case DEFAULT_PAYMENT_RUN:
                return 36;
            default: throw new IllegalArgumentException("Unknown payment model");
        }
    }
    @Override
    public String getWebhookCallbackUrl() {
        return baseUrl;
    }

    @Override
    public String getPaymentRunBaseUrl() {
        return baseUrl;
    }

    @Override
    public String getPluginId() {
        return null;
    }

    @Override
    public String getPaymentRunWebhookUrl() {
        return null;
    }

    @Override
    public String getOpenBankingBaseUrl() {
        return baseUrl;
    }

    @Override
    public String getTestRunEnvironment() {
        return "sandbox";
    }

    @Override
    public String getFpiKey() {
        return null;
    }

    @Override
    public String getSweepingJobApiKey() {
        return null;
    }

    @Override
    public String getOpenBankingAuthKey() {
        return null;
    }
}