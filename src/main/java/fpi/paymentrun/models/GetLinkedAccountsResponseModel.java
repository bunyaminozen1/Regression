package fpi.paymentrun.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GetLinkedAccountsResponseModel {
    private List<LinkedAccountResponseModel> linkedAccounts;
    private int count;
    private int responseCount;
}
