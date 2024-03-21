package opc.initialisation;

import opc.junit.helpers.innovator.InnovatorHelper;

import java.io.IOException;

public class DataInitialisation {

    public static void main(String[] args) throws IOException {
        InnovatorHelper.createMultiTestsInnovator("Multi", "multi@autotests.io");
        InnovatorHelper.createMultiUkTestsInnovator("MultiUk", "multiuk@autotests.io");
        InnovatorHelper.createMultiNonFpsTestsInnovator("MultiNonFps", "multinonfps@autotests.io");
        InnovatorHelper.createInnovatorTestsInnovator("Portals", "portals@autotests.io");
        InnovatorHelper.createBackofficeTestsInnovator("Backoffice", "backoffice@autotests.io");
        InnovatorHelper.createSemiInnovator("Semi", "semi@autotests.io");
        InnovatorHelper.createFeesInnovator("Fees", "fees@autotests.io");
        InnovatorHelper.createFinInstitutionInnovator("FIS", "fis@autotests.io");
    }

}
