package opc.junit.multi.sends;

import commons.enums.Currency;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class CorporateSendOtpVerificationAttemptLimitTests extends AbstractSendOtpVerificationAttemptLimitTests{

    private static String sourceIdentityToken;
    private static final String currency = Currency.EUR.name();
    private static String sourceManagedAccount;
    private static String destinationManagedCard;
    private static String destinationIdentityToken;

    @BeforeAll
    public static void Setup() {

        corporateSetupSource();
        corporateSetupDestination();

        sourceManagedAccount =
                createManagedAccount(secondaryScaApp.getCorporatePayneticsEeaManagedAccountsProfileId(), currency, secondaryScaApp.getSecretKey(), sourceIdentityToken).getLeft();
        destinationManagedCard =
                createPrepaidManagedCard(secondaryScaApp.getCorporateNitecrestEeaPrepaidManagedCardsProfileId(), currency, secondaryScaApp.getSecretKey(), destinationIdentityToken).getLeft();
    }

    private static void corporateSetupSource() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(secondaryScaApp.getCorporatesProfileId()).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, secondaryScaApp.getSecretKey());
        sourceIdentityToken = authenticatedCorporate.getRight();
    }

    private static void corporateSetupDestination() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(secondaryScaApp.getCorporatesProfileId()).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(createCorporateModel, secondaryScaApp.getSecretKey());
        destinationIdentityToken = authenticatedCorporate.getRight();
    }

    @Override
    protected String getSourceIdentityToken() {return sourceIdentityToken; }

    @Override
    protected String getSourceManagedAccount() {return sourceManagedAccount;}

    @Override
    protected String getDestinationManagedCard() {return destinationManagedCard;}
}
