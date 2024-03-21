package opc.junit.helpers.admin;

import commons.enums.Currency;
import io.restassured.response.Response;
import opc.enums.opc.BlockType;
import opc.enums.opc.CardBrand;
import opc.enums.opc.ConfigurationName;
import opc.enums.opc.ConfigurationType;
import opc.enums.opc.KycLevel;
import opc.enums.opc.LimitInterval;
import opc.enums.opc.ManagedInstrumentType;
import opc.enums.opc.RetryType;
import opc.junit.database.AdminGatewayDatabaseHelper;
import opc.junit.database.ManagedCardsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.models.admin.AdminSemiToggleModel;
import opc.models.admin.ConsumeAdminUserInviteModel;
import opc.models.admin.ConsumersLimitModel;
import opc.models.admin.ConsumersWithKycResponseModel;
import opc.models.admin.CorporatesLimitModel;
import opc.models.admin.CorporatesWithKybResponseModel;
import opc.models.admin.CreateManualTransactionModel;
import opc.models.admin.CreatePluginModel;
import opc.models.admin.CreatePluginResponseModel;
import opc.models.admin.CreateThirdPartyProviderCertificateModel;
import opc.models.admin.CreateThirdPartyProviderModel;
import opc.models.admin.CurrencyMinMaxModel;
import opc.models.admin.FinancialLimitValueModel;
import opc.models.admin.GetAuthorisationsModel;
import opc.models.admin.GetDepositsModel;
import opc.models.admin.GetSettlementsRequestModel;
import opc.models.admin.GetUserChallengesModel;
import opc.models.admin.LimitContextModel;
import opc.models.admin.LimitValueModel;
import opc.models.admin.LimitsApiContextModel;
import opc.models.admin.LimitsApiContextWithCurrencyModel;
import opc.models.admin.LimitsApiIntervalModel;
import opc.models.admin.LimitsApiLowValueExemptionModel;
import opc.models.admin.LimitsApiLowValueModel;
import opc.models.admin.LimitsApiModel;
import opc.models.admin.LimitsApiValueModel;
import opc.models.admin.LimitsIdentityModel;
import opc.models.admin.LimitsRemainingDeltasModel;
import opc.models.admin.MaxAmountModel;
import opc.models.admin.RemainingDeltaModel;
import opc.models.admin.RemainingDeltasResponseModel;
import opc.models.admin.ResumeDepositsModel;
import opc.models.admin.ResumeSendsModel;
import opc.models.admin.ResumeSettlementsModel;
import opc.models.admin.ResumeTransactionModel;
import opc.models.admin.RetryAuthorisationModel;
import opc.models.admin.RetryAuthorisationsModel;
import opc.models.admin.RetryTransfersModel;
import opc.models.admin.SendAdminUserInviteModel;
import opc.models.admin.SetGlobalLimitModel;
import opc.models.admin.SetLimitModel;
import opc.models.admin.SetScaModel;
import opc.models.admin.SettlementRetryModel;
import opc.models.admin.SpendRulesModel;
import opc.models.admin.UpdateKybModel;
import opc.models.admin.UpdateKycModel;
import opc.models.admin.WindowWidthModel;
import opc.models.innovator.ActivateIdentityModel;
import opc.models.innovator.ContextDimensionKeyValueModel;
import opc.models.innovator.ContextDimensionPartModel;
import opc.models.innovator.ContextDimensionValueModel;
import opc.models.innovator.ContextDimensionsModel;
import opc.models.innovator.ContextModel;
import opc.models.innovator.ContextSetModel;
import opc.models.innovator.ContextValueModel;
import opc.models.innovator.DeactivateIdentityModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.GetInnovatorUserModel;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.models.shared.PasswordModel;
import opc.models.sumsub.questionnaire.SumSubQuestionnaireContext;
import opc.models.sumsub.questionnaire.SumSubQuestionnaireDimensionKeyValueModel;
import opc.models.sumsub.questionnaire.SumSubQuestionnaireModel;
import opc.services.admin.AdminService;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

public class AdminHelper {

    public static String getSettlementId(final String cardId,
                                         final String state,
                                         final String merchantTransactionType,
                                         final String token) {

        final GetSettlementsRequestModel getSettlementsRequestModel =
                GetSettlementsRequestModel.builder()
                        .setCardId(cardId)
                        .setState(Collections.singletonList(state))
                        .setMerchantTransactionType(Collections.singletonList(merchantTransactionType))
                        .build();


        return TestHelper.ensureAsExpected(30,
                        () -> AdminService.getSettlements(getSettlementsRequestModel, token),
                        x -> x.statusCode() == SC_OK &&
                                x.jsonPath().get("entry[0].details.id") != null,
                        Optional.of("Issue with retrieving settlement details"))
                .jsonPath().get("entry[0].details.id");
    }

    public static void retrySettlementAfterSweep(final String innovatorToken, final String settlementId) {

        // TODO Remove after simulator improvements
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        TestHelper.ensureAsExpected(30,
                () -> AdminService.settlementRetry(SettlementRetryModel.DefaultSettlementRetryModel(RetryType.RETRY, "Settle after sweep."), innovatorToken, settlementId),
                SC_NO_CONTENT);
    }

    public static void setProfileSpendRules(final SpendRulesModel spendRulesModel,
                                            final String token,
                                            final String programmeId,
                                            final String profileId) {
        TestHelper.ensureAsExpected(15,
                () -> AdminService.setProfileSpendRules(spendRulesModel, token, programmeId, profileId),
                SC_OK);
    }

    public static void addCardSpendRules(final SpendRulesModel spendRulesModel,
                                         final String token,
                                         final String managedCardId) {
        TestHelper.ensureAsExpected(15,
                () -> AdminService.addCardSpendRules(spendRulesModel, managedCardId, token),
                SC_NO_CONTENT);

        TestHelper.ensureAsExpected(5,
                () -> AdminService.getCardSpendRules(token, managedCardId),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("cardLevelSpendRules.allowCreditAuthorisations").equals(spendRulesModel.getAllowCreditAuthorisations()),
                Optional.of(String.format("Spend rules not added for card with id %s", managedCardId)));
    }

    public static void addCardSpendRules(final SpendRulesModel spendRulesModel,
                                         final String token,
                                         final String managedCardId,
                                         final Function<Response, Boolean> testCondition) {
        TestHelper.ensureAsExpected(15,
                () -> AdminService.addCardSpendRules(spendRulesModel, managedCardId, token),
                SC_NO_CONTENT);

        TestHelper.ensureAsExpected(5,
                () -> AdminService.getCardSpendRules(token, managedCardId),
                testCondition,
                Optional.of(String.format("Spend rules not added for card with id %s", managedCardId)));
    }

    public static void setCorporateVelocityLimit(final CurrencyAmount baseLimit,
                                                 final List<CurrencyAmount> currencyLimit,
                                                 final String authenticationToken,
                                                 final String corporateId) {

        final SetLimitModel setLimitModel = SetLimitModel.builder()
                .setLimitType("CORPORATE_VELOCITY_AGGREGATE")
                .setBaseLimit(baseLimit)
                .setCurrencyLimit(currencyLimit)
                .build();

        TestHelper.ensureAsExpected(15,
                () -> AdminService.setCorporateLimit(setLimitModel, authenticationToken, corporateId),
                SC_NO_CONTENT);
    }

    public static void setConsumerVelocityLimit(final CurrencyAmount baseLimit,
                                                final List<CurrencyAmount> currencyLimit,
                                                final String authenticationToken,
                                                final String consumerId) {

        final SetLimitModel setLimitModel = SetLimitModel.builder()
                .setLimitType("CONSUMER_VELOCITY_AGGREGATE")
                .setBaseLimit(baseLimit)
                .setCurrencyLimit(currencyLimit)
                .build();

        TestHelper.ensureAsExpected(15,
                () -> AdminService.setConsumerLimit(setLimitModel, authenticationToken, consumerId),
                SC_NO_CONTENT);
    }

    public static void setGlobalCorporateVelocityLimit(final SetGlobalLimitModel setGlobalLimitModel,
                                                       final String authenticationToken,
                                                       final String limitType) {

        TestHelper.ensureAsExpected(15,
                () -> AdminService.setGlobalCorporateLimit(setGlobalLimitModel, authenticationToken, limitType),
                SC_NO_CONTENT);
    }

    public static void setGlobalConsumerVelocityLimit(final SetGlobalLimitModel setGlobalLimitModel,
                                                      final String authenticationToken,
                                                      final String limitType) {

        TestHelper.ensureAsExpected(15,
                () -> AdminService.setGlobalConsumerLimit(setGlobalLimitModel, authenticationToken, limitType),
                SC_NO_CONTENT);
    }

    public static void resetCorporateLimit(final String authenticationToken, final String corporateId) {

        TestHelper.ensureAsExpected(15,
                () -> AdminService.resetCorporateLimit(authenticationToken, corporateId),
                SC_NO_CONTENT);
    }

    public static void resetConsumerLimit(final String authenticationToken, final String consumerId) {

        TestHelper.ensureAsExpected(15,
                () -> AdminService.resetConsumerLimit(authenticationToken, consumerId),
                SC_NO_CONTENT);
    }

    public static void resumeDeposit(final String authenticationToken, final String depositId) {

        TestHelper.ensureAsExpected(15,
                () -> AdminService.resumeDeposit(authenticationToken, new ResumeTransactionModel("Note."), depositId),
                SC_NO_CONTENT);

        TestHelper.ensureAsExpected(30,
                () -> AdminService.getDeposit(authenticationToken, depositId),
                x -> x.statusCode() == SC_OK && (x.jsonPath().getString("state").equals("SETTLED") ||
                        x.jsonPath().getString("state").equals("SETTLED_MANUAL")),
                Optional.of("Expecting 200 with deposit state to be SETTLED or SETTLED_MANUAL, check logged payloads"));
    }

    public static void resumeDeposits(final String authenticationToken, final List<String> depositIds) {

        TestHelper.ensureAsExpected(15,
                () -> AdminService.resumeDeposits(authenticationToken, new ResumeDepositsModel(depositIds, "Note.")),
                SC_NO_CONTENT);

        depositIds.forEach(depositId ->
                TestHelper.ensureAsExpected(30,
                        () -> AdminService.getDeposit(authenticationToken, depositId),
                        x -> x.statusCode() == SC_OK && (x.jsonPath().getString("state").equals("SETTLED") ||
                                x.jsonPath().getString("state").equals("SETTLED_MANUAL")),
                        Optional.of("Expecting 200 with deposit state to be SETTLED or SETTLED_MANUAL, check logged payloads")));
    }

    public static void retryDeposit(final String authenticationToken, final String depositId) {

        TestHelper.ensureAsExpected(15,
                () -> AdminService.retryDeposit(authenticationToken, new ResumeTransactionModel("Note."), depositId),
                SC_NO_CONTENT);
    }

    public static Response getDeposit(final String authenticationToken, final String depositId) {

        return TestHelper.ensureAsExpected(15,
                () -> AdminService.getDeposit(authenticationToken, depositId),
                SC_OK);
    }

    public static Response getDeposits(final String authenticationToken, final String managedAccountId) {

        return TestHelper.ensureAsExpected(15,
                () -> AdminService.getDeposits(authenticationToken, new GetDepositsModel(Long.parseLong(managedAccountId))),
                x -> Integer.parseInt(x.jsonPath().getString("count")) > 0,
                Optional.of(String.format("No deposits found for managed account with id %s", managedAccountId)));
    }

    public static void resumeSend(final String authenticationToken, final String sendId) {

        TestHelper.ensureAsExpected(15,
                () -> AdminService.resumeSend(authenticationToken, new ResumeTransactionModel("Note."), sendId),
                SC_NO_CONTENT);

        TestHelper.ensureAsExpected(30,
                () -> AdminService.getSend(authenticationToken, sendId),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("state").equals("COMPLETED"),
                Optional.of("Expecting 200 with send state to be COMPLETED, check logged payloads"));
    }

    public static void resumeSends(final String authenticationToken, final List<String> sendIds) {

        TestHelper.ensureAsExpected(15,
                () -> AdminService.resumeSends(authenticationToken, new ResumeSendsModel(sendIds, "Note.")),
                SC_NO_CONTENT);

        sendIds.forEach(sendId ->
                TestHelper.ensureAsExpected(30,
                        () -> AdminService.getSend(authenticationToken, sendId),
                        x -> x.statusCode() == SC_OK && x.jsonPath().getString("state").equals("COMPLETED"),
                        Optional.of("Expecting 200 with send state to be COMPLETED, check logged payloads")));
    }

    public static Response getSend(final String authenticationToken, final String sendId) {

        return TestHelper.ensureAsExpected(15,
                () -> AdminService.getSend(authenticationToken, sendId),
                SC_OK);
    }

    public static void resumeOct(final String authenticationToken, final String octId) {

        TestHelper.ensureAsExpected(15,
                () -> AdminService.resumeOct(authenticationToken, new ResumeTransactionModel("Note."), octId),
                SC_NO_CONTENT);

        TestHelper.ensureDatabaseResultAsExpected(30,
                () -> ManagedCardsDatabaseHelper.getSettlement(octId),
                x -> x.size() > 0 && (x.get(0).get("settlement_state").equals("SETTLED")
                        || x.get(0).get("settlement_state").equals("SETTLED_MANUAL")),
                Optional.of(String.format("OCT with id %s no settled", octId)));
    }

    public static void resumeOcts(final String authenticationToken, final List<String> octIds) {

        TestHelper.ensureAsExpected(15,
                () -> AdminService.resumeOcts(authenticationToken, new ResumeSettlementsModel(octIds, "Note.")),
                SC_NO_CONTENT);

        octIds.forEach(octId ->
                TestHelper.ensureDatabaseResultAsExpected(30,
                        () -> ManagedCardsDatabaseHelper.getSettlement(octId),
                        x -> x.size() > 0 && (x.get(0).get("settlement_state").equals("SETTLED")
                                || x.get(0).get("settlement_state").equals("SETTLED_MANUAL")),
                        Optional.of(String.format("OCT with id %s no settled", octId))));
    }

    public static void setCorporateFundsSourceLimit(final CurrencyAmount baseLimit,
                                                    final List<CurrencyAmount> currencyLimit,
                                                    final String authenticationToken,
                                                    final String corporateId) {

        final SetLimitModel setLimitModel = SetLimitModel.builder()
                .setLimitType("CORPORATE_FUNDS_SOURCE_AGGREGATE")
                .setBaseLimit(baseLimit)
                .setCurrencyLimit(currencyLimit)
                .build();

        TestHelper.ensureAsExpected(15,
                () -> AdminService.setCorporateLimit(setLimitModel, authenticationToken, corporateId),
                SC_NO_CONTENT);
    }

    public static void setConsumerFundsSourceLimit(final CurrencyAmount baseLimit,
                                                   final List<CurrencyAmount> currencyLimit,
                                                   final String authenticationToken,
                                                   final String consumerId) {

        final SetLimitModel setLimitModel = SetLimitModel.builder()
                .setLimitType("CONSUMER_FUNDS_SOURCE_AGGREGATE")
                .setBaseLimit(baseLimit)
                .setCurrencyLimit(currencyLimit)
                .build();

        TestHelper.ensureAsExpected(15,
                () -> AdminService.setConsumerLimit(setLimitModel, authenticationToken, consumerId),
                SC_NO_CONTENT);
    }

    public static void setProgrammeAuthyChallengeLimit(final String programmeId,
                                                       final LimitInterval limitInterval,
                                                       final int limitCount,
                                                       final String token) {

        final LimitsApiModel limitsApiModel =
                LimitsApiModel.builder()
                        .setContext(LimitsApiContextModel.builder().setProgrammeIdContext(programmeId).build())
                        .setValue(Collections.singletonList(LimitsApiValueModel.builder()
                                .setInterval(LimitsApiIntervalModel.builder().setTumbling(limitInterval.name()).build())
                                .setMaxCount(limitCount)
                                .setMaxSum(1)
                                .build()))
                        .build();

        TestHelper.ensureAsExpected(15,
                () -> AdminService.setLimits(token, limitsApiModel, "AUTHY_CHALLENGE_LIMIT"),
                SC_NO_CONTENT);
    }

    public static void setProgrammeAuthyChallengeLimit(final String programmeId,
                                                       final Map<LimitInterval, Integer> intervalCountMap,
                                                       final String token) {

        final List<LimitsApiValueModel> limitsApiValueModels = new ArrayList<>();
        intervalCountMap.forEach((key, value) -> limitsApiValueModels.add(LimitsApiValueModel.builder()
                .setInterval(LimitsApiIntervalModel.builder().setTumbling(key.name()).build())
                .setMaxCount(value)
                .setMaxSum(1)
                .build()));

        final LimitsApiModel limitsApiModel =
                LimitsApiModel.builder()
                        .setContext(LimitsApiContextModel.builder().setProgrammeIdContext(programmeId).build())
                        .setValue(limitsApiValueModels)
                        .build();

        TestHelper.ensureAsExpected(15,
                () -> AdminService.setLimits(token, limitsApiModel, "AUTHY_CHALLENGE_LIMIT"),
                SC_NO_CONTENT);
    }

    public static void setConsumerLowValueLimit(final String token) {

        final LimitsApiModel limitsApiModel =
                LimitsApiModel.builder()
                        .setValue(Collections.singletonList(LimitsApiValueModel.builder()
                                .setInterval(LimitsApiIntervalModel.builder().setTumbling("ALWAYS").build())
                                .setMaxCount(5)
                                .setMaxSum(100)
                                .build()))
                        .build();

        TestHelper.ensureAsExpected(15,
                () -> AdminService.setLimits(token, limitsApiModel, "CONSUMER_LOW_VALUE_EXEMPTION"),
                SC_NO_CONTENT);
    }

    public static void setCorporateLowValueLimit(final String token) {

        final LimitsApiModel limitsApiModel =
                LimitsApiModel.builder()
                        .setValue(Collections.singletonList(LimitsApiValueModel.builder()
                                .setInterval(LimitsApiIntervalModel.builder().setTumbling("ALWAYS").build())
                                .setMaxCount(5)
                                .setMaxSum(100)
                                .build()))
                        .build();

        TestHelper.ensureAsExpected(15,
                () -> AdminService.setLimits(token, limitsApiModel, "CORPORATE_LOW_VALUE_EXEMPTION"),
                SC_NO_CONTENT);
    }

    public static void setCorporateLowValueLimitWithCurrency(final String programmeId,
                                                             final Currency currency,
                                                             final String token) {

        final LimitsApiLowValueExemptionModel limitsApiLowValueExemptionModel =
                LimitsApiLowValueExemptionModel.builder()
                        .setContext(LimitsApiContextWithCurrencyModel.builder().setProgrammeIdContext(programmeId).setCurrencyContext(String.valueOf(currency)).build())
                        .setValue(LimitsApiLowValueModel.builder()
                                .setMaxCount(5)
                                .setMaxSum(100)
                                .setMaxAmount(25)
                                .build())
                        .build();

        TestHelper.ensureAsExpected(15,
                () -> AdminService.setCorporateLimits(token, limitsApiLowValueExemptionModel),
                SC_OK);
    }

    public static void setConsumerLowValueLimitWithCurrency(final String programmeId,
                                                            final Currency currency,
                                                            final String token) {

        final LimitsApiLowValueExemptionModel limitsApiLowValueExemptionModel =
                LimitsApiLowValueExemptionModel.builder()
                        .setContext(LimitsApiContextWithCurrencyModel.builder().setProgrammeIdContext(programmeId).setCurrencyContext(String.valueOf(currency)).build())
                        .setValue(LimitsApiLowValueModel.builder()
                                .setMaxCount(5)
                                .setMaxSum(100)
                                .setMaxAmount(32)
                                .build())
                        .build();

        TestHelper.ensureAsExpected(15,
                () -> AdminService.setConsumerLimits(token, limitsApiLowValueExemptionModel),
                SC_OK);
    }

    public static RemainingDeltasResponseModel getProgrammeAuthyChallengeRemainingDeltas(final String programmeId,
                                                                                         final String token) {

        final LimitsRemainingDeltasModel limitsRemainingDeltasModel =
                LimitsRemainingDeltasModel.builder()
                        .setContext(LimitsApiContextModel.builder().setProgrammeIdContext(programmeId).build())
                        .setIdentity(LimitsIdentityModel.builder()
                                .setType("programmes")
                                .setId(programmeId)
                                .build())
                        .build();

        return TestHelper.ensureAsExpected(15,
                () -> AdminService.getLimitRemainingDeltas(token, limitsRemainingDeltasModel, "AUTHY_CHALLENGE_LIMIT"),
                SC_OK).as(RemainingDeltasResponseModel.class);
    }

    public static void resetProgrammeAuthyLimitsCounter(final String programmeId,
                                                        final String token) {

        final LimitsIdentityModel limitsIdentityModel =
                LimitsIdentityModel.builder()
                        .setType("programmes")
                        .setId(programmeId)
                        .build();

        TestHelper.ensureAsExpected(15,
                () -> AdminService.resetLimitsCounter(token, limitsIdentityModel, "AUTHY_CHALLENGE_LIMIT"),
                SC_NO_CONTENT);
    }

    public static void setAuthyChallengeLimit(final String programmeId,
                                              final Map<LimitInterval, Integer> intervalCountMap) {

        final Map<LimitInterval, Integer> defaultLimits = new HashMap<>();
        intervalCountMap.forEach((key1, value1) -> defaultLimits.put(key1, 0));
        defaultLimits.entrySet().forEach(limit -> limit.setValue(1000));

        final String token = AdminService.loginAdmin();

        setProgrammeAuthyChallengeLimit(programmeId, defaultLimits, token);

        final Map<LimitInterval, Integer> requiredCountMap = new HashMap<>();

        intervalCountMap.forEach((key, value) -> {

            final List<RemainingDeltaModel> remainingDeltas =
                    getProgrammeAuthyChallengeRemainingDeltas(programmeId, token)
                            .getRemainingDelta();

            final RemainingDeltaModel remainingDelta =
                    remainingDeltas.stream().filter(x -> key.name()
                                    .equals(x.getReason().getInterval().getTumbling()))
                            .collect(Collectors.toList()).get(0);

            final int currentRemainingCount = Integer.parseInt(remainingDelta.getRemainingCount());

            if (currentRemainingCount != value) {

                final int currentMaximumCount = Integer.parseInt(remainingDelta.getReason().getMaxCount());
                final int limitCount = currentMaximumCount - currentRemainingCount + value;

                requiredCountMap.put(key, limitCount);
            }
        });

        setProgrammeAuthyChallengeLimit(programmeId, requiredCountMap, token);
    }

    public static void setManagedCardTokenisationEnabledProperty(final String tenantId,
                                                                 final CardBrand cardBrand,
                                                                 final boolean manualProvisioningEnabled,
                                                                 final boolean pushProvisioningEnabled,
                                                                 final String token) {
        final ContextValueModel contextModel =
                ContextValueModel.builder()
                        .setContext(new ContextDimensionsModel(Arrays.asList(new ContextDimensionKeyValueModel("TenantIdDimension", tenantId, false),
                                new ContextDimensionKeyValueModel("ManagedCardBrand", cardBrand.name(), false))))
                        .setValue(new ContextDimensionPartModel(Arrays.asList(String.valueOf(manualProvisioningEnabled), String.valueOf(pushProvisioningEnabled))))
                        .build();

        TestHelper.ensureAsExpected(15,
                () -> AdminService.setContextPropertyValue(contextModel,
                        ConfigurationName.MANAGED_CARDS,
                        ConfigurationType.MANAGED_CARD_TOKENISATION_ENABLED,
                        token),
                SC_NO_CONTENT);
    }

    public static SumSubQuestionnaireModel setSumSubQuestionnaireContextProperties(final String dimensionKey, final String dimensionValue, final String partValue) {
        return SumSubQuestionnaireModel.builder()
                .context(new SumSubQuestionnaireContext(Arrays.asList(new SumSubQuestionnaireDimensionKeyValueModel(dimensionKey, dimensionValue))))
                .value(new ContextDimensionPartModel(Arrays.asList(partValue)))
                .build();
    }

    public static void setManagedCardDigitalWalletArtwork(final String tenantId,
                                                          final CardBrand cardBrand,
                                                          final List<String> values,
                                                          final String token) {

        final List<ContextDimensionKeyValueModel> dimensionKeyValues = new ArrayList<>();

        if (tenantId != null) {
            dimensionKeyValues.add(new ContextDimensionKeyValueModel("TenantIdDimension", tenantId, false));
        }

        if (cardBrand != null) {
            dimensionKeyValues.add(new ContextDimensionKeyValueModel("ManagedCardBrand", cardBrand.name(), false));
        }

        if (values == null) {

            final ContextDimensionsModel contextModel =
                    new ContextDimensionsModel(dimensionKeyValues);

            TestHelper.ensureAsExpected(15,
                    () -> AdminService.removeContextPropertyValue(contextModel,
                            ConfigurationName.MANAGED_CARDS,
                            ConfigurationType.MANAGED_CARD_DIGITAL_WALLET_ARTWORK,
                            token),
                    SC_NO_CONTENT);
        } else {
            final List<ContextDimensionPartModel> parts = new ArrayList<>();
            values.forEach(value -> parts.add(new ContextDimensionPartModel(Collections.singletonList(value))));

            final ContextSetModel contextModel =
                    ContextSetModel.builder()
                            .setContext(new ContextDimensionsModel(dimensionKeyValues))
                            .setSet(new ContextDimensionValueModel(parts))
                            .build();

            TestHelper.ensureAsExpected(15,
                    () -> AdminService.setContextPropertySet(contextModel,
                            ConfigurationName.MANAGED_CARDS,
                            ConfigurationType.MANAGED_CARD_DIGITAL_WALLET_ARTWORK,
                            token),
                    SC_NO_CONTENT);
        }
    }

    public static void blockManagedAccount(final String managedAccountId,
                                           final BlockType blockType,
                                           final String token) {
        TestHelper.ensureAsExpected(15,
                () -> AdminService.blockManagedAccount(managedAccountId, blockType, token),
                SC_OK);
    }

    public static void setDefaultConsumerDepositLimit(final String token,
                                                      final String consumerId,
                                                      final KycLevel kycLevel) {

        final SetGlobalLimitModel setLimitModel = SetGlobalLimitModel.builder()
                .setContext(new LimitContextModel(consumerId, kycLevel))
                .setWindowWidth(new WindowWidthModel(1, "ALWAYS"))
                .setLimitValue(new LimitValueModel(new MaxAmountModel("15000")))
                .setWideWindowMultiple("1")
                .setFinancialLimitValue(FinancialLimitValueModel.builder()
                        .setRefCurrency("EUR")
                        .setCurrencyMinMax(CurrencyMinMaxModel.builder()
                                .setGbp(new LimitValueModel(new MaxAmountModel("12500")))
                                .setUsd(new LimitValueModel(new MaxAmountModel("16700")))
                                .build())
                        .build())
                .build();

        AdminHelper.setGlobalCorporateVelocityLimit(setLimitModel, token, "CONSUMER_DEPOSIT_AMOUNT_AGGREGATE");
    }

    public static void fundManagedAccount(final String tenantId,
                                          final String managedAccountId,
                                          final String currency,
                                          final Long amount) {

        final CreateManualTransactionModel.Builder createManualTransactionModel =
                CreateManualTransactionModel.builder()
                        .setTargetInstrument(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                        .setCurrency(currency)
                        .setAvailableAmount(amount)
                        .setDeltaAmount(0L)
                        .setPendingAmount(0L)
                        .setReservedAmount(0L)
                        .setSystemAccount("Test")
                        .setForceBalance(true)
                        .setNote("Test deposit");

        final String impersonatedToken = AdminService.impersonateTenant(tenantId, AdminService.loginAdmin());

        TestHelper.ensureAsExpected(60,
                () -> AdminService.createManualTransaction(impersonatedToken, createManualTransactionModel.setRemoteAvailableAdjustment(true).build()),
                SC_OK);
    }

    public static void fundManagedCard(final String tenantId,
                                       final String managedCardId,
                                       final String currency,
                                       final Long amount) {

        final CreateManualTransactionModel.Builder createManualTransactionModel =
                CreateManualTransactionModel.builder()
                        .setTargetInstrument(new ManagedInstrumentTypeId(managedCardId, ManagedInstrumentType.MANAGED_CARDS))
                        .setCurrency(currency)
                        .setAvailableAmount(amount)
                        .setDeltaAmount(0L)
                        .setPendingAmount(0L)
                        .setReservedAmount(0L)
                        .setSystemAccount("Test")
                        .setForceBalance(false)
                        .setNote("Test deposit");

        final String impersonatedToken = AdminService.impersonateTenant(tenantId, AdminService.loginAdmin());

        TestHelper.ensureAsExpected(15,
                () -> AdminService.createManualTransaction(impersonatedToken, createManualTransactionModel.setRemoteAvailableAdjustment(true).build()),
                SC_OK);
    }

    public static void setSca(final String token,
                              final String programmeId,
                              final boolean scaMaEnabled,
                              final boolean scaMcEnabled) {

        TestHelper.ensureAsExpected(15,
                () -> AdminService.setSca(token, programmeId, scaMaEnabled, scaMcEnabled),
                SC_NO_CONTENT);
    }

    public static void setScaConfig(final String token,
                                    final String programmeId,
                                    final SetScaModel setScaModel) {

        TestHelper.ensureAsExpected(15,
                () -> AdminService.setScaConfig(token, programmeId, setScaModel),
                SC_NO_CONTENT);
    }

    public static void setTransactionMonitoring(final String token,
                                                final String programmeId,
                                                final boolean txmEnabled) {

        TestHelper.ensureAsExpected(15,
                () -> AdminService.setTransactionMonitoring(token, programmeId, txmEnabled),
                SC_NO_CONTENT);
    }

    public static void setTrustBiometrics(final String token,
                                          final String programmeId,
                                          final boolean trustBiometricsEnabled) {

        TestHelper.ensureAsExpected(15,
                () -> AdminService.setTrustBiometrics(token, programmeId, trustBiometricsEnabled),
                SC_NO_CONTENT);
    }

    public static Response getConsumerKyc(final String consumerId, final String token) {

        return TestHelper.ensureAsExpected(10, () -> AdminService.getConsumerKyc(consumerId, token), SC_OK);
    }

    public static void updateConsumerKyc(final UpdateKycModel updateKycModel,
                                         final String consumerId,
                                         final String adminToken) {
        TestHelper.ensureAsExpected(15,
                () -> AdminService.updateConsumerKyc(updateKycModel, consumerId, adminToken),
                SC_OK);
    }

    public static void updateCorporateKyb(final UpdateKybModel updateKybModel,
                                          final String corporateId,
                                          final String adminToken) {
        TestHelper.ensureAsExpected(15,
                () -> AdminService.updateCorporateKyb(updateKybModel, corporateId, adminToken),
                SC_OK);
    }

    public static Response getAuthorisationById(final String authorisationId,
                                                final String token) {
        return TestHelper.ensureAsExpected(15, () -> AdminService.getAuthorisationById(authorisationId, token), SC_OK);
    }

    public static Response getAuthorisations(final GetAuthorisationsModel getAuthorisationsModel,
                                             final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.getAuthorisations(getAuthorisationsModel, token), SC_OK);
    }

    public static Response retryAuthorisation(final RetryAuthorisationModel retryAuthorisationModel, final String authorisationId, final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.retryAuthorisation(retryAuthorisationModel, authorisationId, token), SC_NO_CONTENT);
    }

    public static Response retryAuthorisations(final RetryAuthorisationsModel retryAuthorisationsModel, final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.retryAuthorisations(retryAuthorisationsModel, token), SC_NO_CONTENT);
    }

    public static Pair<String, String> getTppWithCertificate() {

        final String token = AdminService.loginAdmin();

        final String tppId =
                TestHelper.ensureAsExpected(15,
                        () -> AdminService.createThirdPartyProvider(token, CreateThirdPartyProviderModel.defaultCreateTtpProviderModel()),
                        SC_OK).jsonPath().getString("id");

        final String clientKeyId =
                TestHelper.ensureAsExpected(15,
                        () -> AdminService.createThirdPartyProviderCertificate(token, CreateThirdPartyProviderCertificateModel.defaultCreateThirdPartyProviderCertificateModel(), tppId),
                        SC_OK).jsonPath().getString("id");

        return Pair.of(tppId, clientKeyId);
    }

    public static void blockManagedCard(final String managedCardId,
                                        final BlockType blockType,
                                        final String token) {
        TestHelper.ensureAsExpected(15,
                () -> AdminService.blockManagedCard(managedCardId, blockType, token),
                SC_OK);
    }

    public static String startCorporateUserKyc(final String corporateId, final String corporateUserId, final String token) {
        return TestHelper.ensureAsExpected(15,
                        () -> AdminService.startCorporateUserKyc(corporateId, corporateUserId, token),
                        SC_OK)
                .jsonPath()
                .get("reference");
    }

    public static Response enableAuthForwarding(final boolean enable,
                                                final String tenantId,
                                                final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.enableAuthForwarding(enable, tenantId, token), SC_NO_CONTENT);
    }

    public static Response setSemiToggle(final AdminSemiToggleModel toggleBody) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.setSemi(AdminService.loginAdmin(), toggleBody), SC_OK);
    }

    public static Response activateCorporate(final ActivateIdentityModel activateIdentityModel,
                                             final String corporateId,
                                             final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.activateCorporate(activateIdentityModel, corporateId, token), SC_NO_CONTENT);
    }

    public static Response activateConsumer(final ActivateIdentityModel activateIdentityModel,
                                            final String corporateId,
                                            final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.activateConsumer(activateIdentityModel, corporateId, token), SC_NO_CONTENT);
    }

    public static Response deactivateCorporate(final DeactivateIdentityModel deactivateIdentityModel,
                                               final String corporateId,
                                               final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.deactivateCorporate(deactivateIdentityModel, corporateId, token), SC_NO_CONTENT);
    }

    public static Response deactivateConsumer(final DeactivateIdentityModel deactivateIdentityModel,
                                              final String consumerId,
                                              final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.deactivateConsumer(deactivateIdentityModel, consumerId, token), SC_NO_CONTENT);
    }

    public static Response deactivateConsumerUser(final String consumerId,
                                                  final String userId,
                                                  final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.deactivateConsumerUser(consumerId, userId, token), SC_NO_CONTENT);
    }

    public static Response activateConsumerUser(final String consumerId,
                                                final String userId,
                                                final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.activateConsumerUser(consumerId, userId, token), SC_NO_CONTENT);
    }

    public static Response deactivateCorporateUser(final String corporateId,
                                                   final String userId,
                                                   final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.deactivateCorporateUser(corporateId, userId, token), SC_NO_CONTENT);
    }

    public static Response activateCorporateUser(final String corporateId,
                                                 final String userId,
                                                 final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.activateCorporateUser(corporateId, userId, token), SC_NO_CONTENT);
    }

    public static Response setManagedCardsProductReference(final ContextModel contextModel,
                                                           final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.setManagedCardsProductReference(contextModel, token), SC_NO_CONTENT);
    }

    public static Response setManagedCardsProductReferenceNameOnCardMaxChars(final String tenantId,
                                                                             final String physicalProductReference,
                                                                             final Integer maxChars,
                                                                             final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.setManagedCardsProductReferenceNameOnCardMaxChars(tenantId, physicalProductReference, maxChars, token), SC_NO_CONTENT);
    }

    public static Response setManagedCardsProductReferenceNameOnCardMaxChars(final Integer maxChars,
                                                                             final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.setManagedCardsProductReferenceNameOnCardMaxChars(maxChars, token), SC_NO_CONTENT);
    }


    public static String createNonRootAdminUser() throws SQLException {
        final SendAdminUserInviteModel sendAdminUserInviteModel = SendAdminUserInviteModel.defaultUserInviteModel();

        TestHelper.ensureAsExpected(15,
                () -> AdminService.sendUserInvite(sendAdminUserInviteModel, AdminService.loginAdmin()), SC_NO_CONTENT);

        final Map<String, String> inviteInfo = AdminGatewayDatabaseHelper.getUserInvite(
                sendAdminUserInviteModel.getEmail()).get(0);

        final ConsumeAdminUserInviteModel consumeAdminUserInviteModel = ConsumeAdminUserInviteModel.builder()
                .nonce(inviteInfo.get("nonce"))
                .email(sendAdminUserInviteModel.getEmail())
                .password(new PasswordModel("Pass1234!"))
                .build();

        TestHelper.ensureAsExpected(15,
                () -> AdminService.consumerUserInvite(consumeAdminUserInviteModel, inviteInfo.get("invite_id")), SC_NO_CONTENT);

        return AdminService.loginNonRootAdmin(sendAdminUserInviteModel.getEmail(), "Pass1234!");
    }

    public static CorporatesWithKybResponseModel getCorporatesSemi(final String profileId,
                                                                   final String token,
                                                                   final int numberOfCorporates) {

        final CorporatesLimitModel corporatesLimitModel =
                CorporatesLimitModel.getSortedSemiCorporates(numberOfCorporates, Long.valueOf(profileId));

        return TestHelper.ensureAsExpected(15,
                        () -> AdminService.getCorporates(corporatesLimitModel, token), SC_OK)
                .then()
                .extract()
                .as(CorporatesWithKybResponseModel.class);
    }

    public static ConsumersWithKycResponseModel getConsumers(final String programmeId,
                                                             final String token) {

        final ConsumersLimitModel consumersLimitModel =
                ConsumersLimitModel.getSortedConsumers(3, Long.valueOf(programmeId));

        return TestHelper.ensureAsExpected(15,
                        () -> AdminService.getConsumers(consumersLimitModel, token), SC_OK)
                .then()
                .extract()
                .as(ConsumersWithKycResponseModel.class);
    }

    public static CorporatesWithKybResponseModel getCorporates(final String profileId,
                                                               final String token) {

        return getCorporates(profileId, token, 2);
    }

    public static CorporatesWithKybResponseModel getCorporates(final String profileId,
                                                               final String token,
                                                               final int numberOfCorporates) {

        final CorporatesLimitModel corporatesLimitModel =
                CorporatesLimitModel.getSortedCorporates(numberOfCorporates, Long.valueOf(profileId));

        return TestHelper.ensureAsExpected(15,
                        () -> AdminService.getCorporates(corporatesLimitModel, token), SC_OK)
                .then()
                .extract()
                .as(CorporatesWithKybResponseModel.class);
    }

    public static void retryTransfer(final String authenticationToken, final String transferId) {

        TestHelper.ensureAsExpected(15,
                () -> AdminService.retryTransfer(authenticationToken, new ResumeTransactionModel("Note."), transferId),
                SC_NO_CONTENT);
    }

    public static void retryTransfers(final String authenticationToken,
                                      RetryTransfersModel retryTransfersModel) {

        TestHelper.ensureAsExpected(15,
                () -> AdminService.retryTransfers(authenticationToken, retryTransfersModel),
                SC_NO_CONTENT);
    }

    public static CreatePluginResponseModel createPlugin(final CreatePluginModel createPluginModel,
                                                         final String adminToken) {

        return TestHelper.ensureAsExpected(15,
                        () -> AdminService.createPlugin(createPluginModel, adminToken),
                        SC_OK)
                .then()
                .extract()
                .as(CreatePluginResponseModel.class);
    }

    public static Response getInnovatorUser(final GetInnovatorUserModel getInnovatorUserModel,
                                            final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.getInnovatorUser(getInnovatorUserModel, token),
                SC_OK);
    }

    public static String getUserChallenges(final GetUserChallengesModel userChallengesModel,
                                             final String adminToken,
                                             final String identityId) {
        return TestHelper.ensureAsExpected(15,
                () -> AdminService.getUserChallenges(userChallengesModel, adminToken, identityId),
                SC_OK)
                .then()
                .extract()
                .jsonPath()
                .getString("challenges[0].challengeId");
    }
}
