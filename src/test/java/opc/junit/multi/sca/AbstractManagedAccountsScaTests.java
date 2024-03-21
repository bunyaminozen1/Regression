package opc.junit.multi.sca;

import commons.enums.State;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.AcceptedResponse;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedaccounts.PatchManagedAccountModel;
import opc.models.multi.passwords.LostPasswordResumeModel;
import opc.models.multi.passwords.LostPasswordStartModel;
import opc.models.shared.PasswordModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.multi.ManagedAccountsService;
import opc.services.multi.PasswordsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@Tag(MultiTags.SCA_MANAGED_ACCOUNTS)
public abstract class AbstractManagedAccountsScaTests extends BaseScaSetup {
    protected abstract String getIdentityTokenScaApp();
    protected abstract String getIdentityManagedAccountProfileScaApp();

    protected abstract String getIdentityTokenScaMaApp();
    protected abstract String getIdentityManagedAccountProfileScaMaApp();
    protected abstract String getIdentityEmailScaMaApp();

    protected abstract String getIdentityTokenScaMcApp();
    protected abstract String getIdentityManagedAccountProfileScaMcApp();

    protected abstract Pair<String, String> createSteppedUpIdentity(final ProgrammeDetailsModel programme);

    protected abstract Pair<String, String> createIdentity(final ProgrammeDetailsModel programme);

    private final static String OTP_VERIFICATION_CODE = TestHelper.OTP_VERIFICATION_CODE;
    private final static String CHANNEL = "SMS";
    private final static String CURRENCY = "EUR";
    private final static int SCA_EXPIRED_TIME = 61000;

    @Test
    public void CreateManagedAccount_ScaMaTrue_Success() {
// scaMA: true

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, getIdentityTokenScaMaApp(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMaApp()))
                .body("tag", equalTo(createManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(createManagedAccountModel.getCurrency()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0));
    }

    @Test
    public void CreateManagedAccount_ScaMaFalse_Success() {
// scaMC: true

        final Pair<String, String> identity = createSteppedUpIdentity(scaMcApp);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMcApp(),
                                CURRENCY)
                        .build();
        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKeyScaMcApp, identity.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMcApp()))
                .body("tag", equalTo(createManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(createManagedAccountModel.getCurrency()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0));
    }

    @Test
    public void CreateManagedAccount_TwoSessions_Success() {
// scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, identity.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMaApp()))
                .body("tag", equalTo(createManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(createManagedAccountModel.getCurrency()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0));

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, newToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMaApp()))
                .body("tag", equalTo(createManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(createManagedAccountModel.getCurrency()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0));
    }

    @Test
    public void CreateManagedAccount_CallEndpointTwice_Success() {
// scaMA: true

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, getIdentityTokenScaMaApp(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMaApp()))
                .body("tag", equalTo(createManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(createManagedAccountModel.getCurrency()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0));

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, getIdentityTokenScaMaApp(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMaApp()))
                .body("tag", equalTo(createManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(createManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(createManagedAccountModel.getCurrency()))
                .body("balances.availableBalance", equalTo(0))
                .body("balances.actualBalance", equalTo(0));
    }

    @Test
    public void CreateManagedAccount_NoStepUp_Forbidden() {
// scaMA: true
        final Pair<String, String> identity = createIdentity(scaMaApp);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, identity.getRight(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetManagedAccounts_ScaEnabled_Success() {
// scaMA: true

        ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, getIdentityTokenScaMaApp());

        ManagedAccountsService.getManagedAccounts(secretKeyScaMaApp, Optional.empty(), getIdentityTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("count", greaterThanOrEqualTo(1))
                .body("responseCount", greaterThanOrEqualTo(1));
    }

    @Test
    public void GetManagedAccounts_ScaMaFalse_Success() {
// scaMC: true
        ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMcApp(), CURRENCY, secretKeyScaMcApp, getIdentityTokenScaMcApp());

        ManagedAccountsService.getManagedAccounts(secretKeyScaMcApp, Optional.empty(), getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK)
                .body("count", greaterThanOrEqualTo(1))
                .body("responseCount", greaterThanOrEqualTo(1));
    }

    @Test
    public void GetManagedAccounts_SeveralManagedAccounts_Success() {
// scaMA: true

        final List<String> accounts = new ArrayList<>();

        IntStream.range(0, 3).forEach(x ->
                accounts.add(ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, getIdentityTokenScaMaApp())));

        ManagedAccountsService.getManagedAccounts(secretKeyScaMaApp, Optional.empty(), getIdentityTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("count", greaterThanOrEqualTo(accounts.size()))
                .body("responseCount", greaterThanOrEqualTo(accounts.size()));
    }

    @Test
    public void GetManagedAccounts_TwoSessions_Success() {
// scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, identity.getRight());

        final ValidatableResponse firstResponse = ManagedAccountsService.getManagedAccounts(secretKeyScaMaApp, Optional.empty(), identity.getRight())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));


        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        final String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        final ValidatableResponse secondResponse = ManagedAccountsService.getManagedAccounts(secretKeyScaMaApp, Optional.empty(), newToken)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));

        String firstCallManagedAccountId = firstResponse.extract().jsonPath().get("accounts.id").toString();
        String secondCallManagedAccountId = secondResponse.extract().jsonPath().get("accounts.id").toString();

        Assertions.assertEquals(firstCallManagedAccountId, secondCallManagedAccountId);
    }

    @Test
    public void GetManagedAccounts_CallEndpointTwice_Success() {
// scaMA: true

        ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, getIdentityTokenScaMaApp());

        final ValidatableResponse firstResponse = ManagedAccountsService.getManagedAccounts(secretKeyScaMaApp, Optional.empty(), getIdentityTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("count", greaterThanOrEqualTo(1))
                .body("responseCount", greaterThanOrEqualTo(1));

        final ValidatableResponse secondResponse = ManagedAccountsService.getManagedAccounts(secretKeyScaMaApp, Optional.empty(), getIdentityTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("count", greaterThanOrEqualTo(1))
                .body("responseCount", greaterThanOrEqualTo(1));

        String firstCallManagedAccountId = firstResponse.extract().jsonPath().get("accounts.id").toString();
        String secondCallManagedAccountId = secondResponse.extract().jsonPath().get("accounts.id").toString();
        Assertions.assertEquals(firstCallManagedAccountId, secondCallManagedAccountId);
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void GetManagedAccounts_NoStepUp_Forbidden() {
        AdminHelper.setSca(adminToken, programmeIdScaApp, false, false);

        ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaApp(), CURRENCY, secretKeyScaApp, getIdentityTokenScaApp());

        AdminHelper.setSca(adminToken, programmeIdScaApp, true, false);

        ManagedAccountsService.getManagedAccounts(secretKeyScaApp, Optional.empty(), getIdentityTokenScaApp())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetManagedAccountById_ScaEnabled_Success() {
// scaMA: true

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, getIdentityTokenScaMaApp());

        ManagedAccountsService.getManagedAccount(secretKeyScaMaApp, managedAccountId, getIdentityTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(managedAccountId))
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMaApp()));
    }

    @Test
    public void GetManagedAccountById_ScaMaFalse_Success() {
// scaMC: true
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMcApp(),
                                CURRENCY)
                        .build();
        String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKeyScaMcApp, getIdentityTokenScaMcApp());

        ManagedAccountsService.getManagedAccount(secretKeyScaMcApp, managedAccountId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(managedAccountId))
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMcApp()));
    }

    @Test
    public void GetManagedAccountById_TwoSessions_Success() {
// scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, identity.getRight());

        ManagedAccountsService.getManagedAccount(secretKeyScaMaApp, managedAccountId, identity.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(managedAccountId))
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMaApp()));

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        ManagedAccountsService.getManagedAccount(secretKeyScaMaApp, managedAccountId, newToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(managedAccountId))
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMaApp()));
    }

    @Test
    public void GetManagedAccountById_CallEndpointTwice_Success() {
// scaMA: true

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, getIdentityTokenScaMaApp());

        ManagedAccountsService.getManagedAccount(secretKeyScaMaApp, managedAccountId, getIdentityTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(managedAccountId))
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMaApp()));

        ManagedAccountsService.getManagedAccount(secretKeyScaMaApp, managedAccountId, getIdentityTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(managedAccountId))
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMaApp()));
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void GetManagedAccountById_NoStepUp_Forbidden() {
        AdminHelper.setSca(adminToken, programmeIdScaApp, false, false);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaApp(),
                                CURRENCY)
                        .build();
        String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKeyScaApp, getIdentityTokenScaApp());

        AdminHelper.setSca(adminToken, programmeIdScaApp, true, false);

        ManagedAccountsService.getManagedAccount(secretKeyScaApp, managedAccountId, getIdentityTokenScaApp())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void PatchManagedAccount_ScaEnabled_Success() {
// scaMA: true

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, getIdentityTokenScaMaApp());

        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        final ValidatableResponse response = ManagedAccountsService.patchManagedAccount(patchManagedAccountModel, secretKeyScaMaApp, managedAccountId, getIdentityTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMaApp()))
                .body("tag", equalTo(patchManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(patchManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(CURRENCY));

        String actualFriendlyName = response.extract().jsonPath().get("friendlyName");
        String actualTag = response.extract().jsonPath().get("tag");

        Assertions.assertNotEquals(createManagedAccountModel.getFriendlyName(), actualFriendlyName);
        Assertions.assertNotEquals(createManagedAccountModel.getFriendlyName(), actualTag);
    }

    @Test
    public void PatchManagedAccount_ScaMaFalse_Success() {
// scaMC: true
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMcApp(),
                                CURRENCY)
                        .build();
        String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKeyScaMcApp, getIdentityTokenScaMcApp());

        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        final ValidatableResponse response = ManagedAccountsService.patchManagedAccount(patchManagedAccountModel, secretKeyScaMcApp, managedAccountId, getIdentityTokenScaMcApp())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMcApp()))
                .body("tag", equalTo(patchManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(patchManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(CURRENCY));

        String actualFriendlyName = response.extract().jsonPath().get("friendlyName");
        String actualTag = response.extract().jsonPath().get("tag");

        Assertions.assertNotEquals(createManagedAccountModel.getFriendlyName(), actualFriendlyName);
        Assertions.assertNotEquals(createManagedAccountModel.getFriendlyName(), actualTag);
    }

    @Test
    public void PatchManagedAccount_TwoSessions_Success() {
// scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, identity.getRight());

        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        final ValidatableResponse firstResponse = ManagedAccountsService.patchManagedAccount(patchManagedAccountModel,
                        secretKeyScaMaApp, managedAccountId, identity.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMaApp()))
                .body("tag", equalTo(patchManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(patchManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(CURRENCY));

        String actualFriendlyNameFirstResponse = firstResponse.extract().jsonPath().get("friendlyName");
        String actualTagFirstResponse = firstResponse.extract().jsonPath().get("tag");

        Assertions.assertNotEquals(createManagedAccountModel.getFriendlyName(), actualFriendlyNameFirstResponse);
        Assertions.assertNotEquals(createManagedAccountModel.getFriendlyName(), actualTagFirstResponse);


        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        final PatchManagedAccountModel patchManagedAccountModelSecondResponse =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        final ValidatableResponse secondResponse = ManagedAccountsService.patchManagedAccount(patchManagedAccountModelSecondResponse, secretKeyScaMaApp,
                        managedAccountId, newToken)
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMaApp()))
                .body("tag", equalTo(patchManagedAccountModelSecondResponse.getTag()))
                .body("friendlyName", equalTo(patchManagedAccountModelSecondResponse.getFriendlyName()))
                .body("currency", equalTo(CURRENCY));

        String actualFriendlyNameSecondResponse = secondResponse.extract().jsonPath().get("friendlyName");
        String actualTagSecondResponse = secondResponse.extract().jsonPath().get("tag");

        Assertions.assertNotEquals(actualFriendlyNameFirstResponse, actualFriendlyNameSecondResponse);
        Assertions.assertNotEquals(actualTagFirstResponse, actualTagSecondResponse);
    }

    @Test
    public void PatchManagedAccount_CallEndpointTwice_Success() {
// scaMA: true

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();
        String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, getIdentityTokenScaMaApp());

        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        final ValidatableResponse firstResponse = ManagedAccountsService.patchManagedAccount(patchManagedAccountModel,
                        secretKeyScaMaApp, managedAccountId, getIdentityTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMaApp()))
                .body("tag", equalTo(patchManagedAccountModel.getTag()))
                .body("friendlyName", equalTo(patchManagedAccountModel.getFriendlyName()))
                .body("currency", equalTo(CURRENCY));

        String actualFriendlyNameFirstResponse = firstResponse.extract().jsonPath().get("friendlyName");
        String actualTagFirstResponse = firstResponse.extract().jsonPath().get("tag");

        Assertions.assertNotEquals(createManagedAccountModel.getFriendlyName(), actualFriendlyNameFirstResponse);
        Assertions.assertNotEquals(createManagedAccountModel.getFriendlyName(), actualTagFirstResponse);


        final PatchManagedAccountModel patchManagedAccountModelSecondResponse =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        final ValidatableResponse secondResponse = ManagedAccountsService.patchManagedAccount(patchManagedAccountModelSecondResponse,
                        secretKeyScaMaApp, managedAccountId, getIdentityTokenScaMaApp())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(getIdentityManagedAccountProfileScaMaApp()))
                .body("tag", equalTo(patchManagedAccountModelSecondResponse.getTag()))
                .body("friendlyName", equalTo(patchManagedAccountModelSecondResponse.getFriendlyName()))
                .body("currency", equalTo(CURRENCY));

        String actualFriendlyNameSecondResponse = secondResponse.extract().jsonPath().get("friendlyName");
        String actualTagSecondResponse = secondResponse.extract().jsonPath().get("tag");

        Assertions.assertNotEquals(actualFriendlyNameSecondResponse, actualFriendlyNameFirstResponse);
        Assertions.assertNotEquals(actualTagSecondResponse, actualTagFirstResponse);
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void PatchManagedAccount_NoStepUp_Forbidden() {
        AdminHelper.setSca(adminToken, programmeIdScaApp, false, false);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaApp(),
                                CURRENCY)
                        .build();
        String managedAccountId = ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKeyScaApp, getIdentityTokenScaApp());

        AdminHelper.setSca(adminToken, programmeIdScaApp, true, false);

        final PatchManagedAccountModel patchManagedAccountModel =
                new PatchManagedAccountModel(RandomStringUtils.randomAlphanumeric(5),
                        RandomStringUtils.randomAlphabetic(5));

        ManagedAccountsService.patchManagedAccount(patchManagedAccountModel, secretKeyScaApp, managedAccountId, getIdentityTokenScaApp())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetManagedAccountsStatement_OneSessionStepUpTrue_Success() {
// scaMA: true

        String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, getIdentityTokenScaMaApp());
        AdminHelper.fundManagedAccount(innovatorIdScaMaApp, managedAccountId, CURRENCY, 1000L);

        ManagedAccountsService.getManagedAccountStatement(secretKeyScaMaApp, managedAccountId, getIdentityTokenScaMaApp(), Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Execution(ExecutionMode.SAME_THREAD)
    @Test
    public void GetManagedAccountsStatement_OneSessionStepUpFalse_Forbidden() {
        AdminHelper.setSca(adminToken, programmeIdScaApp, false, false);

        String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaApp(), CURRENCY, secretKeyScaApp, getIdentityTokenScaApp());
        AdminHelper.fundManagedAccount(innovatorIdScaApp, managedAccountId, CURRENCY, 1000L);

        AdminHelper.setSca(adminToken, programmeIdScaApp, true, false);
        ManagedAccountsService.getManagedAccountStatement(secretKeyScaApp, managedAccountId, getIdentityTokenScaApp(), Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetManagedAccountsStatement_StepUpExpired_Forbidden() throws InterruptedException {
// scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, identity.getRight());
        AdminHelper.fundManagedAccount(innovatorIdScaMaApp, managedAccountId, CURRENCY, 1000L);
        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKeyScaMaApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        ManagedAccountsService.getManagedAccountStatement(secretKeyScaMaApp, managedAccountId, newToken, Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetManagedAccountsStatement_StepUpActive_Success() {
// scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, identity.getRight());
        AdminHelper.fundManagedAccount(innovatorIdScaMaApp, managedAccountId, CURRENCY, 1000L);

        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKeyScaMaApp, identity.getRight());

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);

        ManagedAccountsService.getManagedAccountStatement(secretKeyScaMaApp, managedAccountId, newToken, Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetManagedAccountsStatement_StepUpValidEntryLimit_SuccessLastEntry() throws InterruptedException {
// initial session: first transaction
// scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, identity.getRight());
        AdminHelper.fundManagedAccount(innovatorIdScaMaApp, managedAccountId, CURRENCY, 1000L);
        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKeyScaMaApp, identity.getRight());

//second session: second transaction and new StepUp token
        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        final String secondSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        ManagedAccountsHelper.getManagedAccountStatementForbidden(managedAccountId, secretKeyScaMaApp, secondSessionToken);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMaApp, secondSessionToken);
        AdminHelper.fundManagedAccount(innovatorIdScaMaApp, managedAccountId, CURRENCY, 1000L);
        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKeyScaMaApp, secondSessionToken);

//third session: StepUp token from the second session (no step-up token is provided, SCA was performed in the last SCA_EXPIRED_TIME).
//Retrieve last transaction -> provided only transactions posted in the last SCA_EXPIRED_TIME
        AuthenticationHelper.logout(secondSessionToken, secretKeyScaMaApp);
        final String thirdSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("limit", 1);
        ManagedAccountsService.getManagedAccountStatement(secretKeyScaMaApp, managedAccountId, thirdSessionToken, Optional.of(filters), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(2))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetManagedAccountsStatement_StepUpValidEntryLimit_ForbiddenAllEntries() throws InterruptedException {
// initial session: first transaction
// scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);
        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, identity.getRight());
        AdminHelper.fundManagedAccount(innovatorIdScaMaApp, managedAccountId, CURRENCY, 1000L);
        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKeyScaMaApp, identity.getRight());

//second session: second transaction and new StepUp token
        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        final String secondSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        ManagedAccountsHelper.getManagedAccountStatementForbidden(managedAccountId, secretKeyScaMaApp, secondSessionToken);
        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMaApp, secondSessionToken);
        AdminHelper.fundManagedAccount(innovatorIdScaMaApp, managedAccountId, CURRENCY, 1000L);
        ManagedAccountsHelper.getManagedAccountStatement(managedAccountId, secretKeyScaMaApp, secondSessionToken);

//third session: StepUp token from the second session (no step-up token is provided, SCA was performed in the last SCA_EXPIRED_TIME).
//Retrieve all transactions -> provided only transactions posted in the last SCA_EXPIRED_TIME
        AuthenticationHelper.logout(secondSessionToken, secretKeyScaMaApp);
        final String thirdSessionToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        final Map<String, Object> filters = new HashMap<>();
        filters.put("limit", 4);
        ManagedAccountsService.getManagedAccountStatement(secretKeyScaMaApp, managedAccountId, thirdSessionToken, Optional.of(filters), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void GetManagedAccountsStatement_NewStepUp_Success() throws InterruptedException {
// scaMA: true
        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final String managedAccountId = ManagedAccountsHelper.createManagedAccount(getIdentityManagedAccountProfileScaMaApp(), CURRENCY, secretKeyScaMaApp, identity.getRight());
        AdminHelper.fundManagedAccount(innovatorIdScaMaApp, managedAccountId, CURRENCY, 1000L);

        AuthenticationHelper.logout(identity.getRight(), secretKeyScaMaApp);
        Thread.sleep(SCA_EXPIRED_TIME);
        final String newToken = AuthenticationHelper.login(identity.getLeft(), secretKeyScaMaApp);
        ManagedAccountsHelper.getManagedAccountStatementForbidden(managedAccountId, secretKeyScaMaApp, newToken);

        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMaApp, newToken);
        ManagedAccountsService.getManagedAccountStatement(secretKeyScaMaApp, managedAccountId, newToken, Optional.empty(), AcceptedResponse.JSON)
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetManagedAccountsStatement_ForgotPasswordIssueStepUpAgain_Success() {

        final Pair<String, String> identity = createSteppedUpIdentity(scaMaApp);

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel.DefaultCreateManagedAccountModel(getIdentityManagedAccountProfileScaMaApp(),
                                CURRENCY)
                        .build();

        final String managedAccountId = ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKeyScaMaApp, identity.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("id");

        PasswordsService.startLostPassword(new LostPasswordStartModel(identity.getLeft()), secretKeyScaMaApp)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(identity.getLeft())
                        .setNewPassword(new PasswordModel("9876"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        final String newToken = PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKeyScaMaApp)
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("token");

        ManagedAccountsService.getManagedAccount(secretKeyScaMaApp, managedAccountId, newToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));

        AuthenticationHelper.startAndVerifyStepup(OTP_VERIFICATION_CODE, CHANNEL, secretKeyScaMaApp, newToken);

        ManagedAccountsService.getManagedAccount(secretKeyScaMaApp, managedAccountId, newToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(managedAccountId))
                .body("state.state", equalTo(State.ACTIVE.name()));
    }
}
