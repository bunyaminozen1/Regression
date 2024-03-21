package opc.models.multi.beneficiaries;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ManagedInstrumentType;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class CreateBeneficiariesBatchModel {
  @JsonProperty("tag")
  private final String tag;
  @JsonProperty("beneficiaries")
  private final List<BeneficiaryModel> beneficiaries;

  public CreateBeneficiariesBatchModel(final CreateBeneficiariesBatchModel.Builder builder) {
    this.beneficiaries = builder.beneficiaries;
    this.tag = builder.tag;
  }

  public String getTag() {
    return tag;
  }
  public List<BeneficiaryModel> getBeneficiaries()  { return beneficiaries; }

  public static class Builder {
    private String tag;
    private List<BeneficiaryModel> beneficiaries;

    public CreateBeneficiariesBatchModel.Builder setTag(String tag) {
      this.tag = tag;
      return this;
    }

    public CreateBeneficiariesBatchModel.Builder setBeneficiaries(List<BeneficiaryModel> beneficiaries) {
      this.beneficiaries = beneficiaries;
      return this;
    }

    public CreateBeneficiariesBatchModel build() { return new CreateBeneficiariesBatchModel(this); }
  }

  public static CreateBeneficiariesBatchModel.Builder builder() {
    return new CreateBeneficiariesBatchModel.Builder();
  }

  public static CreateBeneficiariesBatchModel.Builder InstrumentsBeneficiaryBatch(final IdentityType identityType,
                                                                                  final ManagedInstrumentType managedInstrumentType,
                                                                                  final String name,
                                                                                  final List<String> managedInstruments) {
    List<BeneficiaryModel> listOfBeneficiaries =
         managedInstruments.stream().map(
                managedInstrument -> BeneficiaryModel
                    .InstrumentBeneficiary(identityType, managedInstrumentType, name, managedInstrument)
                    .build()).
            collect(Collectors.toList());

    return new Builder()
        .setTag(RandomStringUtils.randomAlphabetic(5))
        .setBeneficiaries(listOfBeneficiaries);
  }

  public static CreateBeneficiariesBatchModel.Builder SEPABeneficiaryBatch(final IdentityType identityType,
                                                                           final String name,
                                                                           final List<Pair<String, String>> ibanAndBankIdentifierCodes) {
    List<BeneficiaryModel> listOfBeneficiaries =
        ibanAndBankIdentifierCodes.stream().map(
                ibanAndBankIdentifierCode -> BeneficiaryModel
                    .SEPABankAccountBeneficiary(identityType, name, ibanAndBankIdentifierCode.getLeft(), ibanAndBankIdentifierCode.getRight())
                    .build()).
            collect(Collectors.toList());

    return new Builder()
        .setTag(RandomStringUtils.randomAlphabetic(5))
        .setBeneficiaries(listOfBeneficiaries);
  }

  public static CreateBeneficiariesBatchModel.Builder FasterPaymentsBeneficiaryBatch(final IdentityType identityType,
                                                                                     final String name,
                                                                                     final List<Pair<String, String>> accountNumbersAndSortCodes) {
    List<BeneficiaryModel> listOfBeneficiaries =
        accountNumbersAndSortCodes.stream().map(
                accountNumberAndSortCode -> BeneficiaryModel
                    .FasterPaymentsBankAccountBeneficiary(identityType, name, accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight())
                    .build()).
            collect(Collectors.toList());

    return new Builder()
        .setTag(RandomStringUtils.randomAlphabetic(5))
        .setBeneficiaries(listOfBeneficiaries);
  }

  public static CreateBeneficiariesBatchModel.Builder MultipleBeneficiaryBatch(final IdentityType identityType,
                                                                               final ManagedInstrumentType managedInstrumentType,
                                                                               final String name,
                                                                               final List<String> managedInstruments,
                                                                               final List<Pair<String, String>> ibanAndBankIdentifierCodes,
                                                                               final List<Pair<String, String>> accountNumbersAndSortCodes) {

    List<BeneficiaryModel> listOfAllBeneficiaries = new ArrayList<>();

    listOfAllBeneficiaries.addAll(
        managedInstruments.stream()
            .map(managedInstrument -> BeneficiaryModel.InstrumentBeneficiary(identityType, managedInstrumentType, name, managedInstrument).build())
            .collect(Collectors.toList()));

    listOfAllBeneficiaries.addAll(
        ibanAndBankIdentifierCodes.stream()
            .map(ibanAndBankIdentifierCode -> BeneficiaryModel.SEPABankAccountBeneficiary(identityType, name, ibanAndBankIdentifierCode.getLeft(), ibanAndBankIdentifierCode.getRight()).build())
            .collect(Collectors.toList()));

    listOfAllBeneficiaries.addAll(
        accountNumbersAndSortCodes.stream()
            .map(accountNumberAndSortCode -> BeneficiaryModel.FasterPaymentsBankAccountBeneficiary(identityType, name, accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build())
            .collect(Collectors.toList()));

    return new Builder()
        .setTag(RandomStringUtils.randomAlphabetic(5))
        .setBeneficiaries(listOfAllBeneficiaries);
  }
}