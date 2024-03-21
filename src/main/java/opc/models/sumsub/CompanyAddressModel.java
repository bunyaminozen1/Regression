package opc.models.sumsub;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyAddressModel {

    private String town;
    private String country;
    private String postCode;

    public CompanyAddressModel(final String town, final String postCode) {
        this.town = town;
        this.postCode = postCode;
    }

    public CompanyAddressModel(final String town, final String country, final String postCode) {
        this.town = town;
        this.country = country;
        this.postCode = postCode;
    }
}
