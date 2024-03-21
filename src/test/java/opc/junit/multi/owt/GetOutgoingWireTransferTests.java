package opc.junit.multi.owt;

import io.restassured.response.ValidatableResponse;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.enums.opc.OwtType;
import opc.junit.database.OwtDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.OutgoingWireTransfersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;
import opc.models.multi.outgoingwiretransfers.SepaBankDetailsModel;
import opc.models.shared.VerificationModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.OutgoingWireTransfersService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag(MultiTags.OWT)
public class GetOutgoingWireTransferTests extends BaseOutgoingWireTransfersSetup {

    private static final String CHANNEL = EnrolmentChannel.SMS.name();
    private static final String VERIFICATION_CODE = "123456";

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static String corporateId;
    private static String consumerId;
    private static Pair<String, CreateManagedAccountModel> corporateManagedAccount;
    private static Pair<String, CreateManagedAccountModel> consumerManagedAccount;
    private static final List<Pair<String, OutgoingWireTransfersModel>> corporateOWTs = new ArrayList<>();
    private static final List<Pair<String, OutgoingWireTransfersModel>> consumerOWTs = new ArrayList<>();

    @BeforeAll
    public static void Setup() throws SQLException {
        corporateSetup();
        consumerSetup();

        corporateManagedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);
        consumerManagedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);
        corporateDepositAndSendOwt(corporateAuthenticationToken);
        consumerDepositAndSendOwt(consumerAuthenticationToken);

        ensureOWTsAreCompleted(corporateOWTs.get(0).getRight().getSourceInstrument().getId());
        ensureOWTsAreCompleted(consumerOWTs.get(0).getRight().getSourceInstrument().getId());
    }

    @Test
    public void GetOwt_Corporate_Success(){

        final Pair<String, OutgoingWireTransfersModel> transferAmount = corporateOWTs.get(0);

        final ValidatableResponse response =
                OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, transferAmount.getLeft(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(response, transferAmount.getRight());
    }

    @Test
    public void GetOwt_Consumer_Success(){

        final Pair<String, OutgoingWireTransfersModel> transferAmount = consumerOWTs.get(0);

        final ValidatableResponse response =
                OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, transferAmount.getLeft(), consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(response, transferAmount.getRight());
    }

    @Test
    public void GetOwt_UnknownOwtId_NotFound(){

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, RandomStringUtils.randomNumeric(18), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetOwt_InvalidOwtId_BadRequest(){

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, RandomStringUtils.randomAlphanumeric(6), consumerAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetOwt_InvalidApiKey_Unauthorised(){
        OutgoingWireTransfersService.getOutgoingWireTransfer("abc", consumerOWTs.get(0).getLeft(), consumerAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetOwt_NoApiKey_BadRequest(){
        OutgoingWireTransfersService.getOutgoingWireTransfer("", consumerOWTs.get(0).getLeft(), consumerAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetOwt_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, corporateOWTs.get(0).getLeft(), corporateAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetOwt_RootUserLoggedOut_Unauthorised(){
        final String token = ConsumersHelper.createUnauthenticatedConsumer(consumerProfileId, secretKey).getRight();

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, consumerOWTs.get(0).getLeft(), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetOwt_CrossIdentityChecks_Forbidden(){

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, corporateOWTs.get(0).getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetOwt_BackofficeCorporateImpersonator_Forbidden(){

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, corporateOWTs.get(0).getLeft(), getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetOwt_BackofficeConsumerImpersonator_Forbidden(){

        OutgoingWireTransfersService.getOutgoingWireTransfer(secretKey, corporateOWTs.get(0).getLeft(), getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    private void assertSuccessfulResponse(final ValidatableResponse response,
                                          final OutgoingWireTransfersModel outgoingWireTransfersModel) {
        response.body("id", notNullValue())
                .body("profileId", equalTo(outgoingWireTransfersModel.getProfileId()))
                .body("tag", equalTo(outgoingWireTransfersModel.getTag()))
                .body("sourceInstrument.type", equalTo(outgoingWireTransfersModel.getSourceInstrument().getType()))
                .body("sourceInstrument.id", equalTo(outgoingWireTransfersModel.getSourceInstrument().getId()))
                .body("transferAmount.currency", equalTo(outgoingWireTransfersModel.getTransferAmount().getCurrency()))
                .body("transferAmount.amount", equalTo(outgoingWireTransfersModel.getTransferAmount().getAmount().intValue()))
                .body("description", equalTo(outgoingWireTransfersModel.getDescription()))
                .body("destination.name", equalTo(outgoingWireTransfersModel.getDestinationBeneficiary().getName()))
                .body("destination.address", equalTo(outgoingWireTransfersModel.getDestinationBeneficiary().getAddress()))
                .body("destination.bankName", equalTo(outgoingWireTransfersModel.getDestinationBeneficiary().getBankName()))
                .body("destination.bankAddress", equalTo(outgoingWireTransfersModel.getDestinationBeneficiary().getBankAddress()))
                .body("destination.bankCountry", equalTo(outgoingWireTransfersModel.getDestinationBeneficiary().getBankCountry()))
                .body("state", equalTo("COMPLETED"))
                .body("creationTimestamp", notNullValue());

        OutgoingWireTransfersHelper
                .checkOwtStateByAccountId(outgoingWireTransfersModel.getSourceInstrument().getId(), "COMPLETED");

        assertBankAccountDetails(response, outgoingWireTransfersModel);
    }

    private void assertBankAccountDetails(final ValidatableResponse response,
                                          final OutgoingWireTransfersModel outgoingWireTransfersModel) {
        final SepaBankDetailsModel sepaBankDetails =
                (SepaBankDetailsModel) outgoingWireTransfersModel.getDestinationBeneficiary().getBankAccountDetails();

        response
                .body("type", equalTo(OwtType.SEPA.name()))
                .body("destination.bankAccountDetails.iban", equalTo(sepaBankDetails.getIban()))
                .body("destination.bankAccountDetails.bankIdentifierCode", equalTo(sepaBankDetails.getBankIdentifierCode()));
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).setBaseCurrency("EUR").build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerId);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, CHANNEL, secretKey, consumerAuthenticationToken);
    }

    private static void corporateSetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).setBaseCurrency("EUR").build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, CHANNEL, secretKey, corporateAuthenticationToken);
    }

    private static void corporateDepositAndSendOwt(final String token){
        fundManagedAccount(corporateManagedAccount.getLeft(), corporateCurrency, 10000L);

        IntStream.range(0, 2).forEach(i -> {
            final OutgoingWireTransfersModel outgoingWireTransfersModel =
                    OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                            corporateManagedAccount.getLeft(),
                            corporateCurrency, 100L, OwtType.SEPA).build();

            final String id =
                    OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, token, Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .extract()
                            .jsonPath()
                            .get("id");

            verifyOwt(id, token);

            corporateOWTs.add(Pair.of(id, outgoingWireTransfersModel));
        });
    }

    private static void consumerDepositAndSendOwt(final String token){
        fundManagedAccount(consumerManagedAccount.getLeft(), consumerCurrency, 10000L);

        IntStream.range(0, 2).forEach(i -> {
            final OutgoingWireTransfersModel outgoingWireTransfersModel =
                    OutgoingWireTransfersModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                            consumerManagedAccount.getLeft(),
                            consumerCurrency, 100L, OwtType.SEPA).build();

            final String id =
                    OutgoingWireTransfersService.sendOutgoingWireTransfer(outgoingWireTransfersModel, secretKey, token, Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .extract()
                            .jsonPath()
                            .get("id");

            verifyOwt(id, token);

            consumerOWTs.add(Pair.of(id, outgoingWireTransfersModel));
        });
    }

    private static void ensureOWTsAreCompleted(final String sourceInstrumentId) throws SQLException {

        final List<String> owtTransferIds = new ArrayList<>();
        final Map<Integer, Map<String, String>> owtTransfers = OwtDatabaseHelper.getOwt(sourceInstrumentId);
        owtTransferIds.add(owtTransfers.get(0).get("id"));
        owtTransferIds.add(owtTransfers.get(1).get("id"));

        owtTransferIds.forEach(transferId -> OutgoingWireTransfersHelper.checkOwtStateById(transferId, "COMPLETED"));
    }

    private static void verifyOwt(final String id,
                                  final String token) {

        TestHelper.ensureAsExpected(15,
                () -> OutgoingWireTransfersService.startOutgoingWireTransferOtpVerification(id, CHANNEL, secretKey, token),
                SC_NO_CONTENT);

        TestHelper.ensureAsExpected(15,
                () -> OutgoingWireTransfersService.verifyOutgoingWireTransfer(new VerificationModel(VERIFICATION_CODE), id,
                        CHANNEL, secretKey, token),
                SC_NO_CONTENT);
    }
}
