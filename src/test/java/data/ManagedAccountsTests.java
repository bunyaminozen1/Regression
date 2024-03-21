package data;

import commons.enums.Currency;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.services.multi.ManagedAccountsService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Optional;
import java.util.stream.IntStream;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

public class ManagedAccountsTests extends BaseTestSetup {

    private static String corporateAuthenticationToken;

    @BeforeAll
    public static void Setup(){
        corporateSetup();
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void CreateManagedAccount_Success(final Currency currency) {

        IntStream.range(0, new RandomDataGenerator().nextInt(1, 5)).forEach(i -> {
            final CreateManagedAccountModel createManagedAccountModel =
                    CreateManagedAccountModel
                            .dataCreateManagedAccountModel(corporateManagedAccountsProfileId,
                                    currency)
                            .build();

            ManagedAccountsService.createManagedAccount(createManagedAccountModel, secretKey, corporateAuthenticationToken, Optional.empty())
                    .then()
                    .statusCode(SC_OK);
        });
    }

    @Test
    public void CreateManagedAccount_Blocked_Success() {

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .dataCreateManagedAccountModel(corporateManagedAccountsProfileId, Currency.EUR)
                        .build();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(createManagedAccountModel,
                        secretKey, corporateAuthenticationToken);

        ManagedAccountsService.blockManagedAccount(secretKey, managedAccountId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void CreateManagedAccount_Unblocked_Success() {

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .dataCreateManagedAccountModel(corporateManagedAccountsProfileId, Currency.EUR)
                        .build();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(createManagedAccountModel,
                        secretKey, corporateAuthenticationToken);

        ManagedAccountsService.blockManagedAccount(secretKey, managedAccountId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ManagedAccountsService.unblockManagedAccount(secretKey, managedAccountId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void CreateManagedAccount_Removed_Success() {

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .dataCreateManagedAccountModel(corporateManagedAccountsProfileId, Currency.EUR)
                        .build();

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(createManagedAccountModel,
                        secretKey, corporateAuthenticationToken);

        ManagedAccountsService.removeManagedAccount(secretKey, managedAccountId, corporateAuthenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void CreateManagedAccount_Upgraded_Success() {

        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .dataCreateManagedAccountModel(corporateManagedAccountsProfileId, Currency.EUR)
                        .build();

        ManagedAccountsHelper.createUpgradedManagedAccount(createManagedAccountModel,
                secretKey, corporateAuthenticationToken);
    }

    private static void corporateSetup() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.dataCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = createSteppedUpCorporate(createCorporateModel, secretKey);
        corporateAuthenticationToken = authenticatedCorporate.getRight();
    }
}
