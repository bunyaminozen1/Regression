package opc.junit.multi.owt;

import commons.enums.Currency;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.OwtType;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.services.multi.OutgoingWireTransfersService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

import java.util.Optional;

import static opc.junit.helpers.TestHelper.OTP_VERIFICATION_CODE;
import static org.apache.http.HttpStatus.SC_OK;

public class CorporateOwtVerificationOtpAttemptLimitTests extends AbstractOwtVerificationOtpAttemptLimitTests {

    private static String identityToken;
    private static String managedAccountId;

    @Override
    protected String getIdentityToken() {return identityToken;}
    @Override
    protected String getManagedAccountId() {return managedAccountId;}

    @BeforeAll
    public static void corporateSetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(secondaryScaApp.getCorporatesProfileId())
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper
                .createEnrolledVerifiedCorporate(createCorporateModel, secondaryScaApp.getSecretKey());
        identityToken = authenticatedCorporate.getRight();
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secondaryScaApp.getSecretKey(), identityToken);
        managedAccountId = createManagedAccount(secondaryScaApp.getCorporatePayneticsEeaManagedAccountsProfileId(),
                Currency.EUR.name(), identityToken, secondaryScaApp.getSecretKey()).getLeft();

        fundManagedAccount(managedAccountId, Currency.EUR.name(), 100000L);
    }

    @Override
    protected Pair<String, String> getNonLimitIdentity() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(passcodeAppCorporateProfileId)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> corporate = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, passcodeAppSecretKey);

        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), passcodeAppSecretKey, corporate.getRight());
        final String managedAccountId = createManagedAccount(passcodeAppCorporateManagedAccountProfileId, Currency.EUR.name(), corporate.getRight(), passcodeAppSecretKey).getLeft();

        fundManagedAccount(managedAccountId, Currency.EUR.name(), 100000L);

        final OutgoingWireTransfersModel outgoingWireTransfersModel =
                OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(passcodeAppOutgoingWireTransfersProfileId,
                        managedAccountId, Currency.EUR.name(), 100L, OwtType.SEPA).build();

        final String id = OutgoingWireTransfersService
                .sendOutgoingWireTransfer(outgoingWireTransfersModel, passcodeAppSecretKey, corporate.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().get("id");

        return Pair.of(id, corporate.getRight());
    }
}
