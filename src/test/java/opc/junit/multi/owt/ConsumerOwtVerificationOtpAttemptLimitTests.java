package opc.junit.multi.owt;

import commons.enums.Currency;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.OwtType;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.services.multi.OutgoingWireTransfersService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

import java.util.Optional;

import static opc.junit.helpers.TestHelper.OTP_VERIFICATION_CODE;
import static org.apache.http.HttpStatus.SC_OK;

public class ConsumerOwtVerificationOtpAttemptLimitTests extends AbstractOwtVerificationOtpAttemptLimitTests{

    private static String identityToken;
    private static String managedAccountId;

    @Override
    protected String getIdentityToken() {return identityToken;}
    @Override
    protected String getManagedAccountId() {return managedAccountId;}

    @BeforeAll
    public static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(secondaryScaApp.getConsumersProfileId())
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper
                .createEnrolledVerifiedConsumer(createConsumerModel, secondaryScaApp.getSecretKey());
        identityToken = authenticatedConsumer.getRight();
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secondaryScaApp.getSecretKey(), identityToken);
        managedAccountId = createManagedAccount(secondaryScaApp.getConsumerPayneticsEeaManagedAccountsProfileId(),
                Currency.EUR.name(), identityToken, secondaryScaApp.getSecretKey()).getLeft();

        fundManagedAccount(managedAccountId, Currency.EUR.name(), 100000L);
    }

    @Override
    protected Pair<String, String> getNonLimitIdentity() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(passcodeAppConsumerProfileId)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> consumer = ConsumersHelper.createEnrolledVerifiedConsumer(createConsumerModel, passcodeAppSecretKey);

        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), passcodeAppSecretKey, consumer.getRight());
        final String managedAccountId = createManagedAccount(passcodeAppConsumerManagedAccountProfileId, Currency.EUR.name(), consumer.getRight(), passcodeAppSecretKey).getLeft();

        fundManagedAccount(managedAccountId, Currency.EUR.name(), 100000L);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId,
                        managedAccountId, Currency.EUR.name(), 100L, OwtType.SEPA).build();

        final String id = OutgoingWireTransfersService
                .sendOutgoingWireTransfer(outgoingWireTransfersModel, passcodeAppSecretKey, consumer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().get("id");

        return Pair.of(id, consumer.getRight());
    }
}
