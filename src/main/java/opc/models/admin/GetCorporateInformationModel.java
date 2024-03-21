package opc.models.admin;

import lombok.Builder;
import lombok.Data;
import opc.models.shared.PagingModel;
@Data
@Builder
public class GetCorporateInformationModel {

  private PagingModel paging;
  private Integer profileId;
  private String companyName;
  private Integer createdFrom;
  private Integer createdTo;
  private String rootName;
  private String rootSurname;
  private String rootEmail;
  private String rootMobileNumber;
  private String companyRegistrationNumber;
  private String companyRegistrationAddress;
  private String companyBusinessAddress;
  private String registrationCountry;
  private String fullCompanyChecksVerified;
  private String active;
  private Integer programmeId;


}
