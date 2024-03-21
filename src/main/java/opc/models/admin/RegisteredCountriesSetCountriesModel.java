
package opc.models.admin;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisteredCountriesSetCountriesModel {

    private RegisteredCountriesContextModel context;
    private RegisteredCountriesSetModel set;




}
