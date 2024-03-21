package spi.beneficiaryvalidation.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class BeneficiaryValidationModel {
    private BankAccountDetailsModel bankAccountDetails;
}
