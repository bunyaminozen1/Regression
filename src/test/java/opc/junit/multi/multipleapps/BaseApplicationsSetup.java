package opc.junit.multi.multipleapps;

import opc.enums.opc.FeeType;
import opc.enums.opc.InnovatorSetup;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.multi.BaseSetupExtension;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.multi.sends.SendFundsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import opc.models.shared.ProgrammeDetailsModel;
import opc.services.multi.ManagedAccountsService;
import opc.services.multi.ManagedCardsService;
import opc.services.multi.SendsService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static opc.enums.opc.ManagedInstrumentType.MANAGED_ACCOUNTS;
import static opc.enums.opc.ManagedInstrumentType.MANAGED_CARDS;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

@Execution(ExecutionMode.CONCURRENT)
@Tag(MultiTags.MULTI)
public class BaseApplicationsSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel applicationTwo;
    protected static ProgrammeDetailsModel applicationThree;
    protected static ProgrammeDetailsModel applicationFour;

    @BeforeAll
    public static void GlobalSetup(){
        applicationTwo = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_TWO);
        applicationThree = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_THREE);
        applicationFour = (ProgrammeDetailsModel) setupExtension.store.get(InnovatorSetup.APPLICATION_FOUR);
    }

    protected static List<Pair<String, CreateManagedAccountModel>> createManagedAccounts(final String managedAccountProfileId,
                                                                                         final String currency,
                                                                                         final String authenticationToken,
                                                                                         final int noOfAccounts,
                                                                                         final String secretKey){
        final List<Pair<String, CreateManagedAccountModel>> accounts = new ArrayList<>();

        IntStream.range(0, noOfAccounts).forEach(x -> accounts.add(createManagedAccount(managedAccountProfileId, currency, authenticationToken, secretKey)));

        return accounts;
    }

    protected static Pair<String, CreateManagedAccountModel> createManagedAccount(final String managedAccountProfileId,
                                                                                  final String currency,
                                                                                  final String authenticationToken,
                                                                                  final String secretKey){
        final CreateManagedAccountModel createManagedAccountModel =
                CreateManagedAccountModel
                        .DefaultCreateManagedAccountModel(managedAccountProfileId,
                                currency)
                        .build();

        final String corporateManagedAccountId =
                ManagedAccountsHelper.createManagedAccount(createManagedAccountModel, secretKey, authenticationToken);

        return Pair.of(corporateManagedAccountId, createManagedAccountModel);
    }

    protected int simulateManagedAccountDepositAndCheckBalance(final String managedAccountId,
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

    protected int simulateManagedCardDepositAndCheckBalance(final String managedAccountProfileId,
                                                            final String sendsProfileId,
                                                            final String managedCardId,
                                                            final String currency,
                                                            final Long depositAmount,
                                                            final String secretKey,
                                                            final String authenticationToken){

        final String managedAccountForCardFunding =
                createManagedAccount(managedAccountProfileId, currency, authenticationToken, secretKey).getLeft();

        TestHelper.simulateManagedAccountDeposit(managedAccountForCardFunding, currency, 11000L, secretKey, authenticationToken);

        final SendFundsModel sendFundsModel =
                SendFundsModel.newBuilder()
                        .setProfileId(sendsProfileId)
                        .setTag(RandomStringUtils.randomAlphabetic(5))
                        .setDestinationAmount(new CurrencyAmount(currency, depositAmount))
                        .setSource(new ManagedInstrumentTypeId(managedAccountForCardFunding, MANAGED_ACCOUNTS))
                        .setDestination(new ManagedInstrumentTypeId(managedCardId, MANAGED_CARDS))
                        .build();

        SendsService.sendFunds(sendFundsModel, secretKey, authenticationToken, Optional.empty())
                .then()
                .statusCode(SC_OK);

        ManagedCardsService.getManagedCard(secretKey, managedCardId, authenticationToken)
                .then()
                .body("balances.availableBalance", equalTo(depositAmount.intValue()))
                .body("balances.actualBalance", equalTo(depositAmount.intValue()));

        return depositAmount.intValue();
    }

    protected static List<Pair<String, CreateManagedCardModel>> createManagedCards(final String managedCardProfileId,
                                                                                   final String currency,
                                                                                   final String authenticationToken,
                                                                                   final int noOfAccounts,
                                                                                   final String secretKey){
        final List<Pair<String, CreateManagedCardModel>> cards = new ArrayList<>();

        IntStream.range(0, noOfAccounts)
                .forEach(x -> cards.add(createPrepaidManagedCard(managedCardProfileId, currency, authenticationToken, secretKey)));

        return cards;
    }

    protected static Pair<String, CreateManagedCardModel> createPrepaidManagedCard(final String managedCardProfileId,
                                                                                   final String currency,
                                                                                   final String authenticationToken,
                                                                                   final String secretKey){
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreatePrepaidManagedCardModel(managedCardProfileId,
                                currency)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, authenticationToken);

        return Pair.of(managedCardId, createManagedCardModel);
    }

    protected static Pair<String, CreateManagedCardModel> createDebitManagedCard(final String managedCardProfileId,
                                                                                 final String managedAccountId,
                                                                                 final String authenticationToken,
                                                                                 final String secretKey){
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreateDebitManagedCardModel(managedCardProfileId,
                                managedAccountId)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, authenticationToken);

        return Pair.of(managedCardId, createManagedCardModel);
    }
}
