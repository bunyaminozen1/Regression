package opc.models.multi.sends;

import lombok.Getter;
import lombok.Setter;
import opc.models.shared.CurrencyAmountResponse;
import opc.models.shared.TypeIdResponseModel;

@Getter
@Setter
public class BulkSendResponseModel {

    private TypeIdResponseModel destination;
    private CurrencyAmountResponse destinationAmount;
    private String id;
    private String profileId;
    private TypeIdResponseModel source;
    private String tag;
    private String scheduledTimestamp;
}
