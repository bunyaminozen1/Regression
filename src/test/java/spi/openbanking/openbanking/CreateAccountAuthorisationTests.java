package spi.openbanking.openbanking;

import org.junit.jupiter.api.Test;
import spi.openbanking.helpers.OpenBankingHelper;
import spi.openbanking.models.CreateAccountAuthorisationModel;
import spi.openbanking.services.OpenBankingService;

import static org.apache.http.HttpStatus.SC_CREATED;
import static org.hamcrest.CoreMatchers.notNullValue;

public class CreateAccountAuthorisationTests {

    @Test
    public void CreateAccountAuthorisation_Success() {

        final CreateAccountAuthorisationModel createAccountAuthorisationModel =
                CreateAccountAuthorisationModel.builder()
                        .institutionId("modelo-sandbox")
                        .callbackUrl("https://example.com/")
                        .state("EXAMPLE")
                        .build();

        OpenBankingService.createAccountAuthorisation(createAccountAuthorisationModel, OpenBankingHelper.API_KEY)
                .then()
                .statusCode(SC_CREATED)
                .body("meta.tracingId", notNullValue())
                .body("data.authorisationUrl", notNullValue());
    }
}
