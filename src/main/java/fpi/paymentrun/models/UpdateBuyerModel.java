package fpi.paymentrun.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import commons.enums.Currency;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;

@Builder
@Getter
@Setter
public class UpdateBuyerModel {

    private String tag;
    private UpdateCompanyModel company;
    private AdminUserModel adminUser;
    private String baseCurrency;
    private boolean resetMobileCounter;

    public static UpdateBuyerModel.UpdateBuyerModelBuilder defaultUpdateBuyerModel(){
        return UpdateBuyerModel.builder()
                .tag(RandomStringUtils.randomAlphabetic(5))
                .company(UpdateCompanyModel.defaultCompanyModel().build())
                .baseCurrency(Currency.GBP.name())
                .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser().build())
                .resetMobileCounter(true);
    }

    public static UpdateBuyerModel.UpdateBuyerModelBuilder creatorRoleUpdateBuyerModel(){
        return UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.creatorRoleForUpdateBuyer().build());
    }

    public static UpdateBuyerModel.UpdateBuyerModelBuilder controllerRoleUpdateBuyerModel(){
        return UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.controllerRoleForUpdateBuyer().build());
    }

    public static UpdateBuyerModel.UpdateBuyerModelBuilder allRolesUpdateBuyerModel(){
        return UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.allRolesForUpdateBuyer().build());
    }

    public static UpdateBuyerModel.UpdateBuyerModelBuilder adminRolesUpdateBuyerModel(){
        return UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.adminRoleForUpdateBuyer().build());
    }

    @SneakyThrows
    public static String creatorRoleUpdateBuyerModelString() {
        return new ObjectMapper().writeValueAsString(creatorRoleUpdateBuyerModel().build());
    }

    @SneakyThrows
    public static String defaultUpdateBuyerModelString() {
        return new ObjectMapper().writeValueAsString(defaultUpdateBuyerModel()
                        .adminUser(AdminUserModel.defaultUpdateBuyerAdminUser()
                                .email(null)
                                .build())
                .build());
    }
}
