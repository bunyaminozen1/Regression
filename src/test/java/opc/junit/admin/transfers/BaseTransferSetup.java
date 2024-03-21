package opc.junit.admin.transfers;

import opc.enums.opc.InnovatorSetup;
import opc.junit.admin.BaseSetupExtension;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.shared.ProgrammeDetailsModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class BaseTransferSetup {
    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();
    protected static ProgrammeDetailsModel applicationOne;
    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String corporateManagedAccountProfileId;
    protected static String consumerManagedAccountProfileId;
    protected static String corporatePrepaidManagedCardsProfileId;
    protected static String consumerPrepaidManagedCardsProfileId;
    protected static String corporateDebitManagedCardsProfileId;
    protected static String consumerDebitManagedCardsProfileId;
    protected static String transfersProfileId;
    protected static String secretKey;
    protected static String innovatorId;

    @BeforeAll
    public static void GlobalSetup() {

        applicationOne = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_ONE);

        innovatorId = applicationOne.getInnovatorId();

        corporateProfileId = applicationOne.getCorporatesProfileId();
        consumerProfileId = applicationOne.getConsumersProfileId();

        corporatePrepaidManagedCardsProfileId = applicationOne.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        consumerPrepaidManagedCardsProfileId = applicationOne.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        corporateDebitManagedCardsProfileId = applicationOne.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        consumerDebitManagedCardsProfileId = applicationOne.getConsumerNitecrestEeaDebitManagedCardsProfileId();
        corporateManagedAccountProfileId = applicationOne.getCorporatePayneticsEeaManagedAccountsProfileId();
        consumerManagedAccountProfileId = applicationOne.getConsumerPayneticsEeaManagedAccountsProfileId();

        secretKey = applicationOne.getSecretKey();
        transfersProfileId = applicationOne.getTransfersProfileId();
        innovatorId = applicationOne.getInnovatorId();
    }

    protected static List<Pair<String, CreateManagedAccountModel>> createManagedAccounts(final String managedAccountProfileId,
                                                                                         final String currency,
                                                                                         final String authenticationToken,
                                                                                         final int noOfAccounts) {
        final List<Pair<String, CreateManagedAccountModel>> accounts = new ArrayList<>();

        IntStream.range(0, noOfAccounts).forEach(x -> accounts.add(createManagedAccount(managedAccountProfileId, currency,
                authenticationToken)));

        return accounts;
    }

    protected static Pair<String, CreateManagedAccountModel> createManagedAccount(final String managedAccountProfileId,
                                                                                  final String currency,
                                                                                  final String authenticationToken) {
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(managedAccountProfileId,
                                currency)
                        .build();

        final String corporateManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, authenticationToken);

        return Pair.of(corporateManagedAccountId, createManagedAccountModel);
    }

    protected void fundManagedAccount(final String managedAccountId,
                                      final String currency,
                                      final Long depositAmount,
                                      final String innovatorId) {
        AdminHelper.fundManagedAccount(innovatorId, managedAccountId, currency, depositAmount);
    }

    protected static void fundManagedAccount(final String managedAccountId,
                                             final String currency,
                                             final Long depositAmount) {
        AdminHelper.fundManagedAccount(innovatorId, managedAccountId, currency, depositAmount);
    }

    protected static Pair<String, CreateManagedCardModel> createPrepaidManagedCard(final String managedCardProfileId,
                                                                                   final String currency,
                                                                                   final String authenticationToken) {
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreatePrepaidManagedCardModel(managedCardProfileId,
                                currency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, authenticationToken);

        return Pair.of(managedCardId, createManagedCardModel);
    }

    protected void fundManagedCard(final String managedCardId,
                                   final String currency,
                                   final Long depositAmount) {

        AdminHelper.fundManagedCard(innovatorId, managedCardId, currency, depositAmount);
    }
}
