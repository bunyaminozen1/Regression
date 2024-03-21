package spi.beneficiaryvalidation.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
public class BulkBeneficiaryValidationModel {

    private List<BankAccountDetailsModel> bankAccountDetails;
}
