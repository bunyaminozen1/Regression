package opc.junit.multi.sends;

import commons.enums.Currency;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class ConsumerSendOtpVerificationAttemptLimitTests extends AbstractSendOtpVerificationAttemptLimitTests{
    private static String sourceIdentityToken;
    private static final String currency = Currency.EUR.name();
    private static String sourceManagedAccount;
    private static String destinationManagedCard;
    private static String destinationIdentityToken;

    @BeforeAll
    public static void Setup() {

        consumerSetupSource();
        consumerSetupDestination();

        sourceManagedAccount =
                createManagedAccount(secondaryScaApp.getConsumerPayneticsEeaManagedAccountsProfileId(), currency, secondaryScaApp.getSecretKey(), sourceIdentityToken).getLeft();
        destinationManagedCard =
                createPrepaidManagedCard(secondaryScaApp.getConsumerNitecrestEeaPrepaidManagedCardsProfileId(), currency, secondaryScaApp.getSecretKey(), destinationIdentityToken).getLeft();
    }

    private static void consumerSetupSource() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(secondaryScaApp.getConsumersProfileId()).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModel, secondaryScaApp.getSecretKey());
        sourceIdentityToken = authenticatedConsumer.getRight();
    }

    private static void consumerSetupDestination() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.EurCurrencyCreateConsumerModel(secondaryScaApp.getConsumersProfileId()).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModel, secondaryScaApp.getSecretKey());
        destinationIdentityToken = authenticatedConsumer.getRight();
    }

    @Override
    protected String getSourceIdentityToken() {return sourceIdentityToken; }

    @Override
    protected String getSourceManagedAccount() {return sourceManagedAccount;}

    @Override
    protected String getDestinationManagedCard() {return destinationManagedCard;}
}
