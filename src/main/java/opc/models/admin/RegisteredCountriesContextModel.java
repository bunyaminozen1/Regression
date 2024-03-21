
package opc.models.admin;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisteredCountriesContextModel {

    private List<RegisteredCountriesDimension> dimension = null;
}
