package opc.models.multi.challenges;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChallengesModel {
    private String resourceType;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> resourceIds;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String verificationCode;

    public static ChallengesModel.ChallengesModelBuilder issueSuppliersBatchChallenge(final List<String> batchIds) {
        return ChallengesModel.builder()
                .resourceType("suppliers_batch")
                .resourceIds(batchIds);
    }

    public static ChallengesModel.ChallengesModelBuilder verifySuppliersBatchChallenge(final String verificationCode) {
        return ChallengesModel.builder()
                .resourceType("suppliers_batch")
                .verificationCode(verificationCode);
    }

    public static ChallengesModel.ChallengesModelBuilder issuePaymentRunsChallenge(final List<String> paymentRunsIds) {
        return ChallengesModel.builder()
                .resourceType("payment_runs")
                .resourceIds(paymentRunsIds);
    }

    public static ChallengesModel.ChallengesModelBuilder verifyPaymentRunsChallenge(final String verificationCode) {
        return ChallengesModel.builder()
                .resourceType("payment_runs")
                .verificationCode(verificationCode);
    }

}
