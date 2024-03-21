package opc.models.admin;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Builder
@Getter
@Setter
public class UpdateModulrCorporateModel {

    private ModulrSubscriptionInfoModel subscriptionInfo;
}
