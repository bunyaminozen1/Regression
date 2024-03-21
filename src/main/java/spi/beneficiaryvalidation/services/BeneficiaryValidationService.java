package spi.beneficiaryvalidation.services;

import commons.services.BaseService;
import io.restassured.response.Response;
import spi.beneficiaryvalidation.models.BeneficiaryValidationModel;
import spi.beneficiaryvalidation.models.BulkBeneficiaryValidationModel;

public class BeneficiaryValidationService extends BaseService {

    public static Response validateBeneficiary(final BeneficiaryValidationModel beneficiaryValidationModel,
                                               final String authenticationToken){
        return getBodyAuthorisationKeyRequest(beneficiaryValidationModel, authenticationToken)
                .when()
                .post("/beneficiary-validation/1.0/validate");
    }

    public static Response validateBeneficiaries(final BulkBeneficiaryValidationModel bulkBeneficiaryValidationModel,
                                                 final String authenticationToken){
        return getBodyAuthorisationKeyRequest(bulkBeneficiaryValidationModel, authenticationToken)
                .when()
                .post("/beneficiary-validation/1.0/validate/bulk");
    }
}
