package opc.models.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RejectDepositModel {
    private String note;
}
