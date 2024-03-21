package spi.openbanking.openbanking;

import org.junit.jupiter.api.Test;
import spi.openbanking.helpers.OpenBankingHelper;
import spi.openbanking.models.OpenBankingInstitutionResponseModel;
import spi.openbanking.models.OpenBankingInstitutionsResponseModel;
import spi.openbanking.services.OpenBankingService;

import java.util.List;

import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GetInstitutionsTests {

    @Test
    public void GetInstitutions_Success() {

        final List<OpenBankingInstitutionResponseModel> institutions =
                OpenBankingHelper.getInstitutions();

        final OpenBankingInstitutionsResponseModel retrievedInstitutions =
                OpenBankingService.getInstitutions(OpenBankingHelper.API_KEY)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .as(OpenBankingInstitutionsResponseModel.class);

        assertEquals(institutions.size(), retrievedInstitutions.getMeta().getCount());
        assertNotNull(retrievedInstitutions.getMeta().getTracingId());

        institutions.forEach(institution -> {
            final OpenBankingInstitutionResponseModel retrievedInstitution =
                    retrievedInstitutions.getData()
                            .stream()
                            .filter(x -> x.getId().equals(institution.getId()))
                            .findFirst()
                            .orElseThrow();
            assertEquals(institution.getId(), retrievedInstitution.getId());
            assertEquals(institution.getDisplayName(), retrievedInstitution.getDisplayName());
            assertEquals(institution.getCountries(), retrievedInstitution.getCountries());
            assertEquals(institution.getEnvironmentType(), retrievedInstitution.getEnvironmentType());
            assertEquals(institution.getReleaseStage(), retrievedInstitution.getReleaseStage());
            assertEquals(institution.getImages().getLogo(), retrievedInstitution.getImages().getLogo());
            assertEquals(institution.getImages().getIcon(), retrievedInstitution.getImages().getIcon());
            assertEquals(institution.getInfo().getLoginUrl(), retrievedInstitution.getInfo().getLoginUrl());
            assertEquals(institution.getInfo().getHelplinePhoneNumber(), retrievedInstitution.getInfo().getHelplinePhoneNumber());
            assertEquals(institution.getCapabilities().getMaxAmount(), retrievedInstitution.getCapabilities().getMaxAmount());
            assertEquals(institution.getCapabilities().isPaymentSingleImmediate(), retrievedInstitution.getCapabilities().isPaymentSingleImmediate());
            assertEquals(institution.getCapabilities().isStatusSingleImmediate(), retrievedInstitution.getCapabilities().isStatusSingleImmediate());
            assertEquals(institution.getCapabilities().getDualAuth(), retrievedInstitution.getCapabilities().getDualAuth());
        });
    }
}
