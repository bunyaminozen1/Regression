package opc.models.admin;

import lombok.Builder;
import lombok.Data;
import opc.models.shared.PagingModel;

@Data
@Builder
public class AuthorisationsRequestModel {
    private final PagingModel paging;
    private final String providerLinkId;
    private final String processorExpiryTimestampFrom;
    private final String processorExpiryTimestampTo;
    private final String cardId;

    public static AuthorisationsRequestModel defaultAuthorisationsRequestModel(final PagingModel paging,
                                                                               final String providerLinkId,
                                                                               final String processorExpiryTimestampFrom,
                                                                               final String processorExpiryTimestampTo,
                                                                               final String cardId) {
        return AuthorisationsRequestModel.builder()
                .paging(paging)
                .providerLinkId(providerLinkId)
                .processorExpiryTimestampFrom(processorExpiryTimestampFrom)
                .processorExpiryTimestampTo(processorExpiryTimestampTo)
                .cardId(cardId).build();
    }

    public static AuthorisationsRequestModel defaultAuthorisationsRequestModel(final PagingModel paging) {
        return AuthorisationsRequestModel.builder()
                .paging(paging).build();
    }

    public static AuthorisationsRequestModel defaultAuthorisationsRequestModel(final PagingModel paging,
                                                                               final String providerLinkId) {
        return AuthorisationsRequestModel.builder()
                .paging(paging)
                .providerLinkId(providerLinkId).build();
    }

}
