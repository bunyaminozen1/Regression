package fpi.paymentrun.models.simulator;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import opc.models.multi.outgoingwiretransfers.FasterPaymentsBankDetailsModel;
import org.apache.commons.lang3.RandomStringUtils;

@Builder
@Getter
@Setter
public class SimulateLinkedAccountModel {

    private final String institutionId;
    private final String buyerId;
    private final String createdBy;
    private final FasterPaymentsBankDetailsModel accountIdentification;

    public static SimulateLinkedAccountModel.SimulateLinkedAccountModelBuilder defaultSimulateLinkedAccountModel(final String institutionId,
                                                                                                                 final String buyerId){
        return SimulateLinkedAccountModel.builder()
                .institutionId(institutionId)
                .buyerId(buyerId)
                .createdBy(buyerId)
                .accountIdentification(new FasterPaymentsBankDetailsModel(RandomStringUtils.randomNumeric(8),
                        RandomStringUtils.randomNumeric(6).toUpperCase()));
    }

    public static SimulateLinkedAccountModel.SimulateLinkedAccountModelBuilder defaultSimulateLinkedAccountModel(final String buyerId){
        return SimulateLinkedAccountModel.builder()
                .institutionId("natwest-sandbox")
                .buyerId(buyerId)
                .createdBy(buyerId)
                .accountIdentification(new FasterPaymentsBankDetailsModel(RandomStringUtils.randomNumeric(8),
                        RandomStringUtils.randomNumeric(6).toUpperCase()));
    }

}
