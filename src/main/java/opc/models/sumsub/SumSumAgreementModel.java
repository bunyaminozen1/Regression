package opc.models.sumsub;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
public class SumSumAgreementModel {

    private final String source;
    private final List<String> targets;

    public static SumSumAgreementModel defaultModel() {
        return SumSumAgreementModel.builder()
                .source("websdk")
                .targets(List.of("constConsentEn_v7"))
                .build();
    }
}