package opc.junit.multi.owt.bulkpayments;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;

public class ConsumerOwtOtpVerificationAttemptLimitTests extends AbstractOwtOtpVerificationAttemptLimitTests {

    private String identityToken;
    private String currency;
    private String identityManagedAccountProfileId;
    private String destinationIdentityName;

    @BeforeEach
    public void SourceSetup() {
        final CreateConsumerModel consumerDetails =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(secondaryScaApp.getConsumersProfileId()).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(
                consumerDetails, secondaryScaApp.getSecretKey());
        currency = consumerDetails.getBaseCurrency();
        identityToken = authenticatedConsumer.getRight();

        identityManagedAccountProfileId = secondaryScaApp.getConsumerPayneticsEeaManagedAccountsProfileId();

        destinationIdentityName = String.format("%s %s", consumerDetails.getRootUser().getName(),
                consumerDetails.getRootUser().getSurname());
    }

    @Override
    protected String getToken() {
        return identityToken;
    }

    @Override
    protected String getCurrency() {
        return currency;
    }

    @Override
    public String getManagedAccountProfileId() {
        return identityManagedAccountProfileId;
    }


    @Override
    protected String getDestinationIdentityName() {
        return destinationIdentityName;
    }

    @Override
    protected IdentityType getIdentityType() {
        return IdentityType.CONSUMER;
    }
}
