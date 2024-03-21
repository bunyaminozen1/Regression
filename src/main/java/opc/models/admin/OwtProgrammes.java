package opc.models.admin;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import opc.models.innovator.FeeModel;

import java.util.List;

@Data
@Builder
@Getter
@Setter
public class OwtProgrammes {
    private final String code;
    private final String payletTypeCode;
    private final String supportedType;
    private final Boolean skipWithdrawalSca;
    private final Boolean blockWithdrawalToExternalAccounts;
    private final List<FeesOWTModel> fee;
}
