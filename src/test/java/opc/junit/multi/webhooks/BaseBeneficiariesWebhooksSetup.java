package opc.junit.multi.webhooks;

import io.restassured.response.Response;
import opc.enums.opc.InnovatorSetup;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.junit.helpers.webhook.WebhookHelper;
import opc.junit.multi.BaseSetupExtension;
import opc.models.innovator.UpdateProgrammeModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.models.multi.managedcards.CreateManagedCardModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.tags.MultiTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Execution(ExecutionMode.CONCURRENT)
@Tag(MultiTags.BENEFICIARY_WEBHOOKS)
public class BaseBeneficiariesWebhooksSetup {

    @RegisterExtension
    static BaseSetupExtension setupExtension = new BaseSetupExtension();

    protected static ProgrammeDetailsModel passcodeApp;
    protected static ProgrammeDetailsModel destinationApp;

    protected static String corporateProfileId;
    protected static String consumerProfileId;
    protected static String secretKey;
    protected static String sharedKey;
    protected static String programmeId;

    protected static String destinationCorporateProfileId;
    protected static String destinationConsumerProfileId;
    protected static String destinationSecretKey;
    protected static String destinationSharedKey;
    protected static String destinationProgrammeId;

    protected static String destinationCorporatePrepaidManagedCardsProfileId;
    protected static String destinationConsumerPrepaidManagedCardsProfileId;
    protected static String destinationCorporateDebitManagedCardsProfileId;
    protected static String destinationConsumerDebitManagedCardsProfileId;
    protected static String destinationCorporateManagedAccountsProfileId;
    protected static String destinationConsumerManagedAccountsProfileId;

    protected static String innovatorToken;

    protected static Pair<String, String> webhookServiceDetails;

    @BeforeAll
    public static void GlobalSetup() {

        //Source
        passcodeApp = (ProgrammeDetailsModel) setupExtension.store
                .get(InnovatorSetup.PASSCODE_APP);

        corporateProfileId = passcodeApp.getCorporatesProfileId();
        consumerProfileId = passcodeApp.getConsumersProfileId();
        secretKey = passcodeApp.getSecretKey();
        sharedKey = passcodeApp.getSharedKey();
        programmeId = passcodeApp.getProgrammeId();

        innovatorToken = InnovatorHelper.loginInnovator(passcodeApp.getInnovatorEmail(),
                passcodeApp.getInnovatorPassword());

        //Destination
        destinationApp = (ProgrammeDetailsModel) setupExtension.store
                .get(InnovatorSetup.APPLICATION_ONE);
        destinationCorporateProfileId = destinationApp.getCorporatesProfileId();
        destinationConsumerProfileId = destinationApp.getConsumersProfileId();
        destinationSecretKey = destinationApp.getSecretKey();
        destinationSharedKey = destinationApp.getSharedKey();
        destinationProgrammeId = destinationApp.getProgrammeId();

        destinationCorporatePrepaidManagedCardsProfileId = destinationApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId();
        destinationConsumerPrepaidManagedCardsProfileId = destinationApp.getConsumerNitecrestEeaPrepaidManagedCardsProfileId();
        destinationCorporateDebitManagedCardsProfileId = destinationApp.getCorporateNitecrestEeaDebitManagedCardsProfileId();
        destinationConsumerDebitManagedCardsProfileId = destinationApp.getConsumerNitecrestEeaDebitManagedCardsProfileId();
        destinationCorporateManagedAccountsProfileId = destinationApp.getCorporatePayneticsEeaManagedAccountsProfileId();
        destinationConsumerManagedAccountsProfileId = destinationApp.getConsumerPayneticsEeaManagedAccountsProfileId();
        final String innovatorEmail = passcodeApp.getInnovatorEmail();
        final String innovatorPassword = passcodeApp.getInnovatorPassword();

        innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
        webhookServiceDetails = WebhookHelper.generateWebhookUrl();

        InnovatorHelper.enableWebhook(UpdateProgrammeModel.WebHookUrlSetup(programmeId, false, webhookServiceDetails.getRight()),
                programmeId, InnovatorHelper.loginInnovator(passcodeApp.getInnovatorEmail(), passcodeApp.getInnovatorPassword()));
    }

    protected static List<String> createManagedAccounts(final String managedAccountProfileId,
                                                        final String currency,
                                                        final String secretKey,
                                                        final String authenticationToken,
                                                        final int noOfAccounts) {
        final List<String> accounts = new ArrayList<>();

        IntStream.range(0, noOfAccounts).forEach(x -> accounts.add(createManagedAccount(managedAccountProfileId, currency, secretKey, authenticationToken).getLeft()));

        return accounts;
    }

    protected static Pair<String, CreateManagedAccountModel> createManagedAccount(final String managedAccountProfileId,
                                                                                  final String currency,
                                                                                  final String secretKey,
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

    protected static Pair<String, CreateManagedCardModel> createPrepaidManagedCard(final String managedCardProfileId,
                                                                                   final String currency,
                                                                                   final String secretKey,
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

    protected static Pair<String, CreateManagedCardModel> createDebitManagedCard(final String managedCardProfileId,
                                                                                 final String managedAccountId,
                                                                                 final String secretKey,
                                                                                 final String authenticationToken) {
        final CreateManagedCardModel createManagedCardModel =
                CreateManagedCardModel
                        .DefaultCreateDebitManagedCardModel(managedCardProfileId,
                                managedAccountId)
                        .build();

        final String managedCardId =
                ManagedCardsHelper.createManagedCard(createManagedCardModel, secretKey, authenticationToken);

        return Pair.of(managedCardId, createManagedCardModel);
    }

    protected static String getBeneficiaryBatchId(Response response) {
        return response.jsonPath().get("operationBatchId.batchId");
    }
}
