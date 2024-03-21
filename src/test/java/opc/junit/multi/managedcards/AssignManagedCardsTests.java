package opc.junit.multi.managedcards;

import io.restassured.response.ValidatableResponse;
import opc.enums.opc.*;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.innovator.CreateUnassignedCardBatchModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedcards.AssignManagedCardModel;
import opc.models.multi.users.UsersModel;
import opc.models.testmodels.UnassignedManagedCardDetails;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedCardsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import static opc.enums.opc.InstrumentType.PHYSICAL;
import static opc.enums.opc.InstrumentType.VIRTUAL;
import static opc.enums.opc.ManagedCardMode.DEBIT_MODE;
import static opc.enums.opc.ManagedCardMode.PREPAID_MODE;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag(MultiTags.MANAGED_CARDS_OPERATIONS)
public class AssignManagedCardsTests extends BaseManagedCardsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static String corporateId;
    private static String consumerId;
    private static List<UnassignedManagedCardDetails> corporateUnassignedCards;
    private static List<UnassignedManagedCardDetails> consumerUnassignedCards;

    @BeforeAll
    public static void Setup(){
        corporateSetup();
        consumerSetup();

        final String corporateManagedAccountId =
                createManagedAccount(corporateManagedAccountsProfileId, corporateCurrency, corporateAuthenticationToken);
        final String consumerManagedAccountId =
                createManagedAccount(consumerManagedAccountsProfileId, consumerCurrency, consumerAuthenticationToken);

        corporateUnassignedCards =
                ManagedCardsHelper.replenishCardPool(corporateManagedAccountId, corporatePrepaidManagedCardsProfileId,
                        corporateDebitManagedCardsProfileId, corporateCurrency, CardLevelClassification.CORPORATE, innovatorToken);
        consumerUnassignedCards =
                ManagedCardsHelper.replenishCardPool(consumerManagedAccountId, consumerPrepaidManagedCardsProfileId,
                        consumerDebitManagedCardsProfileId, consumerCurrency, CardLevelClassification.CONSUMER, innovatorToken);
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void AssignManagedCard_PrepaidCorporate_Success(final InstrumentType instrumentType) {

        final UnassignedManagedCardDetails unassignedCard =
                getCorporateUnassignedCard(instrumentType, PREPAID_MODE);

        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .build();

        final ValidatableResponse response =
                ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulPrepaid(response, unassignedCard.getCreateUnassignedCardBatchModel(), assignManagedCardModel, corporatePrepaidManagedCardsProfileId,
                IdentityType.CORPORATE, instrumentType);
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void AssignManagedCard_DebitCorporate_Success(final InstrumentType instrumentType) {

        final UnassignedManagedCardDetails unassignedCard =
                getCorporateUnassignedCard(instrumentType, DEBIT_MODE);

        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .build();

        final ValidatableResponse response =
                ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("parentManagedAccountId", equalTo(unassignedCard.getUnassignedCardResponseModel().getParentManagedAccountId()));

        assertSuccessfulDebit(response, unassignedCard.getCreateUnassignedCardBatchModel(), assignManagedCardModel, corporateDebitManagedCardsProfileId,
                IdentityType.CORPORATE, instrumentType);
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void AssignManagedCard_NoThreeDSecureConfigNoMobileNumberProvided_Conflict(final InstrumentType instrumentType) {

        final UnassignedManagedCardDetails unassignedCard =
                getConsumerUnassignedCard(instrumentType, PREPAID_MODE);

        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .setCardholderMobileNumber(null)
                        .setThreeDSecureAuthConfig(null)
                        .build();

        //Expect the Cardholder Mobile Number or 3DS Config to be provided
        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("THREEDS_DETAILS_NOT_PROVIDED"));
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void AssignManagedCard_RequiredOnly_Success(final InstrumentType instrumentType) {

        final UnassignedManagedCardDetails unassignedCard =
            getConsumerUnassignedCard(instrumentType, PREPAID_MODE);

        final AssignManagedCardModel assignManagedCardModel =
            AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                .setNameOnCardLine2(null)
                .setThreeDSecureAuthConfig(null)
                .build();

        final ValidatableResponse response =
            ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK);

        assertSuccessfulPrepaid(response,
            unassignedCard.getCreateUnassignedCardBatchModel(), assignManagedCardModel, consumerPrepaidManagedCardsProfileId,
            IdentityType.CONSUMER, instrumentType);
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void AssignManagedCard_PrepaidConsumer_Success(final InstrumentType instrumentType) {
        final UnassignedManagedCardDetails unassignedCard =
                getConsumerUnassignedCard(instrumentType, PREPAID_MODE);

        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .build();

        final ValidatableResponse response =
                ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulPrepaid(response,
                unassignedCard.getCreateUnassignedCardBatchModel(), assignManagedCardModel, consumerPrepaidManagedCardsProfileId,
                IdentityType.CONSUMER, instrumentType);
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void AssignManagedCard_DebitConsumer_Success(final InstrumentType instrumentType) {

        final UnassignedManagedCardDetails unassignedCard =
                getConsumerUnassignedCard(instrumentType, DEBIT_MODE);

        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .build();

        final ValidatableResponse response =
                ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK)
                        .body("parentManagedAccountId", equalTo(unassignedCard.getUnassignedCardResponseModel().getParentManagedAccountId()));

        assertSuccessfulDebit(response, unassignedCard.getCreateUnassignedCardBatchModel(), assignManagedCardModel, consumerDebitManagedCardsProfileId,
                IdentityType.CONSUMER, instrumentType);
    }

    @ParameterizedTest
    @MethodSource("getInstrumentTypes")
    public void AssignManagedCard_CorporateUser_Success(final InstrumentType instrumentType) {

        final UnassignedManagedCardDetails unassignedCard =
                getCorporateUnassignedCard(instrumentType, PREPAID_MODE);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .build();

        final ValidatableResponse response =
                ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, user.getRight())
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulPrepaid(response,
                unassignedCard.getCreateUnassignedCardBatchModel(), assignManagedCardModel, corporatePrepaidManagedCardsProfileId,
                IdentityType.CORPORATE, instrumentType);
    }

    @Test
    public void AssignManagedCard_UnknownInstrument_InstrumentNotFound() {

        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE).build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_FOUND"));
    }

    @Test
    public void AssignManagedCard_AlreadyAssigned_InstrumentNotFound() {
        final UnassignedManagedCardDetails unassignedCard =
                getConsumerUnassignedCard(VIRTUAL, PREPAID_MODE);

        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK);

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INSTRUMENT_NOT_FOUND"));
    }

    @Test
    public void AssignManagedCard_CardLevelClassificationCheck_ModelConstraintsViolated() {
        final UnassignedManagedCardDetails unassignedCard =
                getCorporateUnassignedCard(VIRTUAL, PREPAID_MODE);

        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MODEL_CONSTRAINTS_VIOLATED"));
    }

    @Test
    public void AssignManagedCard_WrongActivationCode_ActivationCodeInvalid() {
        final UnassignedManagedCardDetails unassignedCard =
                getCorporateUnassignedCard(VIRTUAL, PREPAID_MODE);

        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setActivationCode(RandomStringUtils.randomNumeric(6))
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ACTIVATION_CODE_INVALID"));
    }

    @Test
    public void AssignManagedCard_InvalidMobileNumber_BadRequest() {
        final UnassignedManagedCardDetails unassignedCard =
                getCorporateUnassignedCard(VIRTUAL, PREPAID_MODE);

        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setCardholderMobileNumber(RandomStringUtils.randomNumeric(6))
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void AssignManagedCard_NoExternalReference_BadRequest(final String externalHandle) {
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(externalHandle)
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, corporateAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void AssignManagedCard_InvalidApiKey_Unauthorised(){
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE).build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, "abc", corporateAuthenticationToken)
        .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void AssignManagedCard_NoApiKey_BadRequest(){
        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE).build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, "", corporateAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void AssignManagedCard_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE).build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, consumerAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void AssignManagedCard_RootUserLoggedOut_Unauthorised(){
        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE).build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void AssignManagedCard_BackofficeCorporateImpersonation_Forbidden() {

        final UnassignedManagedCardDetails unassignedCard =
                getCorporateUnassignedCard(VIRTUAL, PREPAID_MODE);

        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void AssignManagedCard_BackofficeConsumerImpersonation_Forbidden() {

        final UnassignedManagedCardDetails unassignedCard =
                getCorporateUnassignedCard(VIRTUAL, DEBIT_MODE);

        final AssignManagedCardModel assignManagedCardModel =
                AssignManagedCardModel.DefaultAssignManagedCardModel(TestHelper.VERIFICATION_CODE)
                        .setExternalReference(unassignedCard.getUnassignedCardResponseModel().getExternalHandle())
                        .build();

        ManagedCardsService.assignManagedCard(assignManagedCardModel, secretKey, getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    private UnassignedManagedCardDetails getCorporateUnassignedCard(final InstrumentType instrumentType,
                                                                    final ManagedCardMode managedCardMode){
        final UnassignedManagedCardDetails unassignedCard =
                corporateUnassignedCards.stream()
                        .filter(x -> x.getInstrumentType() == instrumentType
                        && x.getManagedCardMode() == managedCardMode
                        && x.getUnassignedCardResponseModel().isUnassigned())
                        .collect(Collectors.toList()).get(0);

        unassignedCard.getUnassignedCardResponseModel().setUnassigned(false);

        return unassignedCard;
    }

    private UnassignedManagedCardDetails getConsumerUnassignedCard(final InstrumentType instrumentType,
                                                                   final ManagedCardMode managedCardMode){
        final UnassignedManagedCardDetails unassignedCard =
                consumerUnassignedCards.stream()
                        .filter(x -> x.getInstrumentType() == instrumentType
                                && x.getManagedCardMode() == managedCardMode
                                && x.getUnassignedCardResponseModel().isUnassigned())
                        .collect(Collectors.toList()).get(0);

        unassignedCard.getUnassignedCardResponseModel().setUnassigned(false);

        return unassignedCard;
    }

    private void assertSuccessfulPrepaid(final ValidatableResponse response,
                                          final CreateUnassignedCardBatchModel createUnassignedCardBatchModel,
                                          final AssignManagedCardModel assignManagedCardModel,
                                          final String managedCardProfileId,
                                          final IdentityType identityType,
                                          final InstrumentType instrumentType) {
        response
                .body("tag", equalTo(createUnassignedCardBatchModel.getPrepaidCardBatchRequest().getPrepaidUnassignedCardBatchRequest().getTag()))
                .body("currency", equalTo(createUnassignedCardBatchModel.getPrepaidCardBatchRequest().getCurrency()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0));

        assertSuccessfulResponse(response, createUnassignedCardBatchModel, assignManagedCardModel, managedCardProfileId, identityType, instrumentType);
    }

    private void assertSuccessfulDebit(final ValidatableResponse response,
                                         final CreateUnassignedCardBatchModel createUnassignedCardBatchModel,
                                         final AssignManagedCardModel assignManagedCardModel,
                                         final String managedCardProfileId,
                                         final IdentityType identityType,
                                         final InstrumentType instrumentType) {
        response
                .body("tag", equalTo(createUnassignedCardBatchModel.getDebitCardBatchRequest().getDebitUnassignedCardBatchRequest().getTag()));

        assertSuccessfulResponse(response, createUnassignedCardBatchModel, assignManagedCardModel, managedCardProfileId, identityType, instrumentType);
    }

    private void assertSuccessfulResponse(final ValidatableResponse response,
                                          final CreateUnassignedCardBatchModel createUnassignedCardBatchModel,
                                          final AssignManagedCardModel assignManagedCardModel,
                                          final String managedCardProfileId,
                                          final IdentityType identityType,
                                          final InstrumentType instrumentType){
        response
                .body("id", notNullValue())
                .body("profileId", equalTo(managedCardProfileId))
                .body("externalHandle", notNullValue())
                .body("friendlyName", equalTo(assignManagedCardModel.getFriendlyName()))
                .body("state.state", equalTo("ACTIVE"))
                .body("type", equalTo(instrumentType.name()))
                .body("cardBrand", equalTo("MASTERCARD"))
                .body("cardNumber.value", notNullValue())
                .body("cvv.value", notNullValue())
                .body("cardNumberFirstSix", notNullValue())
                .body("cardNumberLastFour", notNullValue())
                .body("nameOnCard", equalTo(assignManagedCardModel.getNameOnCard()))
                .body("nameOnCardLine2", equalTo(assignManagedCardModel.getNameOnCardLine2()))
                .body("startMmyy", equalTo(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMyy"))))
                .body("expiryMmyy", equalTo(LocalDateTime.now().plusYears(3).format(DateTimeFormatter.ofPattern("MMyy"))))
                .body("cardLevelClassification", equalTo(identityType.toString()))
                .body("expiryPeriodMonths", equalTo(36))
                .body("renewalType", equalTo("NO_RENEW"))
                .body("creationTimestamp", notNullValue())
                .body("cardholderMobileNumber", equalTo(assignManagedCardModel.getCardholderMobileNumber()))
                .body("billingAddress.addressLine1", equalTo(assignManagedCardModel.getBillingAddress().getAddressLine1()))
                .body("billingAddress.addressLine2", equalTo(assignManagedCardModel.getBillingAddress().getAddressLine2()))
                .body("billingAddress.city", equalTo(assignManagedCardModel.getBillingAddress().getCity()))
                .body("billingAddress.postCode", equalTo(assignManagedCardModel.getBillingAddress().getPostCode()))
                .body("billingAddress.state", equalTo(assignManagedCardModel.getBillingAddress().getState()))
                .body("billingAddress.country", equalTo(assignManagedCardModel.getBillingAddress().getCountry()))
                .body("mode", equalTo(String.format("%s_MODE", createUnassignedCardBatchModel.getCardFundingType())));

        if (instrumentType.equals(PHYSICAL)) {
            response
                    .body("physicalCardDetails.pendingActivation", equalTo(false))
                    .body("physicalCardDetails.productReference", equalTo(CardBureau.NITECREST.getProductReference()))
                    .body("physicalCardDetails.carrierType", equalTo(CardBureau.NITECREST.getCarrierType()))
                    .body("physicalCardDetails.pinBlocked", equalTo(false))
                    .body("physicalCardDetails.replacement.replacementId", equalTo("0"))
                    //.body("physicalCardDetails.deliveryAddress.name", equalTo(assignManagedCardModel.getBillingAddress().getName()))
                    //.body("physicalCardDetails.deliveryAddress.surname", equalTo(assignManagedCardModel.getBillingAddress().getSurname()))
                    .body("physicalCardDetails.deliveryAddress.addressLine1", equalTo(assignManagedCardModel.getBillingAddress().getAddressLine1()))
                    .body("physicalCardDetails.deliveryAddress.addressLine2", equalTo(assignManagedCardModel.getBillingAddress().getAddressLine2()))
                    .body("physicalCardDetails.deliveryAddress.city", equalTo(assignManagedCardModel.getBillingAddress().getCity()))
                    .body("physicalCardDetails.deliveryAddress.postCode", equalTo(assignManagedCardModel.getBillingAddress().getPostCode()))
                    .body("physicalCardDetails.deliveryAddress.state", equalTo(assignManagedCardModel.getBillingAddress().getState()))
                    .body("physicalCardDetails.deliveryAddress.country", equalTo(assignManagedCardModel.getBillingAddress().getCountry()))
                    .body("physicalCardDetails.nameOnCardLine2", equalTo(assignManagedCardModel.getNameOnCardLine2()))
                    .body("physicalCardDetails.deliveryMethod", equalTo("STANDARD_DELIVERY"));
        }
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();

        ConsumersHelper.verifyKyc(secretKey, consumerId);
    }

    private static void corporateSetup() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }
}