package spi.openbanking.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OpenBankingInstitutionsResponseModel {

    private MetaResponseModel meta;
    private List<OpenBankingInstitutionResponseModel> data;
}
