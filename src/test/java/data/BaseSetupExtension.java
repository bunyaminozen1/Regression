package data;

import com.fasterxml.jackson.databind.ObjectMapper;
import opc.enums.opc.InnovatorSetup;
import opc.models.shared.ProgrammeDetailsModel;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

public class BaseSetupExtension implements BeforeAllCallback {

    public ExtensionContext.Store store;
    private static ProgrammeDetailsModel dataApplication;
    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {

        setUpInnovator();

        context.getStore(GLOBAL).put(InnovatorSetup.DATA_APPLICATION, dataApplication);

        this.store = context.getStore(GLOBAL);
    }

    private void setUpInnovator() throws IOException {

        final String innovatorId;

        if (StringUtils.isNotBlank(System.getProperty("sandbox.innovator"))) {
            innovatorId = System.getProperty("sandbox.innovator");
        } else {
            innovatorId = "1991";
        }

        final List<ProgrammeDetailsModel> applications = Arrays.asList(new ObjectMapper()
                .readValue(new File("./src/test/resources/TestConfiguration/sandbox_data_configuration.json"),
                        ProgrammeDetailsModel[].class));

        dataApplication =
                applications.stream().filter(x -> x.getInnovatorId()
                        .equals(innovatorId)).collect(Collectors.toList()).get(0);
    }
}