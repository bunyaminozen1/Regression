package spi.openbanking.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OpenBankingAccountsResponseModel {

    private MetaResponseModel meta;
    private List<OpenBankingAccountResponseModel> data;
}
