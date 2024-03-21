package opc.junit.openbanking.consents;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import opc.models.openbanking.ConsentsResponseModel;
import org.junit.jupiter.api.Test;

public class ConsentsPlayground {

  @Test
  public void ModelTest() throws IOException {
    ObjectMapper om = new ObjectMapper();
    final var consents = om.readValue(new File(
            "/Users/arminzukic/IdeaProjects/opc-e2e/src/test/java/opc/junit/openbanking/consents/schemas/consents.json"),
        ConsentsResponseModel.class);


  }
}
