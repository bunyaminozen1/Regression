package commons.config;

import commons.enums.PaymentModel;
import opc.enums.opc.UrlType;

public class DevEnvironment extends BaseEnvironment implements Configuration {

    private final String baseUrl;

    public DevEnvironment(final String baseUrl) {
        this.baseUrl = baseUrl;
    }
    @Override
    public String getBaseUrl() {
        return String.join("", baseUrl, ":8080");
    }

    @Override
    public String getSecureServiceOrigin() {
        return "js.weavr.io";
    }

    @Override
    public String getSimulatorUrl() {
        return String.join("", baseUrl, ":8180");
    }

    @Override
    public String getOldSimulatorUrl() {
        return String.join("", baseUrl, ":8180/test");
    }

    @Override
    public String getMailhogUrl() {
        return String.join("", baseUrl, ":8025");
    }

    @Override
    public String getSumsubUrl() {
        return SUMSUB_URL;
    }

    @Override
    public String getSumsubToken() {
        return "sbx:W7GHRd0EETN4BQ4bD4LpGQfp.YEcT97BTAdtkS3GZqolaY62cGI1raq1b";
    }

    @Override
    public String getSumsubSecret() {
        return "AR72koIKkbQ1tsOsxUuOzBTekOcu9c95";
    }

    @Override
    public String getDatabaseUrl() {
        return String.join("", "jdbc:mysql://", baseUrl.split("http://")[1], ":3306");
    }

    @Override
    public String getDatabaseUsername() {
        return "root";
    }

    @Override
    public String getDatabasePassword() {
        return "root";
    }

    @Override
    public String getMainTestEnvironment() {
        return "dev";
    }

    @Override
    public String getWebhookUrl() {
        return WEBHOOOK_URL;
    }

    @Override
    public String getVerificationCode() {
        return "111111";
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
                return 21;
            case DEFAULT_PAYMENT_RUN:
                return 23;
            default: throw new IllegalArgumentException("Unknown payment model");
        }
    }

    @Override
    public String getWebhookCallbackUrl() {
        return String.join("", baseUrl, ":8480");
    }
    @Override
    public String getPaymentRunBaseUrl() {
        return String.join("", baseUrl, ":5009");
    }

    @Override
    public String getPluginId() {
        return "112089486570422280";
    }

    @Override
    public String getPaymentRunWebhookUrl() {
        return String.join("", getPaymentRunBaseUrl(), "/webhook/v1");
    }
    @Override
    public String getOpenBankingBaseUrl() {
        return String.join("", baseUrl, ":5008");
    }

    @Override
    public String getTestRunEnvironment() {
        return "dev";
    }

    @Override
    public String getFpiKey() {
        return "TZe0HsVeGaUBjjjLThYACA==";
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