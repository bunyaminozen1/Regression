package opc.models.multiprivate;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import opc.enums.opc.CountryCode;
import opc.helpers.ModelHelper;
import opc.models.backoffice.IdentityModel;
import opc.models.multi.outgoingwiretransfers.FasterPaymentsBankDetailsModel;
import opc.models.multi.outgoingwiretransfers.SepaBankDetailsModel;
import org.apache.commons.lang3.RandomStringUtils;

@Builder
@Getter
@Setter
public class RegisterLinkedAccountModel {

    private IdentityModel identity;
    private String programmeId;
    private String profileId;
    private String friendlyName;
    private String currency;
    private String tag;
    private String country;
    private Object accountDetails;

    public static RegisterLinkedAccountModel.RegisterLinkedAccountModelBuilder DefaultRegisterLinkedAccountFasterModel(final IdentityModel identityModel,
                                                                                                                       final String programmeId,
                                                                                                                       final String profileId,
                                                                                                                       final String currency) {
        return commonRegisterLinkedAccountModel(identityModel, programmeId, profileId, currency)
                .accountDetails(new FasterPaymentsBankDetailsModel(ModelHelper.generateRandomValidAccountNumber(), ModelHelper.generateRandomValidSortCode()));
    }

    public static RegisterLinkedAccountModel.RegisterLinkedAccountModelBuilder DefaultRegisterLinkedAccountSepaModel(final IdentityModel identityModel,
                                                                                                                     final String programmeId,
                                                                                                                     final String profileId,
                                                                                                                     final String currency) {
        return commonRegisterLinkedAccountModel(identityModel, programmeId, profileId, currency)
                .accountDetails(new SepaBankDetailsModel(ModelHelper.generateRandomValidIban(), ModelHelper.generateRandomValidBankIdentifierNumber().toUpperCase()));
        //to confirm why uppercase BIC is required - this will probably change in the near future
    }

    public static RegisterLinkedAccountModelBuilder commonRegisterLinkedAccountModel (final IdentityModel identityModel,
                                                                                      final String programmeId,
                                                                                      final String profileId,
                                                                                      final String currency) {
        return RegisterLinkedAccountModel
                .builder()
                .identity(identityModel)
                .programmeId(programmeId)
                .profileId(profileId)
                .friendlyName(RandomStringUtils.randomAlphabetic(10))
                .tag(RandomStringUtils.randomAlphabetic(10))
                .currency(currency)
                .country(CountryCode.GB.toString());
    }
}
