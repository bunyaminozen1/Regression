package fpi.paymentrun.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import opc.enums.opc.CompanyType;
import opc.enums.opc.CountryCode;
import opc.models.shared.AddressModel;
import org.apache.commons.lang3.RandomStringUtils;

@Builder
@Getter
@Setter
public class CompanyModel {
    private final String type;
    private final AddressModel businessAddress;
    private final String name;
    private final String registrationNumber;
    private final String countryOfRegistration;

    public static CompanyModel.CompanyModelBuilder defaultCompanyModel(){
        return CompanyModel.builder()
                .type(CompanyType.getRandomWithExcludedCompanyType(CompanyType.NON_PROFIT_ORGANISATION).name())
                .name(RandomStringUtils.randomAlphabetic(6))
                .businessAddress(AddressModel.RandomAddressModel())
                .registrationNumber(RandomStringUtils.randomAlphanumeric(10))
                // TODO List of accepted country codes at random
                .countryOfRegistration(CountryCode.MT.name());
    }

    public static CompanyModel.CompanyModelBuilder gbCompanyModel(){
        return CompanyModel.builder()
                .type(CompanyType.getRandomWithExcludedCompanyType(CompanyType.NON_PROFIT_ORGANISATION).name())
                .name(RandomStringUtils.randomAlphabetic(6))
                .businessAddress(AddressModel.RandomAddressModel())
                .registrationNumber(RandomStringUtils.randomAlphanumeric(10))
                .countryOfRegistration(CountryCode.GB.name());
    }
}
