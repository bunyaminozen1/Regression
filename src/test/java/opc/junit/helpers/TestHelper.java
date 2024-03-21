package opc.junit.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.bjansen.ssv.SwaggerValidator;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import commons.config.ConfigHelper;
import commons.enums.AuthenticationType;
import io.restassured.response.Response;
import io.swagger.util.Json;
import lombok.SneakyThrows;
import opc.enums.opc.ApiDocument;
import opc.enums.opc.ApiSchemaDefinition;
import opc.enums.opc.FeeType;
import opc.helpers.ModelHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.innovator.CreateProgrammeModel;
import opc.models.innovator.InnovatorRegistrationModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.transfers.TransferFundsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.LoginModel;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.simulator.SimulateDepositByIbanModel;
import opc.models.simulator.SimulateDepositModel;
import opc.models.simulator.SimulatePendingDepositModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedAccountsService;
import opc.services.multi.TransfersService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import org.testcontainers.shaded.org.apache.commons.io.filefilter.PrefixFileFilter;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.IntStream;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.SC_OK;

public class TestHelper {

    public static String DEFAULT_PASSWORD = "Pass1234";
    public static String DEFAULT_COMPLEX_PASSWORD = "Pass1234!";
    public static String DEFAULT_PASSCODE = "1234";
    public static String DEFAULT_INNOVATOR_PASSWORD = "Pass1234!";
    public static String VERIFICATION_CODE = ConfigHelper.getEnvironmentConfiguration().getVerificationCode();
    public static String OTP_VERIFICATION_CODE = ConfigHelper.getEnvironmentConfiguration().getOtpVerificationCode();

    public static String getDefaultPassword(final String secretKey) {

        final AuthenticationType authenticationType =
                AuthenticationType.valueOf(getFullProgrammeConfiguration()
                        .stream().filter(x -> x.getSecretKey().equals(secretKey))
                        .findFirst().orElseThrow().getAuthenticationType());

        switch (authenticationType) {
            case PASSCODE:
                return DEFAULT_PASSCODE;
            case PASSWORD:
                return DEFAULT_PASSWORD;
            case COMPLEX_PASSWORD:
                return DEFAULT_COMPLEX_PASSWORD;
            default: throw new IllegalArgumentException("Authentication type not found.");
        }
    }

    private static List<ProgrammeDetailsModel> getFullProgrammeConfiguration() {
        final List<File> files =
                new ArrayList<>(FileUtils.listFiles(new File("./src/test/resources/TestConfiguration/"),
                        new PrefixFileFilter(ConfigHelper.getEnvironmentConfiguration().getMainTestEnvironment()), null));

        final List<ProgrammeDetailsModel> programmes = new ArrayList<>();

        files.forEach(file -> {
            try {
                programmes.addAll(Arrays.asList(new ObjectMapper()
                        .readValue(file,ProgrammeDetailsModel[].class)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return programmes;
    }

    public static Triple<String, String, String> registerLoggedInInnovatorWithProgramme() {

        final InnovatorRegistrationModel registrationModel =
                InnovatorRegistrationModel.RandomInnovatorRegistrationModel()
                        .build();

        final String innovatorId =
                ensureAsExpected(5, () -> InnovatorService.registerInnovator(registrationModel), SC_OK)
                        .jsonPath()
                        .get("innovatorId");

        final String token =
                ensureAsExpected(5, () -> InnovatorService.loginInnovator(new LoginModel(registrationModel.getEmail(),
                        registrationModel.getPassword())), SC_OK)
                        .jsonPath()
                        .get("token");

        final String programmeId =
                ensureAsExpected(5,
                        () -> InnovatorService.createProgramme(CreateProgrammeModel.InitialProgrammeModel(), token),
                        SC_OK)
                        .jsonPath()
                        .get("id");

        return Triple.of(innovatorId, programmeId, token);
    }

    public static Map<FeeType, CurrencyAmount> getFees(final String currency){
        final Map<FeeType, CurrencyAmount> fees = new HashMap<>();

        switch (currency) {
            case "EUR":
                fees.put(FeeType.DEPOSIT_FEE, new CurrencyAmount(currency, 110L));
                fees.put(FeeType.CHARGE_FEE, new CurrencyAmount(currency, 103L));
                fees.put(FeeType.MA_TO_MA_TRANSFER_FEE, new CurrencyAmount(currency, 100L));
                fees.put(FeeType.MA_TO_MC_TRANSFER_FEE, new CurrencyAmount(currency, 100L));
                fees.put(FeeType.MC_TO_MA_TRANSFER_FEE, new CurrencyAmount(currency, 100L));
                fees.put(FeeType.MC_TO_MC_TRANSFER_FEE, new CurrencyAmount(currency, 100L));
                fees.put(FeeType.MA_TO_MA_SEND_FEE, new CurrencyAmount(currency, 100L));
                fees.put(FeeType.MA_TO_MC_SEND_FEE, new CurrencyAmount(currency, 100L));
                fees.put(FeeType.MC_TO_MA_SEND_FEE, new CurrencyAmount(currency, 100L));
                fees.put(FeeType.MC_TO_MC_SEND_FEE, new CurrencyAmount(currency, 100L));
                fees.put(FeeType.PURCHASE_FEE, new CurrencyAmount(currency, 12L));
                fees.put(FeeType.REFUND_FEE, new CurrencyAmount(currency, 11L));
                fees.put(FeeType.SEPA_OWT_FEE, new CurrencyAmount(currency, 101L));
                fees.put(FeeType.FASTER_PAYMENTS_OWT_FEE, new CurrencyAmount(currency, 100L));
                fees.put(FeeType.ATM_FEE, new CurrencyAmount(currency, 15L));
                break;

            case "GBP":
                fees.put(FeeType.DEPOSIT_FEE, new CurrencyAmount(currency, 117L));
                fees.put(FeeType.CHARGE_FEE, new CurrencyAmount(currency, 108L));
                fees.put(FeeType.MA_TO_MA_TRANSFER_FEE, new CurrencyAmount(currency, 102L));
                fees.put(FeeType.MA_TO_MC_TRANSFER_FEE, new CurrencyAmount(currency, 102L));
                fees.put(FeeType.MC_TO_MA_TRANSFER_FEE, new CurrencyAmount(currency, 102L));
                fees.put(FeeType.MC_TO_MC_TRANSFER_FEE, new CurrencyAmount(currency, 102L));
                fees.put(FeeType.MA_TO_MA_SEND_FEE, new CurrencyAmount(currency, 102L));
                fees.put(FeeType.MA_TO_MC_SEND_FEE, new CurrencyAmount(currency, 102L));
                fees.put(FeeType.MC_TO_MA_SEND_FEE, new CurrencyAmount(currency, 102L));
                fees.put(FeeType.MC_TO_MC_SEND_FEE, new CurrencyAmount(currency, 102L));
                fees.put(FeeType.PURCHASE_FEE, new CurrencyAmount(currency, 10L));
                fees.put(FeeType.REFUND_FEE, new CurrencyAmount(currency, 9L));
                fees.put(FeeType.SEPA_OWT_FEE, new CurrencyAmount(currency, 103L));
                fees.put(FeeType.FASTER_PAYMENTS_OWT_FEE, new CurrencyAmount(currency, 102L));
                fees.put(FeeType.ATM_FEE, new CurrencyAmount(currency, 19L));
                break;

            case "USD":
                fees.put(FeeType.DEPOSIT_FEE, new CurrencyAmount(currency, 115L));
                fees.put(FeeType.CHARGE_FEE, new CurrencyAmount(currency, 106L));
                fees.put(FeeType.MA_TO_MA_TRANSFER_FEE, new CurrencyAmount(currency, 105L));
                fees.put(FeeType.MA_TO_MC_TRANSFER_FEE, new CurrencyAmount(currency, 105L));
                fees.put(FeeType.MC_TO_MA_TRANSFER_FEE, new CurrencyAmount(currency, 105L));
                fees.put(FeeType.MC_TO_MC_TRANSFER_FEE, new CurrencyAmount(currency, 105L));
                fees.put(FeeType.MA_TO_MA_SEND_FEE, new CurrencyAmount(currency, 105L));
                fees.put(FeeType.MA_TO_MC_SEND_FEE, new CurrencyAmount(currency, 105L));
                fees.put(FeeType.MC_TO_MA_SEND_FEE, new CurrencyAmount(currency, 105L));
                fees.put(FeeType.MC_TO_MC_SEND_FEE, new CurrencyAmount(currency, 105L));
                fees.put(FeeType.PURCHASE_FEE, new CurrencyAmount(currency, 14L));
                fees.put(FeeType.REFUND_FEE, new CurrencyAmount(currency, 13L));
                fees.put(FeeType.SEPA_OWT_FEE, new CurrencyAmount(currency, 107L));
                fees.put(FeeType.FASTER_PAYMENTS_OWT_FEE, new CurrencyAmount(currency, 105L));
                fees.put(FeeType.ATM_FEE, new CurrencyAmount(currency, 17L));
                break;

            default:
                throw new IllegalArgumentException("Currency not supported.");
        }
        return fees;
    }

    @SneakyThrows
    public static void validateSchemaDefinition(final ApiDocument apiDocument,
                                                final ApiSchemaDefinition apiSchemaDefinition,
                                                final String modelString) {
        final InputStreamReader spec =
                new InputStreamReader(Objects.requireNonNull(TestHelper.class.getResourceAsStream(String.format("/swagger/%s", apiDocument.getFilename()))));
        final SwaggerValidator validator = SwaggerValidator.forYamlSchema(spec);

        final JsonNode model =
                Json.mapper().readTree(modelString);

        final ProcessingReport report = validator.validate(model, String.format("/definitions/%s",
                apiSchemaDefinition.name()), true);

        if (!report.isSuccess()) {

            final StringBuilder failureMessage = new StringBuilder();

            report.forEach(message ->
                    failureMessage.append(System.getProperty("line.separator"))
                            .append(String.format("%s", message.asException().getMessage())));

            Assertions.fail(failureMessage.toString());
        }
    }

    public static Response ensureAsExpected(final int seconds,
                                            final Callable<Response> callableResponse,
                                            final int status) {
        final Response[] response = new Response[1];

        final BooleanSupplier booleanSupplier = () -> {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
                response[0] = callableResponse.call();
                return response[0].statusCode() == status;
            } catch (Exception e) {
                if (response[0] != null) {
                    response[0].then().log().all();
                }
                return false;
            }
        };

        if (!isConditionSatisfied(seconds, booleanSupplier)) {
            if (response[0] != null) {
                response[0].then().log().all();
            }
            Assertions.fail(String.format("Expected %s response code but was %s, check logged payloads.",
                    status, Objects.requireNonNull(response[0]).statusCode()));
        }

        return response[0];
    }

    public static Response ensureAsExpected(final int seconds,
                                            final Callable<Response> callableResponse,
                                            final Function<Response, Boolean> testCondition,
                                            final Optional<String> message) {

        final Response[] response = new Response[1];

        final BooleanSupplier booleanSupplier = () -> {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
                response[0] = callableResponse.call();
                return testCondition.apply(response[0]);
            } catch (Exception e) {
                if (response[0] != null) {
                    response[0].then().log().all();
                }
                return false;
            }
        };

        if (!isConditionSatisfied(seconds, booleanSupplier)) {
            if (response[0] != null) {
                response[0].then().log().all();
            }
            Assertions.fail(message.orElse("Issue with call, check logged payloads"));
        }

        return response[0];
    }

    public static Map<Integer, Map<String, String>> ensureDatabaseDataRetrieved(final int seconds,
                                                                                final Callable<Map<Integer, Map<String, String>>> callableResponse,
                                                                                final Function<Map<Integer, Map<String, String>>, Boolean> testCondition) {

        return ensureDatabaseResultAsExpected(seconds, callableResponse,
                testCondition, Optional.of("Number of rows returned not as expected"));
    }

    public static Map<Integer, Map<String, String>> ensureDatabaseResultAsExpected(final int seconds,
                                                                                   final Callable<Map<Integer, Map<String, String>>> callableResponse,
                                                                                   final Function<Map<Integer, Map<String, String>>, Boolean> testCondition,
                                                                                   final Optional<String> message) {

        final Map<Integer, Map<String, String>>[] data = new Map[]{new HashMap<>()};

        final BooleanSupplier booleanSupplier = () -> {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
                data[0] = callableResponse.call();
                return testCondition.apply(data[0]);
            }catch (Exception e){
                return false;
            }
        };

        return isConditionSatisfied(seconds, booleanSupplier) ?
                data[0] : Assertions.fail(message.orElse("Issue with the database call result."));
    }

    public static boolean isConditionSatisfied(final int seconds, final BooleanSupplier booleanSupplier) {
        long start = System.currentTimeMillis();
        while (!booleanSupplier.getAsBoolean()){
            if (System.currentTimeMillis() - start > (seconds * 1000L) ){
                return false;
            }
        }
        return true;
    }

    public static Pair<Integer, Integer> simulateManagedAccountDeposit(final String managedAccountId,
                                                                       final String currency,
                                                                       final long amount,
                                                                       final String secretKey,
                                                                       final String token) {
        return simulateManagedAccountDeposit(managedAccountId, currency, amount, secretKey, token, 1);
    }

    public static Pair<Integer, Integer> simulateManagedAccountPendingDeposit(final String managedAccountId,
                                                                              final String senderName,
                                                                              final String senderIban,
                                                                              final String currency,
                                                                              final long amount,
                                                                              final boolean webhook,
                                                                              final boolean isSepaInstant,
                                                                              final String reference,
                                                                              final boolean expectToApprove,
                                                                              final String innovatorId,
                                                                              final String secretKey,
                                                                              final String token) {
        return simulateManagedAccountPendingDeposit(managedAccountId,senderName, senderIban, currency, amount, webhook, isSepaInstant, reference, expectToApprove, innovatorId, secretKey, token, 1);
    }

    public static Pair<Integer, Integer> simulateMaPendingDepositByIbanId(final String ibanId,
                                                                                      final String managedAccountId,
                                                                                      final String senderName,
                                                                                      final String senderIban,
                                                                                      final String currency,
                                                                                      final long amount,
                                                                                      final boolean webhook,
                                                                                      final boolean isSepaInstant,
                                                                                      final String reference,
                                                                                      final boolean expectToApprove,
                                                                                      final String innovatorId,
                                                                                      final String secretKey,
                                                                                      final String token) {
        return simulateManagedAccountPendingDepositByIban(ibanId, managedAccountId,senderName, senderIban, currency, amount, webhook, isSepaInstant, reference, expectToApprove, innovatorId, secretKey, token, 1);
    }

    public static Pair<Integer, Integer> simulateManagedAccountDeposit(final String managedAccountId,
                                                                       final String currency,
                                                                       final long amount,
                                                                       final String secretKey,
                                                                       final String token,
                                                                       final int depositCount) {

        checkAndUpgradeIbanIfUnassigned(managedAccountId, secretKey, token);

        final SimulateDepositModel simulateDepositModel
                = SimulateDepositModel.defaultSimulateModel(new CurrencyAmount(currency, amount));

        final int[] expectedFinalAmount = {ManagedAccountsHelper.getManagedAccountAvailableBalance(managedAccountId, secretKey, token)};
        final int[] fee = new int[1];

        IntStream.range(0, depositCount).forEach(i -> {
            SimulatorService.simulateManagedAccountDeposit(simulateDepositModel, secretKey, managedAccountId);

            fee[0] = TestHelper.getFees(currency).get(FeeType.DEPOSIT_FEE).getAmount().intValue();

            expectedFinalAmount[0] += amount - fee[0];

            TestHelper. ensureAsExpected(120,
                    () -> ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, token),
                    x -> x.statusCode() == SC_OK && x.jsonPath().get("balances.availableBalance").equals(expectedFinalAmount[0]),
                    Optional.of(String.format("Expecting 200 with a balance value of %s for managed account %s, check logged payload",
                            expectedFinalAmount[0], managedAccountId)));
        });

        return Pair.of(ManagedAccountsHelper.getManagedAccountAvailableBalance(managedAccountId, secretKey, token), fee[0]);
    }



    public static Pair<Integer, Integer> simulateManagedAccountDeposit(final String managedAccountId,
                                                                       final SimulateDepositModel simulateDepositModel,
                                                                       final String secretKey,
                                                                       final String token) {

        checkAndUpgradeIbanIfUnassigned(managedAccountId, secretKey, token);

        final int[] expectedFinalAmount = {ManagedAccountsHelper.getManagedAccountAvailableBalance(managedAccountId, secretKey, token)};
        final int[] fee = new int[1];

        SimulatorService.simulateManagedAccountDeposit(simulateDepositModel, secretKey, managedAccountId);

        fee[0] = TestHelper.getFees(simulateDepositModel.getDepositAmount().getCurrency()).get(FeeType.DEPOSIT_FEE).getAmount().intValue();

        expectedFinalAmount[0] += (int) (simulateDepositModel.getDepositAmount().getAmount() - fee[0]);

        TestHelper. ensureAsExpected(120,
                () -> ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, token),
                x -> x.statusCode() == SC_OK && x.jsonPath().get("balances.availableBalance").equals(expectedFinalAmount[0]),
                Optional.of(String.format("Expecting 200 with a balance value of %s for managed account %s, check logged payload",
                        expectedFinalAmount[0], managedAccountId)));

        return Pair.of(ManagedAccountsHelper.getManagedAccountAvailableBalance(managedAccountId, secretKey, token), fee[0]);
    }

    public static Pair<Integer, Integer> simulateManagedAccountPendingDeposit(final String managedAccountId,
                                                                              final String senderName,
                                                                              final String senderIban,
                                                                              final String currency,
                                                                              final long amount,
                                                                              final boolean webhook,
                                                                              final boolean isSepaInstant,
                                                                              final String reference,
                                                                              final boolean expectToApprove,
                                                                              final String innovatorId,
                                                                              final String secretKey,
                                                                              final String token,
                                                                              final int depositCount) {
        checkAndUpgradeIbanIfUnassigned(managedAccountId, secretKey, token);

        final SimulatePendingDepositModel simulatePendingDepositModel =
            SimulatePendingDepositModel.builder()
                .senderName(senderName)
                .senderIban(senderIban)
                .depositAmount(new CurrencyAmount(currency, amount))
                .webhook(webhook)
                .immediateMonitorReplyExpected(isSepaInstant)
                .reference(reference)
            .build();

        final int[] expectedFinalAmount = {ManagedAccountsHelper.getManagedAccountAvailableBalance(managedAccountId, secretKey, token)};
        final int[] fee = new int[1];

        IntStream.range(0, depositCount).forEach(i -> {
            SimulatorService.simulateManagedAccountPendingDeposit(simulatePendingDepositModel, innovatorId, managedAccountId);

            fee[0] = TestHelper.getFees(currency).get(FeeType.DEPOSIT_FEE).getAmount().intValue();

            expectedFinalAmount[0] += amount - fee[0];

            TestHelper. ensureAsExpected(120,
                () -> ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, token),
                x -> x.statusCode() == SC_OK && x.jsonPath().get("balances.availableBalance").equals(expectToApprove? expectedFinalAmount[0] : 0),
                Optional.of(String.format("Expecting 200 with a balance value of %s for managed account %s, check logged payload",
                    expectToApprove? expectedFinalAmount[0] : 0, managedAccountId)));
        });

        return Pair.of(ManagedAccountsHelper.getManagedAccountAvailableBalance(managedAccountId, secretKey, token), fee[0]);
    }

    public static Pair<Integer, Integer> simulateManagedAccountPendingDepositByIban(final String iban,
                                                                                    final String managedAccountId,
                                                                                    final String senderName,
                                                                                    final String senderIban,
                                                                                    final String currency,
                                                                                    final long amount,
                                                                                    final boolean webhook,
                                                                                    final boolean isSepaInstant,
                                                                                    final String reference,
                                                                                    final boolean expectToApprove,
                                                                                    final String innovatorId,
                                                                                    final String secretKey,
                                                                                    final String token,
                                                                                    final int depositCount) {

        checkAndUpgradeIbanIfUnassigned(managedAccountId, secretKey, token);

        final SimulatePendingDepositModel simulatePendingDepositModel =
            SimulatePendingDepositModel.builder()
                .senderName(senderName)
                .senderIban(senderIban)
                .depositAmount(new CurrencyAmount(currency, amount))
                .webhook(webhook)
                .immediateMonitorReplyExpected(isSepaInstant)
                .reference(reference)
                .build();

        final int[] expectedFinalAmount = {ManagedAccountsHelper.getManagedAccountAvailableBalance(managedAccountId, secretKey, token)};
        final int[] fee = new int[1];

        IntStream.range(0, depositCount).forEach(i -> {
            SimulatorService.simulateManagedAccountPendingDepositByIban(simulatePendingDepositModel, innovatorId, iban);

            fee[0] = TestHelper.getFees(currency).get(FeeType.DEPOSIT_FEE).getAmount().intValue();

            expectedFinalAmount[0] += amount - fee[0];

            TestHelper. ensureAsExpected(120,
                () -> ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, token),
                x -> x.statusCode() == SC_OK && x.jsonPath().get("balances.availableBalance").equals(expectToApprove? expectedFinalAmount[0] : 0),
                Optional.of(String.format("Expecting 200 with a balance value of %s for managed account %s, check logged payload",
                    expectToApprove? expectedFinalAmount[0] : 0, managedAccountId)));
        });

        return Pair.of(ManagedAccountsHelper.getManagedAccountAvailableBalance(managedAccountId, secretKey, token), fee[0]);
    }

    public static void simulateDepositWithSpecificBalanceCheck(final String managedAccountId,
                                                               final CurrencyAmount depositAmount,
                                                               final String secretKey,
                                                               final String token,
                                                               final int expectedBalance) {

        checkAndUpgradeIbanIfUnassigned(managedAccountId, secretKey, token);

        final SimulateDepositModel simulateDepositModel
                = SimulateDepositModel.defaultSimulateModel(depositAmount);

        SimulatorService.simulateManagedAccountDeposit(simulateDepositModel, secretKey, managedAccountId);

        final int[] actualBalance = new int[1];

        TestHelper.ensureAsExpected(120,
                () -> ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, token),
                x -> {
                        actualBalance[0] = x.jsonPath().get("balances.availableBalance");
                        return x.statusCode() == SC_OK && actualBalance[0] == expectedBalance;
                },
                Optional.of(String.format("Expecting balance to be %s but was %s",
                        expectedBalance, actualBalance[0])));
    }

    public static void simulateDepositInStatePending(final String managedAccountId,
                                                     final CurrencyAmount depositAmount,
                                                     final String secretKey,
                                                     final String token,
                                                     final int expectedActualBalance,
                                                     final int expectedAvailableBalance) {

        checkAndUpgradeIbanIfUnassigned(managedAccountId, secretKey, token);

        final SimulateDepositModel simulateDepositModel
            = SimulateDepositModel.defaultSimulateModel(depositAmount);

        SimulatorService.simulateManagedAccountDeposit(simulateDepositModel, secretKey, managedAccountId);

        final int[] actualBalance = new int[1];
        final int[] availableBalance = new int[1];

        TestHelper.ensureAsExpected(120,
            () -> ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, token),
            x -> {
                actualBalance[0] = x.jsonPath().get("balances.actualBalance");
                availableBalance[0] = x.jsonPath().get("balances.availableBalance");
                return x.statusCode() == SC_OK && actualBalance[0] == expectedActualBalance && availableBalance[0] == expectedAvailableBalance;
            },
            Optional.of(String.format("Expecting actual balance to be %s but was %s, expecting available balance to be %s but was %s",
                expectedActualBalance, actualBalance[0], expectedAvailableBalance, availableBalance[0])));
    }

    public static void simulateSuccessfulDeposit(final String managedAccountId,
                                                 final CurrencyAmount depositAmount,
                                                 final String secretKey,
                                                 final String token) {
        checkAndUpgradeIbanIfUnassigned(managedAccountId, secretKey, token);

        final SimulateDepositModel simulateDepositModel
                = SimulateDepositModel.dataSimulateModel(depositAmount);

        SimulatorService.simulateManagedAccountDeposit(simulateDepositModel, secretKey, managedAccountId);

        TestHelper.ensureAsExpected(120,
                () -> ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, token),
                x -> {
                    final int balance = x.jsonPath().get("balances.availableBalance");
                    return x.statusCode() == SC_OK && balance > 0;
                },
                Optional.of("Deposit unsuccessful"));
    }

    public static void simulateSuccessfulPendingDeposit(final String managedAccountId,
                                                        final String currency,
                                                        final long amount,
                                                        final boolean webhook,
                                                        final boolean isSepaInstant,
                                                        final String innovatorId,
                                                        final String secretKey,
                                                        final String token) {

        checkAndUpgradeIbanIfUnassigned(managedAccountId, secretKey, token);

        final SimulatePendingDepositModel simulatePendingDepositModel =
                SimulatePendingDepositModel.builder()
                        .senderName(String.format("SenderName%s", RandomStringUtils.randomNumeric(5)))
                        .senderIban(ModelHelper.generateRandomValidIban())
                        .depositAmount(new CurrencyAmount(currency, amount))
                        .webhook(webhook)
                        .immediateMonitorReplyExpected(isSepaInstant)
                        .reference(String.format("Reference%s", RandomStringUtils.randomNumeric(5)))
                        .build();

        SimulatorService.simulateManagedAccountPendingDeposit(simulatePendingDepositModel, innovatorId, managedAccountId);

        TestHelper.ensureAsExpected(120,
                () -> ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, token),
                x -> {
                    final int balance = x.jsonPath().get("balances.availableBalance");
                    return x.statusCode() == SC_OK && balance > 0;
                },
                Optional.of("Deposit unsuccessful"));
    }

    public static void simulateDepositWithIban(final String managedAccountId,
                                               final String iban,
                                               final CurrencyAmount depositAmount,
                                               final String secretKey,
                                               final String token,
                                               final int expectedBalance) {

        checkAndUpgradeIbanIfUnassigned(managedAccountId, secretKey, token);


        final SimulateDepositByIbanModel simulateDepositModel
            = new SimulateDepositByIbanModel(depositAmount, iban);

        SimulatorService.simulateDepositByIban(simulateDepositModel, secretKey);

        final int[] actualBalance = new int[1];

        TestHelper.ensureAsExpected(120,
            () -> ManagedAccountsService.getManagedAccount(secretKey, managedAccountId, token),
            x -> {
                actualBalance[0] = x.jsonPath().get("balances.availableBalance");
                return x.statusCode() == SC_OK && actualBalance[0] == expectedBalance;
            },
            Optional.of(String.format("Expecting balance to be %s but was %s",
                expectedBalance, actualBalance[0])));
    }

    public static Pair<String, CreateManagedAccountModel> simulateManagedAccountDepositAndTransferToCard(final String managedAccountProfileId,
                                                                                                         final String transfersProfileId,
                                                                                                         final String managedCardId,
                                                                                                         final String currency,
                                                                                                         final Long depositAmount,
                                                                                                         final String secretKey,
                                                                                                         final String token) {
        return simulateManagedAccountDepositAndTransferToCard(managedAccountProfileId, transfersProfileId, managedCardId,
                currency, depositAmount, secretKey, token, 1);
    }

    public static Pair<String, CreateManagedAccountModel> simulateManagedAccountDepositAndTransferToCard(final String managedAccountProfileId,
                                                                                                         final String transfersProfileId,
                                                                                                         final String managedCardId,
                                                                                                         final String currency,
                                                                                                         final Long depositAmount,
                                                                                                         final String secretKey,
                                                                                                         final String token,
                                                                                                         final int transferCount) {

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(managedAccountProfileId, currency).build();

        final String managedAccountForCardFunding =
                ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, token);

        checkAndUpgradeIbanIfUnassigned(managedAccountForCardFunding, secretKey, token);

        simulateManagedAccountDeposit(managedAccountForCardFunding, currency, 10000L, secretKey, token, 1);

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(currency, depositAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountForCardFunding, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedCardId, MANAGED_CARDS))
                        .build();

        IntStream.range(0, transferCount).forEach(i ->
                TransfersService.transferFunds(transferFundsModel, secretKey, token, Optional.empty())
                        .then()
                        .statusCode(SC_OK));

        return Pair.of(managedAccountForCardFunding, createManagedAccountModel);
    }

    public static void transferFundsToCard(final String managedAccountId,
                                           final String transfersProfileId,
                                           final String managedCardId,
                                           final String currency,
                                           final Long depositAmount,
                                           final String secretKey,
                                           final String token) {

        final TransferFundsModel transferFundsModel =
                TransferFundsModel.newBuilder()
                        .setProfileId(transfersProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(currency, depositAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedCardId, MANAGED_CARDS))
                        .build();

        TransfersService.transferFunds(transferFundsModel, secretKey, token, Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    public static String convertToDecimal(final Long amount) {
        return new DecimalFormat("0.00").format(amount / 100);
    }

    public static Pair<Integer, Integer> transferFundsToCards(final String managedAccountId,
                                                              final String transfersProfileId,
                                                              final List<String> managedCardIds,
                                                              final String currency,
                                                              final String secretKey,
                                                              final String token) {

        final Long depositAmount = 500L;
        final int fee = TestHelper.getFees(currency).get(FeeType.MA_TO_MC_TRANSFER_FEE).getAmount().intValue();

        managedCardIds.forEach(managedCardId -> {
            final TransferFundsModel transferFundsModel =
                    TransferFundsModel.newBuilder()
                            .setProfileId(transfersProfileId)
                            .setTag(RandomStringUtils.randomAlphabetic(5))
                            .setDestinationAmount(new CurrencyAmount(currency, depositAmount))
                            .setSource(new ManagedInstrumentTypeId(managedAccountId, MANAGED_ACCOUNTS))
                            .setDestination(new ManagedInstrumentTypeId(managedCardId, MANAGED_CARDS))
                            .build();

            TransfersService.transferFunds(transferFundsModel, secretKey, token, Optional.empty())
                    .then()
                    .statusCode(SC_OK);
        });

        return Pair.of(depositAmount.intValue(), fee);
    }

    public static String generateTimestampAfter(final long minutes) {
        return String.valueOf(Instant.now().toEpochMilli() + TimeUnit.MINUTES.toMillis(minutes));
    }

    public static String generateTimestampAfter(final TimeUnit timeUnit,
                                                final long time) {
        return String.valueOf(Instant.now().toEpochMilli() + timeUnit.toMillis(time));
    }

    public static String generateTimestampBefore(final long minutes) {
        return String.valueOf(Instant.now().toEpochMilli() - TimeUnit.MINUTES.toMillis(minutes));
    }

    public static String generateTimestampNow() {
        return String.valueOf(Instant.now().toEpochMilli());
    }

    public static String generateRandomNumericStringWithNoLeadingZero(int length) {
        String randomNumericString;
        do {
            randomNumericString = RandomStringUtils.randomNumeric(length);

        } while (randomNumericString.startsWith("0"));
        return randomNumericString;
    }

    public static String generateRandomValidMobileNumber(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("Length must be greater than or equal to 1");
        }

        // Ensure the first digit is not zero to make it a valid mobile number
        Random random = new Random();
        StringBuilder mobileNumber = new StringBuilder("+");
        mobileNumber.append(random.nextInt(9) + 1); // First digit is 1-9

        // Generate the remaining digits
        for (int i = 1; i < length; i++) {
            mobileNumber.append(random.nextInt(10)); // Subsequent digits are 0-9
        }

        return mobileNumber.toString();
    }

    private static void checkAndUpgradeIbanIfUnassigned(final String managedAccountId,
                                                 final String secretKey,
                                                 final String token) {
        if ( ManagedAccountsService.getManagedAccountIban(secretKey,managedAccountId, token).then().extract().jsonPath().get("state").equals("UNALLOCATED")) {
            ensureAsExpected(5,
                    () -> ManagedAccountsService.assignManagedAccountIban(secretKey, managedAccountId, token),
                    SC_OK);
        }
    }
}
