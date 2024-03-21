package opc.junit.openbanking.outgoingwiretransfers;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import commons.enums.Currency;
import opc.enums.opc.FeeType;
import opc.enums.opc.ManagedInstrumentType;
import opc.enums.opc.OwtType;
import commons.enums.State;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.multi.OutgoingWireTransfersHelper;
import opc.junit.helpers.openbanking.OpenBankingAccountInformationHelper;
import opc.junit.helpers.openbanking.OpenBankingHelper;
import opc.junit.helpers.openbanking.OpenBankingSecureServiceHelper;
import opc.junit.openbanking.BaseSetup;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.outgoingwiretransfers.Beneficiary;
import opc.models.multi.outgoingwiretransfers.FasterPaymentsBankDetailsModel;
import opc.models.multi.outgoingwiretransfers.SepaBankDetailsModel;
import opc.models.openbanking.CreateOutgoingWireTransferModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.services.innovator.InnovatorService;
import opc.services.multi.AuthenticationService;
import opc.services.multi.ManagedAccountsService;
import opc.services.multi.OutgoingWireTransfersService;
import opc.services.openbanking.PaymentInitiationService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static opc.enums.opc.ManagedInstrumentType.UNKNOWN;
import static opc.enums.openbanking.SignatureHeader.DATE;
import static opc.enums.openbanking.SignatureHeader.DIGEST;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_PRECONDITION_FAILED;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class CreateOutgoingWireTransferTests extends BaseSetup {

    @BeforeAll
    public static void OneTimeSetup() throws Exception {
        corporateSetup();
        consumerSetup();

        final String corporateConsent = OpenBankingAccountInformationHelper.createConsent(sharedKey, OpenBankingHelper.generateHeaders(clientKeyId));
        final String consumerConsent = OpenBankingAccountInformationHelper.createConsent(sharedKey, OpenBankingHelper.generateHeaders(clientKeyId));

        OpenBankingSecureServiceHelper.authoriseConsent(sharedKey, corporateAuthenticationToken, tppId, corporateConsent);
        OpenBankingSecureServiceHelper.authoriseConsent(sharedKey, consumerAuthenticationToken, tppId, consumerConsent);
    }

    @ParameterizedTest
    @MethodSource("owtSuccessfulArgs")
    public void SendOwt_Corporate_Success(final Currency currency, final OwtType type, final FeeType feeType) throws Exception {

        final Long paymentAmount = 200L;

        final int fee = TestHelper.getFees(currency.name()).get(feeType).getAmount().intValue();

        final Pair<String, Pair<Integer, Integer>> managedAccount =
                ManagedAccountsHelper.createFundedManagedAccount(corporateManagedAccountProfileId,
                        currency.name(), secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        currency.name(), paymentAmount, type)
                        .description(RandomStringUtils.randomAlphabetic(type.equals(OwtType.SEPA) ? 35 : 18))
                        .build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        final ValidatableResponse response =
                PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                    .then()
                    .statusCode(SC_OK);

        assertSuccessfulResponse(response, createOutgoingWireTransferModel, type, corporateAuthenticationToken, sharedKey);

        final int currentBalance =
                managedAccount.getRight().getLeft() - paymentAmount.intValue() - fee;

        assertManagedAccountBalance(managedAccount.getLeft(), corporateAuthenticationToken, currentBalance);
    }

    @ParameterizedTest
    @MethodSource("owtSuccessfulArgs")
    public void SendOwt_Consumer_Success(final Currency currency, final OwtType type, final FeeType feeType) throws Exception {

        final Long paymentAmount = 200L;

        final int fee = TestHelper.getFees(currency.name()).get(feeType).getAmount().intValue();

        final Pair<String, Pair<Integer, Integer>> managedAccount =
                ManagedAccountsHelper.createFundedManagedAccount(consumerManagedAccountProfileId,
                        currency.name(), secretKey, consumerAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        currency.name(), paymentAmount, type)
                        .description(RandomStringUtils.randomAlphabetic(type.equals(OwtType.SEPA) ? 35 : 18))
                        .build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        final ValidatableResponse response =
                PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(response, createOutgoingWireTransferModel, type, consumerAuthenticationToken, sharedKey);

        final int currentBalance =
                managedAccount.getRight().getLeft() - paymentAmount.intValue() - fee;

        assertManagedAccountBalance(managedAccount.getLeft(), consumerAuthenticationToken, currentBalance);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class, names = { "EUR" })
    public void SendOwt_RequiredOnly_Success(final Currency currency) throws Exception {

        final Long paymentAmount = 200L;
        final int fee = TestHelper.getFees(currency.name())
                .get(FeeType.SEPA_OWT_FEE).getAmount().intValue();

        final Pair<String, Pair<Integer, Integer>> managedAccount =
                ManagedAccountsHelper.createFundedManagedAccount(corporateManagedAccountProfileId,
                        currency.name(), secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.builder()
                .profileId(outgoingWireTransfersProfileId)
                .destinationBeneficiary(Beneficiary.DefaultBeneficiaryWithSepa().build())
                .sourceInstrument(new ManagedInstrumentTypeId(managedAccount.getLeft(), ManagedInstrumentType.MANAGED_ACCOUNTS))
                .transferAmount(new CurrencyAmount(currency.name(), paymentAmount)).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        final ValidatableResponse response =
                PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(response, createOutgoingWireTransferModel, OwtType.SEPA, consumerAuthenticationToken, sharedKey);

        final int currentBalance =
                managedAccount.getRight().getLeft() - paymentAmount.intValue() - fee;

        assertManagedAccountBalance(managedAccount.getLeft(), consumerAuthenticationToken, currentBalance);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class, names = { "EUR" })
    public void SendOwt_BeneficiaryRequiredOnly_Success(final Currency currency) throws Exception {

        final Long paymentAmount = 200L;
        final int fee = TestHelper.getFees(currency.name())
                .get(FeeType.SEPA_OWT_FEE).getAmount().intValue();

        final Pair<String, Pair<Integer, Integer>> managedAccount =
                ManagedAccountsHelper.createFundedManagedAccount(corporateManagedAccountProfileId,
                        currency.name(), secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccount.getLeft(),
                        currency.name(), paymentAmount, OwtType.SEPA)
                        .description(RandomStringUtils.randomAlphabetic(35))
                        .destinationBeneficiary(Beneficiary.builder()
                                .setName(RandomStringUtils.randomAlphabetic(5))
                                .setBankAccountDetails(new SepaBankDetailsModel("TS123123123213213123", "TS12TEST123"))
                                .build())
                        .build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        final ValidatableResponse response =
                PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(response, createOutgoingWireTransferModel, OwtType.SEPA, consumerAuthenticationToken, sharedKey);

        final int currentBalance =
                managedAccount.getRight().getLeft() - paymentAmount.intValue() - fee;

        assertManagedAccountBalance(managedAccount.getLeft(), consumerAuthenticationToken, currentBalance);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class, names = { "USD", "GBP" })
    public void SendOwt_SepaUnsupportedCurrency_Conflict(final Currency currency) throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        currency.name(), secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        currency.name(), 200L, OwtType.SEPA).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CURRENCY_MISMATCH"));
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class, names = { "USD", "EUR" })
    public void SendOwt_FasterPaymentsUnsupportedCurrency_Conflict(final Currency currency) throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        currency.name(), secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        currency.name(), 200L, OwtType.FASTER_PAYMENTS).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CURRENCY_MISMATCH"));
    }

    @Test
    public void SendOwt_UnknownSourceInstrumentId_SourceNotFound() throws Exception {

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        RandomStringUtils.randomNumeric(18),
                        corporateCurrency, 10L, OwtType.SEPA).build();


        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_NOT_FOUND"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void SendOwt_NoSourceInstrumentId_BadRequest(final String instrumentId) throws Exception {

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        instrumentId,
                        corporateCurrency, 10L, OwtType.SEPA).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendOwt_UnknownProfileId_Conflict() throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(RandomStringUtils.randomNumeric(18),
                        managedAccountId,
                        corporateCurrency, 10L, OwtType.SEPA).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MESSAGE"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void SendOwt_NoProfileId_BadRequest(final String profileId) throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, corporateCurrency, secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(profileId,
                        managedAccountId,
                        corporateCurrency, 10L, OwtType.SEPA).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendOwt_UnknownSourceInstrumentType_BadRequest() throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        corporateCurrency, 10L, OwtType.SEPA)
                        .sourceInstrument(new ManagedInstrumentTypeId(managedAccountId, UNKNOWN))
                        .build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendOwt_CrossIdentityCheck_SourceNotFound() throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        corporateCurrency, 10L, OwtType.SEPA).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_NOT_FOUND"));
    }

    @Test
    public void SendOwt_NoFunds_FundsInsufficient() throws Exception {
        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        corporateCurrency, 100L, OwtType.SEPA).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FUNDS_INSUFFICIENT"));
    }

    @Test
    public void SendOwt_NotEnoughFunds_FundsInsufficient() throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        corporateCurrency, 5000L, OwtType.SEPA).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FUNDS_INSUFFICIENT"));
    }

    @Test
    public void SendOwt_NotEnoughFundsForFee_FundsInsufficient() throws Exception {

        final Long owtAmount = 200L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);

        fundManagedAccount(managedAccountId, corporateCurrency, owtAmount);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        corporateCurrency, owtAmount, OwtType.SEPA).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FUNDS_INSUFFICIENT"));
    }

    @Test
    public void SendOwt_InvalidAmount_AmountInvalid() throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        corporateCurrency, -100L, OwtType.SEPA).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("AMOUNT_INVALID"));
    }

    @Test
    public void SendOwt_SourceManagedAccountRemoved_SourceInstrumentDestroyed() throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);

        ManagedAccountsHelper.removeManagedAccount(managedAccountId, secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        corporateCurrency, 10L, OwtType.SEPA).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_INSTRUMENT_DESTROYED"));
    }

    @Test
    public void SendOwt_SourceManagedAccountBlocked_SourceInstrumentBlocked() throws Exception {
        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);

        fundManagedAccount(managedAccountId, corporateCurrency, 1000L);

        ManagedAccountsHelper.blockManagedAccount(managedAccountId, secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        corporateCurrency, 100L, OwtType.SEPA).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_INSTRUMENT_BLOCKED"));
    }

    @Test
    public void SendOwt_MissingBeneficiaryDetails_BadRequest() throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);
        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.builder()
                        .tag(RandomStringUtils.randomAlphabetic(5))
                        .description(RandomStringUtils.randomAlphabetic(5))
                        .profileId(outgoingWireTransfersProfileId)
                        .sourceInstrument(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .transferAmount(new CurrencyAmount(corporateCurrency, 100L)).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void SendOwt_MissingBankAccountIban_BadRequest(final String iban) throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccountId,
                        corporateCurrency, 200L, OwtType.SEPA)
                        .destinationBeneficiary(Beneficiary.DefaultBeneficiary()
                                .setBankAccountDetails(new SepaBankDetailsModel(iban, "TS12TEST123"))
                                .build())
                        .build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void SendOwt_MissingBankAccountBic_BadRequest(final String bic) throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccountId,
                        corporateCurrency, 200L, OwtType.SEPA)
                        .destinationBeneficiary(Beneficiary.DefaultBeneficiary()
                                .setBankAccountDetails(new SepaBankDetailsModel("TS123123123213213123", bic))
                                .build())
                        .build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendOwt_MissingBankAccountDetails_BadRequest() throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);
        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.builder()
                        .tag(RandomStringUtils.randomAlphabetic(5))
                        .description(RandomStringUtils.randomAlphabetic(5))
                        .profileId(outgoingWireTransfersProfileId)
                        .destinationBeneficiary(Beneficiary.DefaultBeneficiary().build())
                        .sourceInstrument(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .transferAmount(new CurrencyAmount(corporateCurrency, 100L)).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendOwt_MissingBeneficiaryName_BadRequest() throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);
        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.builder()
                        .tag(RandomStringUtils.randomAlphabetic(5))
                        .description(RandomStringUtils.randomAlphabetic(5))
                        .profileId(outgoingWireTransfersProfileId)
                        .destinationBeneficiary(Beneficiary.DefaultBeneficiaryWithSepa()
                                .setName(null)
                                .build())
                        .sourceInstrument(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .transferAmount(new CurrencyAmount(corporateCurrency, 100L)).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendOwt_MissingSourceInstrument_BadRequest() throws Exception {

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.builder()
                        .tag(RandomStringUtils.randomAlphabetic(5))
                        .description(RandomStringUtils.randomAlphabetic(5))
                        .profileId(outgoingWireTransfersProfileId)
                        .destinationBeneficiary(Beneficiary.DefaultBeneficiaryWithSepa().build())
                        .transferAmount(new CurrencyAmount(corporateCurrency, 100L)).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendOwt_MissingTransferAmount_BadRequest() throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.builder()
                        .tag(RandomStringUtils.randomAlphabetic(5))
                        .description(RandomStringUtils.randomAlphabetic(5))
                        .profileId(outgoingWireTransfersProfileId)
                        .destinationBeneficiary(Beneficiary.DefaultBeneficiaryWithSepa().build())
                        .sourceInstrument(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void SendOwt_NoCurrency_BadRequest(final String currency) throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.builder()
                        .tag(RandomStringUtils.randomAlphabetic(5))
                        .description(RandomStringUtils.randomAlphabetic(5))
                        .profileId(outgoingWireTransfersProfileId)
                        .destinationBeneficiary(Beneficiary.DefaultBeneficiaryWithSepa().build())
                        .sourceInstrument(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .transferAmount(new CurrencyAmount(currency, 10L))
                        .build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class, names = { "EUR" })
    public void SendOwt_CorporateManagedAccountStatement_Success(final Currency currency) throws Exception {
        final Long depositAmount = 10000L;
        final long sendAmount = 200L;
        final int fee = TestHelper.getFees(currency.name())
                .get(FeeType.SEPA_OWT_FEE).getAmount().intValue();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, currency.name(), secretKey, corporateAuthenticationToken);

        final int preSendBalance = simulateManagedAccountDepositAndCheckBalance(managedAccountId,
                currency.name(),
                depositAmount,
                secretKey,
                corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        currency.name(), sendAmount, OwtType.SEPA).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        final ValidatableResponse response =
                PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(response, createOutgoingWireTransferModel, OwtType.SEPA, corporateAuthenticationToken, sharedKey);

        final String outgoingWireTransferId = response.extract().jsonPath().getString("id");

        final int currentBalance =
                preSendBalance - (int) sendAmount - fee;

        assertManagedAccountBalance(managedAccountId, corporateAuthenticationToken, currentBalance);

        final SepaBankDetailsModel sepaBankDetails =
                (SepaBankDetailsModel) createOutgoingWireTransferModel.getDestinationBeneficiary().getBankAccountDetails();

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, corporateAuthenticationToken, 3)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", equalTo(outgoingWireTransferId))
                .body("entry[0].transactionId.type", equalTo("OUTGOING_WIRE_TRANSFER"))
                .body("entry[0].transactionAmount.currency", equalTo(currency.name()))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact((int)(sendAmount))))
                .body("entry[0].balanceAfter.currency", equalTo(currency.name()))
                .body("entry[0].balanceAfter.amount", equalTo(currentBalance))
                .body("entry[0].cardholderFee.currency", equalTo(currency.name()))
                .body("entry[0].cardholderFee.amount", equalTo(fee))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.description", equalTo(createOutgoingWireTransferModel.getDescription()))
                .body("entry[0].additionalFields.beneficiaryName", equalTo(createOutgoingWireTransferModel.getDestinationBeneficiary().getName()))
                .body("entry[0].additionalFields.beneficiaryAccount", equalTo(sepaBankDetails.getIban()))
                .body("entry[0].additionalFields.beneficiaryBankCode", equalTo(sepaBankDetails.getBankIdentifierCode()))
                .body("entry[0].availableBalanceAfter.currency", equalTo(currency.name()))
                .body("entry[0].availableBalanceAfter.amount", equalTo(currentBalance))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(currency.name()))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].actualBalanceAfter.currency", equalTo(currency.name()))
                .body("entry[0].actualBalanceAfter.amount", equalTo(currentBalance))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(currency.name()))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(Math.negateExact((int) sendAmount + fee)))
                .body("entry[0].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[1].transactionId.id", equalTo(outgoingWireTransferId))
                .body("entry[1].transactionId.type", equalTo("OUTGOING_WIRE_TRANSFER"))
                .body("entry[1].transactionAmount.currency", equalTo(currency.name()))
                .body("entry[1].transactionAmount.amount", equalTo(Math.negateExact((int)(sendAmount))))
                .body("entry[1].balanceAfter.currency", equalTo(currency.name()))
                .body("entry[1].balanceAfter.amount", equalTo(currentBalance))
                .body("entry[1].cardholderFee.currency", equalTo(currency.name()))
                .body("entry[1].cardholderFee.amount", equalTo(fee))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].additionalFields.description", equalTo(createOutgoingWireTransferModel.getDescription()))
                .body("entry[1].additionalFields.beneficiaryName", equalTo(createOutgoingWireTransferModel.getDestinationBeneficiary().getName()))
                .body("entry[1].additionalFields.beneficiaryAccount", equalTo(sepaBankDetails.getIban()))
                .body("entry[1].additionalFields.beneficiaryBankCode", equalTo(sepaBankDetails.getBankIdentifierCode()))
                .body("entry[1].availableBalanceAfter.currency", equalTo(currency.name()))
                .body("entry[1].availableBalanceAfter.amount", equalTo(currentBalance))
                .body("entry[1].availableBalanceAdjustment.currency", equalTo(currency.name()))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(Math.negateExact((int) sendAmount + fee)))
                .body("entry[1].actualBalanceAfter.currency", equalTo(currency.name()))
                .body("entry[1].actualBalanceAfter.amount", equalTo(preSendBalance))
                .body("entry[1].actualBalanceAdjustment.currency", equalTo(currency.name()))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].entryState", equalTo(State.PENDING.name()))
                .body("entry[2].transactionId.id", notNullValue())
                .body("entry[2].transactionId.type", equalTo("DEPOSIT"))
                .body("entry[2].transactionAmount.currency", equalTo(currency.name()))
                .body("entry[2].transactionAmount.amount", equalTo(depositAmount.intValue()))
                .body("entry[2].balanceAfter.currency", equalTo(currency.name()))
                .body("entry[2].balanceAfter.amount", equalTo(preSendBalance))
                .body("entry[2].cardholderFee.currency", equalTo(currency.name()))
                .body("entry[2].cardholderFee.amount", equalTo(TestHelper.getFees(currency.name()).get(FeeType.DEPOSIT_FEE).getAmount().intValue()))
                .body("entry[2].processedTimestamp", notNullValue())
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class, names = { "EUR" })
    public void SendOwt_ConsumerManagedAccountStatement_Success(final Currency currency) throws Exception {
        final Long depositAmount = 10000L;
        final long sendAmount = 200L;
        final int fee = TestHelper.getFees(currency.name())
                .get(FeeType.SEPA_OWT_FEE).getAmount().intValue();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(consumerManagedAccountProfileId, currency.name(), secretKey, consumerAuthenticationToken);

        final int preSendBalance = simulateManagedAccountDepositAndCheckBalance(managedAccountId,
                currency.name(),
                depositAmount,
                secretKey,
                consumerAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        currency.name(), sendAmount, OwtType.SEPA).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        final ValidatableResponse response =
                PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(response, createOutgoingWireTransferModel, OwtType.SEPA, corporateAuthenticationToken, sharedKey);

        final String outgoingWireTransferId = response.extract().jsonPath().getString("id");

        final int currentBalance =
                preSendBalance - (int) sendAmount - fee;

        assertManagedAccountBalance(managedAccountId, consumerAuthenticationToken, currentBalance);

        final SepaBankDetailsModel sepaBankDetails =
                (SepaBankDetailsModel) createOutgoingWireTransferModel.getDestinationBeneficiary().getBankAccountDetails();

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKey, consumerAuthenticationToken, 3)
                .then()
                .statusCode(SC_OK)
                .body("entry[0].transactionId.id", equalTo(outgoingWireTransferId))
                .body("entry[0].transactionId.type", equalTo("OUTGOING_WIRE_TRANSFER"))
                .body("entry[0].transactionAmount.currency", equalTo(currency.name()))
                .body("entry[0].transactionAmount.amount", equalTo(Math.negateExact((int)(sendAmount))))
                .body("entry[0].balanceAfter.currency", equalTo(currency.name()))
                .body("entry[0].balanceAfter.amount", equalTo(currentBalance))
                .body("entry[0].cardholderFee.currency", equalTo(currency.name()))
                .body("entry[0].cardholderFee.amount", equalTo(fee))
                .body("entry[0].processedTimestamp", notNullValue())
                .body("entry[0].additionalFields.description", equalTo(createOutgoingWireTransferModel.getDescription()))
                .body("entry[0].additionalFields.beneficiaryName", equalTo(createOutgoingWireTransferModel.getDestinationBeneficiary().getName()))
                .body("entry[0].additionalFields.beneficiaryAccount", equalTo(sepaBankDetails.getIban()))
                .body("entry[0].additionalFields.beneficiaryBankCode", equalTo(sepaBankDetails.getBankIdentifierCode()))
                .body("entry[0].availableBalanceAfter.currency", equalTo(currency.name()))
                .body("entry[0].availableBalanceAfter.amount", equalTo(currentBalance))
                .body("entry[0].availableBalanceAdjustment.currency", equalTo(currency.name()))
                .body("entry[0].availableBalanceAdjustment.amount", equalTo(0))
                .body("entry[0].actualBalanceAfter.currency", equalTo(currency.name()))
                .body("entry[0].actualBalanceAfter.amount", equalTo(currentBalance))
                .body("entry[0].actualBalanceAdjustment.currency", equalTo(currency.name()))
                .body("entry[0].actualBalanceAdjustment.amount", equalTo(Math.negateExact((int) sendAmount + fee)))
                .body("entry[0].entryState", equalTo(State.COMPLETED.name()))
                .body("entry[1].transactionId.id", equalTo(outgoingWireTransferId))
                .body("entry[1].transactionId.type", equalTo("OUTGOING_WIRE_TRANSFER"))
                .body("entry[1].transactionAmount.currency", equalTo(currency.name()))
                .body("entry[1].transactionAmount.amount", equalTo(Math.negateExact((int)(sendAmount))))
                .body("entry[1].balanceAfter.currency", equalTo(currency.name()))
                .body("entry[1].balanceAfter.amount", equalTo(currentBalance))
                .body("entry[1].cardholderFee.currency", equalTo(currency.name()))
                .body("entry[1].cardholderFee.amount", equalTo(fee))
                .body("entry[1].processedTimestamp", notNullValue())
                .body("entry[1].additionalFields.description", equalTo(createOutgoingWireTransferModel.getDescription()))
                .body("entry[1].additionalFields.beneficiaryName", equalTo(createOutgoingWireTransferModel.getDestinationBeneficiary().getName()))
                .body("entry[1].additionalFields.beneficiaryAccount", equalTo(sepaBankDetails.getIban()))
                .body("entry[1].additionalFields.beneficiaryBankCode", equalTo(sepaBankDetails.getBankIdentifierCode()))
                .body("entry[1].availableBalanceAfter.currency", equalTo(currency.name()))
                .body("entry[1].availableBalanceAfter.amount", equalTo(currentBalance))
                .body("entry[1].availableBalanceAdjustment.currency", equalTo(currency.name()))
                .body("entry[1].availableBalanceAdjustment.amount", equalTo(Math.negateExact((int) sendAmount + fee)))
                .body("entry[1].actualBalanceAfter.currency", equalTo(currency.name()))
                .body("entry[1].actualBalanceAfter.amount", equalTo(preSendBalance))
                .body("entry[1].actualBalanceAdjustment.currency", equalTo(currency.name()))
                .body("entry[1].actualBalanceAdjustment.amount", equalTo(0))
                .body("entry[1].entryState", equalTo(State.PENDING.name()))
                .body("entry[2].transactionId.id", notNullValue())
                .body("entry[2].transactionId.type", equalTo("DEPOSIT"))
                .body("entry[2].transactionAmount.currency", equalTo(currency.name()))
                .body("entry[2].transactionAmount.amount", equalTo(depositAmount.intValue()))
                .body("entry[2].balanceAfter.currency", equalTo(currency.name()))
                .body("entry[2].balanceAfter.amount", equalTo(preSendBalance))
                .body("entry[2].cardholderFee.currency", equalTo(currency.name()))
                .body("entry[2].cardholderFee.amount", equalTo(TestHelper.getFees(currency.name()).get(FeeType.DEPOSIT_FEE).getAmount().intValue()))
                .body("entry[2].processedTimestamp", notNullValue())
                .body("count", equalTo(3))
                .body("responseCount", equalTo(3));
    }

    @Test
    public void SendOwt_UnknownCurrency_CurrencyMismatch() throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.builder()
                        .tag(RandomStringUtils.randomAlphabetic(5))
                        .description(RandomStringUtils.randomAlphabetic(5))
                        .profileId(outgoingWireTransfersProfileId)
                        .destinationBeneficiary(Beneficiary.DefaultBeneficiaryWithSepa().build())
                        .sourceInstrument(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .transferAmount(new CurrencyAmount("ABC", 10L))
                        .build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CURRENCY_MISMATCH"));
    }

    @Test
    public void SendOwt_InvalidCurrency_CurrencyMismatch() throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.builder()
                        .tag(RandomStringUtils.randomAlphabetic(5))
                        .description(RandomStringUtils.randomAlphabetic(5))
                        .profileId(outgoingWireTransfersProfileId)
                        .destinationBeneficiary(Beneficiary.DefaultBeneficiaryWithSepa().build())
                        .sourceInstrument(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .transferAmount(new CurrencyAmount("ABCD", 10L))
                        .build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(longs = { 0, -1 })
    public void SendOwt_InvalidAmount_BadRequest(final long amount) throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.builder()
                        .tag(RandomStringUtils.randomAlphabetic(5))
                        .description(RandomStringUtils.randomAlphabetic(5))
                        .profileId(outgoingWireTransfersProfileId)
                        .destinationBeneficiary(Beneficiary.DefaultBeneficiaryWithSepa().build())
                        .sourceInstrument(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .transferAmount(new CurrencyAmount(corporateCurrency, amount))
                        .build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("AMOUNT_INVALID"));
    }

    @Test
    public void SendOwt_FasterPayments_Conflict() throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);

        fundManagedAccount(managedAccountId, corporateCurrency, 1000L);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        corporateCurrency, 200L, OwtType.FASTER_PAYMENTS).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_CONFLICT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567890", "MT121234567890", "MT121234567890123456789012345678901"})
    public void SendOwt_InvalidBankAccountIban_BadRequest(final String iban) throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccountId,
                        corporateCurrency, 200L, OwtType.SEPA)
                        .destinationBeneficiary(Beneficiary.DefaultBeneficiary()
                                .setBankAccountDetails(new SepaBankDetailsModel(iban, "TS12TEST123"))
                                .build())
                        .build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567890", "MT12abcD1234"})
    public void SendOwt_InvalidBankAccountBic_BadRequest(final String bic) throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccountId,
                        corporateCurrency, 200L, OwtType.SEPA)
                        .destinationBeneficiary(Beneficiary.DefaultBeneficiary()
                                .setBankAccountDetails(new SepaBankDetailsModel("TS123123123213213123", bic))
                                .build())
                        .build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcdefgh", "1234567", "123456789"})
    public void SendOwt_InvalidBankAccountNumber_BadRequest(final String accountNumber) throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, Currency.GBP.name(), secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccountId,
                        Currency.GBP.name(), 200L, OwtType.FASTER_PAYMENTS)
                        .destinationBeneficiary(Beneficiary.DefaultBeneficiary()
                                .setBankAccountDetails(new FasterPaymentsBankDetailsModel(accountNumber, RandomStringUtils.randomNumeric(6)))
                                .build())
                        .build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcdef", "12345", "1234567"})
    public void SendOwt_InvalidBankAccountSortCode_BadRequest(final String sortCode) throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, Currency.GBP.name(), secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId, managedAccountId,
                        Currency.GBP.name(), 200L, OwtType.FASTER_PAYMENTS)
                        .destinationBeneficiary(Beneficiary.DefaultBeneficiary()
                                .setBankAccountDetails(new FasterPaymentsBankDetailsModel(RandomStringUtils.randomNumeric(8), sortCode))
                                .build())
                        .build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendOwt_ManagedCardAsSource_Conflict() throws Exception {

        final String managedCardId =
                ManagedCardsHelper.createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId, corporateCurrency, secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedCardId,
                        corporateCurrency, 200L, OwtType.SEPA).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SOURCE_NOT_FOUND"));
    }

    @Test
    public void SendOwt_InvalidApiKey_Unauthorised() throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, secretKey, consumerAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        consumerCurrency, 100L, OwtType.SEPA).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void SendOwt_NoApiKey_BadRequest() throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(consumerManagedAccountProfileId, consumerCurrency, secretKey, consumerAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        consumerCurrency, 100L, OwtType.SEPA).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendOwt_DifferentInnovatorApiKey_Forbidden() throws Exception {

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        consumerCurrency, 100L, OwtType.SEPA).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void SendOwt_RootUserLoggedOut_Unauthorised() throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);

        AuthenticationService.logout(secretKey, consumerAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        consumerCurrency, 100L, OwtType.SEPA).build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void SendOwt_SameIdempotencyRefSamePayload_Success() throws Exception {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;
        final int fee = TestHelper.getFees(createCorporateModel.getBaseCurrency())
                .get(FeeType.SEPA_OWT_FEE).getAmount().intValue();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(), secretKey, authenticatedCorporate.getRight());

        fundManagedAccount(managedAccountId, createCorporateModel.getBaseCurrency(), depositAmount);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        createCorporateModel.getBaseCurrency(), sendAmount, OwtType.SEPA).build();

        responses.add(PaymentInitiationService.createOutgoingWireTransfer(sharedKey,
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel)))),
                createOutgoingWireTransferModel, idempotencyReference));
        responses.add(PaymentInitiationService.createOutgoingWireTransfer(sharedKey,
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel)))),
                createOutgoingWireTransferModel, idempotencyReference));

        responses.forEach(response ->
                assertSuccessfulResponse(response
                        .then()
                        .statusCode(SC_OK), createOutgoingWireTransferModel, OwtType.SEPA, authenticatedCorporate.getRight(), sharedKey));

        assertEquals(responses.get(0).jsonPath().getString("id"), responses.get(1).jsonPath().getString("id"));
        assertEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(1).jsonPath().getString("creationTimestamp"));
        assertEquals("1",
                OutgoingWireTransfersService.getOutgoingWireTransfers(secretKey, Optional.empty(), authenticatedCorporate.getRight()).jsonPath().getString("count"));

        final int currentBalance =
                depositAmount.intValue() - sendAmount.intValue() - fee;

        assertManagedAccountBalance(managedAccountId, authenticatedCorporate.getRight(), currentBalance);
    }

    @Test
    public void SendOwt_SameIdempotencyRefDifferentPayload_PreconditionFailed() throws Exception {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);

        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(),
                        secretKey, authenticatedCorporate.getRight());

        fundManagedAccount(managedAccountId, createCorporateModel.getBaseCurrency(), depositAmount);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        createCorporateModel.getBaseCurrency(), sendAmount, OwtType.SEPA).build();

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel1 =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        createCorporateModel.getBaseCurrency(), sendAmount, OwtType.SEPA)
                        .tag(RandomStringUtils.randomAlphabetic(5))
                        .build();

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey,
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel)))),
                createOutgoingWireTransferModel, idempotencyReference)
                .then()
                .statusCode(SC_OK);

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey,
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel1)))),
                createOutgoingWireTransferModel, idempotencyReference)
                .then()
                .statusCode(SC_PRECONDITION_FAILED);
    }

    @Test
    public void SendOwt_DifferentIdempotencyRef_Success() throws Exception {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.EUR.name())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        CorporatesHelper.verifyKyb(secretKey, corporateUserId);

        final Long depositAmount = 10000L;
        final Long sendAmount = 200L;

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, createCorporateModel.getBaseCurrency(),
                        secretKey, authenticatedCorporate.getRight());

        fundManagedAccount(managedAccountId, createCorporateModel.getBaseCurrency(), depositAmount);

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final String idempotencyReference1 = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        createCorporateModel.getBaseCurrency(), sendAmount, OwtType.SEPA).build();

        responses.add(PaymentInitiationService.createOutgoingWireTransfer(sharedKey,
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel)))),
                createOutgoingWireTransferModel, idempotencyReference));
        responses.add(PaymentInitiationService.createOutgoingWireTransfer(sharedKey,
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel)))),
                createOutgoingWireTransferModel, idempotencyReference1));
        responses.add(PaymentInitiationService.createOutgoingWireTransfer(sharedKey,
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel)))),
                createOutgoingWireTransferModel));

        responses.forEach(response ->
                response.then()
                        .statusCode(SC_OK));

        assertNotEquals(responses.get(0).jsonPath().getString("id"), responses.get(1).jsonPath().getString("id"));
        assertNotEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(1).jsonPath().getString("creationTimestamp"));
        assertNotEquals(responses.get(1).jsonPath().getString("id"), responses.get(2).jsonPath().getString("id"));
        assertNotEquals(responses.get(1).jsonPath().getString("creationTimestamp"), responses.get(2).jsonPath().getString("creationTimestamp"));
        assertNotEquals(responses.get(0).jsonPath().getString("id"), responses.get(2).jsonPath().getString("id"));
        assertNotEquals(responses.get(0).jsonPath().getString("creationTimestamp"), responses.get(2).jsonPath().getString("creationTimestamp"));
        assertEquals("3",
                OutgoingWireTransfersService.getOutgoingWireTransfers(secretKey, Optional.empty(),
                        authenticatedCorporate.getRight()).jsonPath().getString("count"));
    }

    @Test
    public void SendOwt_InvalidDescriptionLengthSepa_BadRequest() throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        corporateCurrency, 100L, OwtType.SEPA)
                        .description(RandomStringUtils.randomAlphabetic(36))
                        .build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request: description size must be between 0 and 35"));
    }

    @Test
    public void SendOwt_InvalidDescriptionLengthFasterPayments_BadRequest() throws Exception {

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId,
                        corporateCurrency, secretKey, corporateAuthenticationToken);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        corporateCurrency, 100L, OwtType.FASTER_PAYMENTS)
                        .description(RandomStringUtils.randomAlphabetic(19))
                        .build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request: description size must be between 0 and 18"));
    }

    @Test
    public void SendOwt_SepaLimitCheck_BadRequest() throws Exception {

        final String currency = Currency.EUR.name();
        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, currency,
                        secretKey, corporateAuthenticationToken);

        AdminHelper.fundManagedAccount(innovatorId, managedAccountId, currency, 20000L);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        currency, 8001L, OwtType.SEPA)
                        .build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("AMOUNT_INVALID"));
    }

    @Test
    public void SendOwt_FasterPaymentsLimitCheck_BadRequest() throws Exception {

        final String currency = Currency.GBP.name();
        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, currency,
                        secretKey, corporateAuthenticationToken);

        AdminHelper.fundManagedAccount(innovatorId, managedAccountId, currency, 20000L);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        currency, 10001L, OwtType.FASTER_PAYMENTS)
                        .build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("AMOUNT_INVALID"));
    }

    @ParameterizedTest
    @MethodSource("owtSuccessfulArgs")
    public void SendOwt_LimitChecks_Success(final Currency currency, final OwtType type, final FeeType feeType) throws Exception {

        final Long depositAmount = 20000L;
        final Long sendAmount = currency.equals(Currency.EUR) ? 8000L : 10000L;

        final int fee = TestHelper.getFees(currency.name()).get(feeType).getAmount().intValue();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(corporateManagedAccountProfileId, currency.name(),
                        secretKey, corporateAuthenticationToken);

        fundManagedAccount(managedAccountId, currency.name(), depositAmount);

        final CreateOutgoingWireTransferModel createOutgoingWireTransferModel =
                CreateOutgoingWireTransferModel.DefaultOutgoingWireTransfersModel(outgoingWireTransfersProfileId,
                        managedAccountId,
                        currency.name(), sendAmount, type)
                        .build();

        final Map<String, String> headers =
                OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(),
                        DIGEST, Optional.of(OpenBankingHelper.generateBodyDigest(createOutgoingWireTransferModel))));

        final ValidatableResponse response =
                PaymentInitiationService.createOutgoingWireTransfer(sharedKey, headers, createOutgoingWireTransferModel)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(response, createOutgoingWireTransferModel, type, corporateAuthenticationToken, sharedKey);

        final int currentBalance =
                depositAmount.intValue() - sendAmount.intValue() - fee;

        assertManagedAccountBalance(managedAccountId, corporateAuthenticationToken, currentBalance);
    }

    private void  assertSuccessfulResponse(final ValidatableResponse response,
                                          final CreateOutgoingWireTransferModel createOutgoingWireTransferModel,
                                          final OwtType owtType,
                                          final String token,
                                          final String sharedKey) {
        response.body("consent.id", notNullValue())
                .body("consent.state", equalTo("AWAITING_AUTHORISATION"))
                .body("consent.tppId",equalTo(tppId))
                .body("consent.tppName",equalTo("Test"))
                .body("consent.links.redirect", equalTo(String.format("https://openbanking.weavr.io/consent?programmeKey=%s&scope=PAYMENT_INITIATION&consentId=%s&tppId=%s&paymentType=OUTGOING_WIRE_TRANSFER",
                        URLEncoder.encode(sharedKey, StandardCharsets.UTF_8), response.extract().jsonPath().getString("consent.id"), tppId)))
                .body("consent.lastUpdated", notNullValue())
                .body("consent.expiry", notNullValue())
                .body("consent.createdTimestamp", notNullValue());

        response.body("paymentRequest.profileId", equalTo(createOutgoingWireTransferModel.getProfileId()))
                .body("paymentRequest.tag", equalTo(createOutgoingWireTransferModel.getTag()))
                .body("paymentRequest.sourceInstrument.type", equalTo(createOutgoingWireTransferModel.getSourceInstrument().getType()))
                .body("paymentRequest.sourceInstrument.id", equalTo(createOutgoingWireTransferModel.getSourceInstrument().getId()))
                .body("paymentRequest.transferAmount.currency", equalTo(createOutgoingWireTransferModel.getTransferAmount().getCurrency()))
                .body("paymentRequest.transferAmount.amount", equalTo(createOutgoingWireTransferModel.getTransferAmount().getAmount().intValue()))
                .body("paymentRequest.description", equalTo(createOutgoingWireTransferModel.getDescription()))
                .body("paymentRequest.destinationBeneficiary.name", equalTo(createOutgoingWireTransferModel.getDestinationBeneficiary().getName()))
                .body("paymentRequest.destinationBeneficiary.address", equalTo(createOutgoingWireTransferModel.getDestinationBeneficiary().getAddress()))
                .body("paymentRequest.destinationBeneficiary.bankName", equalTo(createOutgoingWireTransferModel.getDestinationBeneficiary().getBankName()))
                .body("paymentRequest.destinationBeneficiary.bankAddress", equalTo(createOutgoingWireTransferModel.getDestinationBeneficiary().getBankAddress()))
                .body("paymentRequest.destinationBeneficiary.bankCountry", equalTo(createOutgoingWireTransferModel.getDestinationBeneficiary().getBankCountry()));

        OpenBankingSecureServiceHelper.authoriseAndVerifyPayment(sharedKey, token, tppId, response.extract().jsonPath().getString("consent.id"));

        OutgoingWireTransfersHelper
                .checkOwtStateByAccountId(createOutgoingWireTransferModel.getSourceInstrument().getId(),"COMPLETED");

        assertBankAccountDetails(response, createOutgoingWireTransferModel, owtType);
    }

    private void assertBankAccountDetails(final ValidatableResponse response,
                                          final CreateOutgoingWireTransferModel createOutgoingWireTransferModel,
                                          final OwtType owtType) {
        switch (owtType){
            case SEPA:
                final SepaBankDetailsModel sepaBankDetails =
                        (SepaBankDetailsModel) createOutgoingWireTransferModel.getDestinationBeneficiary().getBankAccountDetails();

                response
                        .body("paymentRequest.destinationBeneficiary.bankAccountDetails.iban", equalTo(sepaBankDetails.getIban()))
                        .body("paymentRequest.destinationBeneficiary.bankAccountDetails.bankIdentifierCode", equalTo(sepaBankDetails.getBankIdentifierCode()));
                break;
            case FASTER_PAYMENTS:
                final FasterPaymentsBankDetailsModel fasterPaymentsBankDetails =
                        (FasterPaymentsBankDetailsModel) createOutgoingWireTransferModel.getDestinationBeneficiary().getBankAccountDetails();

                response
                        .body("paymentRequest.destinationBeneficiary.bankAccountDetails.accountNumber", equalTo(fasterPaymentsBankDetails.getAccountNumber()))
                        .body("paymentRequest.destinationBeneficiary.bankAccountDetails.sortCode", equalTo(fasterPaymentsBankDetails.getSortCode()));
                break;
            default: throw new IllegalArgumentException("OWT type not supported.");
        }
    }

    private void assertManagedAccountBalance(final String managedAccountId, final String token, final int balance){
        assertManagedAccountBalance(managedAccountId, token, secretKey, balance);
    }

    private void assertManagedAccountBalance(final String managedAccountId, final String token, final String secretKey, final int balance){
        ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, token)
                .then()
                .statusCode(SC_OK)
                .body("balances.availableBalance", equalTo(balance))
                .body("balances.actualBalance", equalTo(balance));
    }

    private static Stream<Arguments> owtSuccessfulArgs() {
        return Stream.of(Arguments.of(Currency.EUR, OwtType.SEPA, FeeType.SEPA_OWT_FEE),
                Arguments.of(Currency.GBP, OwtType.FASTER_PAYMENTS, FeeType.FASTER_PAYMENTS_OWT_FEE));
    }

    private static void fundManagedAccount(final String managedAccountId,
                                           final String currency,
                                           final Long depositAmount){
        AdminHelper.fundManagedAccount(innovatorId, managedAccountId, currency, depositAmount);
    }

    private static int simulateManagedAccountDepositAndCheckBalance(final String managedAccountId,
                                                                    final String currency,
                                                                    final Long depositAmount,
                                                                    final String secretKey,
                                                                    final String authenticationToken){
        TestHelper.simulateManagedAccountDeposit(managedAccountId, currency, depositAmount, secretKey, authenticationToken);

        final int balance = (int) (depositAmount - TestHelper.getFees(currency).get(FeeType.DEPOSIT_FEE).getAmount());

        ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, authenticationToken)
                .then()
                .body("balances.availableBalance", equalTo(balance))
                .body("balances.actualBalance", equalTo(balance));

        return balance;
    }
}
