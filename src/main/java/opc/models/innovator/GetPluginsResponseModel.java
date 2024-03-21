package opc.models.innovator;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GetPluginsResponseModel {
    private List<GetPluginResponseModel> plugins;
}
