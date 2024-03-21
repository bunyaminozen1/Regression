package opc.junit.multi.multipleapps;

import commons.enums.Currency;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.services.multi.ManagedAccountsService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class ManagedAccountsTests extends BaseApplicationsSetup {

    private static String consumerProfileId;
    private static String managedAccountsProfileId;
    private static String secretKey;
    private static String authenticationToken;

    @BeforeAll
    public static void TestSetup(){
        consumerProfileId = applicationTwo.getConsumersProfileId();
        managedAccountsProfileId = applicationTwo.getConsumerPayneticsEeaManagedAccountsProfileId();
        secretKey = applicationTwo.getSecretKey();

        consumerSetup();
    }

    @Test
    public void CreateManagedAccount_Success(){
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(managedAccountsProfileId, Currency.getRandomCurrency().name())
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("profileId", equalTo(managedAccountsProfileId));
    }

    @Test
    public void CreateManagedAccount_BusinessPurchasingProfile_Forbidden(){

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(applicationThree.getConsumerPayneticsEeaManagedAccountsProfileId(),
                                Currency.getRandomCurrency().name())
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreateManagedAccount_BusinessPayoutsProfile_Forbidden(){

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(applicationFour.getConsumerPayneticsEeaManagedAccountsProfileId(),
                                Currency.getRandomCurrency().name())
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreateManagedAccount_BusinessPurchasingKey_Forbidden(){

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(managedAccountsProfileId,
                                Currency.getRandomCurrency().name())
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, applicationThree.getSecretKey(), authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreateManagedAccount_BusinessPayoutsKey_Forbidden(){

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(managedAccountsProfileId,
                                Currency.getRandomCurrency().name())
                        .build();

        ManagedAccountsService.createManagedAccount(createManagedAccountModel, applicationFour.getSecretKey(), authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        authenticationToken = authenticatedConsumer.getRight();

        ConsumersHelper.verifyKyc(secretKey, authenticatedConsumer.getLeft());
    }
}
