package fpi.paymentrun.models.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class ApplyProfilesModel {
    private String programmeId;
    private String corporateProfileId;
    private String managedAccountProfileId;
    private String owtProfileId;
    private String withdrawProfileId;
    private String linkedAccountProfileId;
    private String openBankingRedirectUrl;
    private String companyRegistrationName;
}
