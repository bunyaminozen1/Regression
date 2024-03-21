package fpi.paymentrun.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import opc.models.shared.AddressModel;

@Builder
@Getter
@Setter
public class UpdateCompanyModel {

    private AddressModel businessAddress;

    public static UpdateCompanyModel.UpdateCompanyModelBuilder defaultCompanyModel(){
        return UpdateCompanyModel.builder()
                .businessAddress(AddressModel.RandomAddressModel());
    }
}
