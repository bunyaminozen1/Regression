package spi.beneficiaryvalidation.beneficiaryvalidation;

import org.junit.jupiter.api.Test;
import spi.beneficiaryvalidation.helpers.BeneficiaryHelper;
import spi.beneficiaryvalidation.models.BankAccountDetailsModel;
import spi.beneficiaryvalidation.models.BeneficiaryValidationModel;
import spi.beneficiaryvalidation.services.BeneficiaryValidationService;

import static org.apache.http.HttpStatus.SC_OK;

public class BeneficiaryValidationTests {

    @Test
    public void ValidateBeneficiary_Success() {

        final BeneficiaryValidationModel beneficiaryValidationModel =
                BeneficiaryValidationModel.builder()
                        .bankAccountDetails(BankAccountDetailsModel.builder()
                                .accountNumber("473983335")
                                .sortCode("718214")
                                .build())
                        .build();

        BeneficiaryValidationService.validateBeneficiary(beneficiaryValidationModel, BeneficiaryHelper.API_KEY)
                .then()
                .statusCode(SC_OK);
    }
}
