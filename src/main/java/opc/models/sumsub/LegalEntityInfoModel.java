package opc.models.sumsub;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LegalEntityInfoModel {

    final LegalEntityDirectorInfoModel companyInfo;

    public static LegalEntityInfoModel defaultLegalEntityInfoModel(){
        return LegalEntityInfoModel.builder()
            .companyInfo(LegalEntityDirectorInfoModel.defaultCompanyInfoModel())
            .build();
    }
}
