package commons.config;

import commons.enums.PaymentModel;
import opc.enums.opc.UrlType;

public class QaEnvironment extends BaseEnvironment implements Configuration {

    private final String baseUrl;

    public QaEnvironment(final String baseUrl) {
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
        return "https://qa.weavr.io/mailhog";
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
        return "jdbc:mysql://development.cbyqez0tz540.eu-central-1.rds.amazonaws.com:3306";
    }

    @Override
    public String getDatabaseUsername() {
        return "qa-db-user";
    }

    @Override
    public String getDatabasePassword() {
        return "xnu6bVZg876jBYaP";
    }

    @Override
    public String getMainTestEnvironment() {
        return "qa";
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
                return 32;
            case DEFAULT_PAYMENT_RUN:
                return 35;
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
        return "111301415442907184";
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
        return "qa";
    }

    @Override
    public String getFpiKey() {
        return "6F04kFu2UkUBi2wMMnkAMA==";
    }

    @Override
    public String getSweepingJobApiKey() {
        return "JP5WqY96xlt7RcmsrhJ7E5bIE";
    }

//    TODO add key on QA
    @Override
    public String getOpenBankingAuthKey() {
        return "???";
    }
}