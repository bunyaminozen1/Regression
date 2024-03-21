package opc.junit.innovator.reports;

import opc.models.innovator.GetReportsModel;
import opc.models.shared.PagingModel;
import opc.services.innovator.InnovatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;

public class GetReportsTests extends BaseReportsSetup {

    @Test
    public void GetReports_Success(){
        final GetReportsModel getReportsModel =
                GetReportsModel.builder()
                        .setPaging(new PagingModel(0, 10))
                        .build();

        InnovatorService.getReports(getReportsModel, innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("report[0].id", equalTo(TOP_REPORT_ID))
                .body("report[0].title", equalTo("Managed Accounts Outgoing Transfers"))
                .body("report[0].description", equalTo("Insights on managed account outgoing transfers, by currency"))
                .body("report[0].category", equalTo("MANAGED_ACCOUNTS"))
                .body("report[1].id", equalTo("70"))
                .body("report[1].title", equalTo("Cards Overview"))
                .body("report[1].description", equalTo("Insights on creation and status of managed cards"))
                .body("report[1].category", equalTo("MANAGED_CARDS"))
                .body("count", equalTo(14))
                .body("responseCount", equalTo(10));
    }

    @Test
    public void GetReports_FilterByCategory_Success(){
        final GetReportsModel getReportsModel =
                GetReportsModel.builder()
                        .setPaging(new PagingModel(0, 100))
                        .setCategory("TECHNICAL")
                        .build();

        InnovatorService.getReports(getReportsModel, innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("report[0].id", equalTo("10"))
                .body("report[0].title", equalTo("API Calls"))
                .body("report[0].description", equalTo("Insights on API Requests"))
                .body("report[0].category", equalTo("TECHNICAL"))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetReports_LimitCheck_Success(){
        final GetReportsModel getReportsModel =
                GetReportsModel.builder()
                        .setPaging(new PagingModel(0, 1))
                        .build();

        InnovatorService.getReports(getReportsModel, innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("report[0].id", equalTo(TOP_REPORT_ID))
                .body("report[0].title", equalTo("Managed Accounts Outgoing Transfers"))
                .body("report[0].description", equalTo("Insights on managed account outgoing transfers, by currency"))
                .body("report[0].category", equalTo("MANAGED_ACCOUNTS"))
                .body("count", equalTo(14))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetReports_InvalidToken_Unauthorised(){
        final GetReportsModel getReportsModel = GetReportsModel.defaultGetReportsModel();

        InnovatorService.getReports(getReportsModel, RandomStringUtils.randomAlphanumeric(10))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetReports_InvalidPagingOffset_BadRequest(){
        final GetReportsModel getReportsModel =
                GetReportsModel.builder()
                        .setPaging(new PagingModel(-1, 100))
                        .setCategory("TECHNICAL")
                        .build();

        InnovatorService.getReports(getReportsModel, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validation.fields[0].name", equalTo("paging.offset"));
    }

    @Test
    public void GetReports_InvalidPagingLimit_BadRequest(){
        final GetReportsModel getReportsModel =
                GetReportsModel.builder()
                .setPaging(new PagingModel(0, -1))
                .setCategory("TECHNICAL")
                .build();

        InnovatorService.getReports(getReportsModel, innovatorToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validation.fields[0].name", equalTo("paging.limit"));
    }

    @Test
    public void GetReports_NoReportForCategory_NoReports(){
        final GetReportsModel getReportsModel =
                GetReportsModel.builder()
                        .setPaging(new PagingModel(0, 100))
                        .setCategory("REVENUE")
                        .build();

        InnovatorService.getReports(getReportsModel, innovatorToken)
                .then()
                .statusCode(SC_OK)
                .body("report[0]", nullValue())
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }
}
