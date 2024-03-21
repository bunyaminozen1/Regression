package fpi.spiopenbanking;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.paymentrun.services.SpiOpenBanking.SpiOpenBankingService;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class GetInstitutionTests extends BasePaymentRunSetup {

    @Test
    public void GetInstitutions_NoFilters_Success() {
        SpiOpenBankingService.getInstitutions(Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("institutions", notNullValue());
    }

    @Test
    public void GetInstitutions_ReleaseStageFilter_Success() {
        final Map<String, Object> filters = new HashMap<>();
        filters.put("releaseStage[]", "live");

        SpiOpenBankingService.getInstitutions(Optional.of(filters))
                .then()
                .statusCode(SC_OK)
                .body("institutions", notNullValue())
                .body("institutions[0].releaseStage", equalTo("live"));
    }

    @Test
    public void GetInstitutions_EnvironmentTypeFilter_Success() {
        final Map<String, Object> filters = new HashMap<>();
        filters.put("environmentType", "mock");

        SpiOpenBankingService.getInstitutions(Optional.of(filters))
                .then()
                .statusCode(SC_OK)
                .body("institutions", notNullValue())
                .body("institutions[0].environmentType", equalTo("mock"));
    }

    @Test
    public void GetInstitutions_CountryFilter_Success() {
        final Map<String, Object> filters = new HashMap<>();
        filters.put("country", "GB");

        SpiOpenBankingService.getInstitutions(Optional.of(filters))
                .then()
                .statusCode(SC_OK)
                .body("institutions", notNullValue())
                .body("institutions[0].countries", equalTo(List.of("GB")));
    }

    @Test
    public void GetInstitutions_InstitutionIdFilter_Success() {
        final Map<String, Object> filters = new HashMap<>();
        filters.put("institutionIds[]", "natwest");

        SpiOpenBankingService.getInstitutions(Optional.of(filters))
                .then()
                .statusCode(SC_OK)
                .body("institutions", notNullValue())
                .body("institutions[0].id", equalTo("natwest"));
    }

    @Test
    public void GetInstitutions_InstitutionNameFilter_Success() {
        final Map<String, Object> filters = new HashMap<>();
        filters.put("institutionName", "Natwest");

        SpiOpenBankingService.getInstitutions(Optional.of(filters))
                .then()
                .statusCode(SC_OK)
                .body("institutions", notNullValue())
                .body("institutions[0].displayName", equalTo("Natwest"));
    }

    @Test
    public void GetInstitutions_MarketFilter_Success() {
        final Map<String, Object> filters = new HashMap<>();
        filters.put("market", "EEA");

        SpiOpenBankingService.getInstitutions(Optional.of(filters))
                .then()
                .statusCode(SC_OK)
                .body("institutions", notNullValue())
                .body("institutions[0].markets", equalTo(List.of("EEA")));
    }

    @Test
    public void GetInstitutions_LimitFilter_Success() {
        final Map<String, Object> filters = new HashMap<>();
        filters.put("offset", 0);
        filters.put("limit", 1);

        SpiOpenBankingService.getInstitutions(Optional.of(filters))
                .then()
                .statusCode(SC_OK)
                .body("institutions.size()", equalTo(1));
    }

    @Test
    public void GetInstitutions_AllFilters_Success() {
        final Map<String, Object> filters = new HashMap<>();
        filters.put("releaseStage[]", "live");
        filters.put("environmentType", "live");
        filters.put("country", "GB");
        filters.put("institutionName", "Natwest");
        filters.put("institutionIds[]", "natwest");
        filters.put("market", "UK");

        SpiOpenBankingService.getInstitutions(Optional.of(filters))
                .then()
                .statusCode(SC_OK)
                .body("institutions.size()", equalTo(1))
                .body("institutions[0].id", equalTo("natwest"))
                .body("institutions[0].displayName", equalTo("Natwest"))
                .body("institutions[0].markets", equalTo(List.of("UK")))
                .body("institutions[0].countries", equalTo(List.of("GB")))
                .body("institutions[0].environmentType", equalTo("live"))
                .body("institutions[0].releaseStage", equalTo("live"))
                .body("institutions[0].currencies", equalTo(List.of("GBP")));
    }

}
