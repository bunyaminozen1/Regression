package opc.models.shared;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.RandomStringUtils;

@Builder
@Getter
@Setter
public class ManagedAccountDetailsModel {
    private String sortCode;
    private String accountNumber;


    public static ManagedAccountDetailsModel.ManagedAccountDetailsModelBuilder DefaultManagedAccountDetailsModel() {

        return ManagedAccountDetailsModel.builder()
                .sortCode(RandomStringUtils.randomNumeric(6))
                .accountNumber(RandomStringUtils.randomNumeric(8));
    }
}
