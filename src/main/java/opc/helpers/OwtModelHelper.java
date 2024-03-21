package opc.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import opc.enums.opc.OwtType;
import opc.models.multi.outgoingwiretransfers.BulkOutgoingWireTransfersModel;
import opc.models.multi.outgoingwiretransfers.OutgoingWireTransfersModel;

public class OwtModelHelper {

  public static BulkOutgoingWireTransfersModel createOwtBulkPayments(final int numberOfOWTs,
                                                                     final String profileId,
                                                                     final String managedAccountId,
                                                                     final String currency,
                                                                     final Long amount,
                                                                     final OwtType owtType) {

    final List<OutgoingWireTransfersModel> outgoingWireTransfersModels = new ArrayList<>();
    IntStream.range(0, numberOfOWTs).forEach(owt ->
        outgoingWireTransfersModels.add(OutgoingWireTransfersModel
            .DefaultOutgoingWireTransfersModel(profileId, managedAccountId, currency, amount, owtType).build()
        ));

    return BulkOutgoingWireTransfersModel.builder().outgoingWireTransfers(outgoingWireTransfersModels).build();
  }

  public static BulkOutgoingWireTransfersModel createOwtBulkPayments(final String profileId,
                                                                     final List<String> managedAccountIds,
                                                                     final String currency,
                                                                     final Long amount,
                                                                     final OwtType owtType) {

    List<OutgoingWireTransfersModel> outgoingWireTransfersModels = managedAccountIds.stream()
        .map(managedAccountId -> OutgoingWireTransfersModel
            .DefaultOutgoingWireTransfersModel(profileId, managedAccountId, currency,
                amount, owtType).build()).collect(Collectors.toList());

    return BulkOutgoingWireTransfersModel.builder().outgoingWireTransfers(outgoingWireTransfersModels).build();
  }

  public static BulkOutgoingWireTransfersModel createOwtBulkPayments(final int numberOfOWTs,
                                                                     final OutgoingWireTransfersModel outgoingWireTransfersModel) {

    List<OutgoingWireTransfersModel> outgoingWireTransfersModels = new ArrayList<>();
    IntStream.range(0, numberOfOWTs).forEach(owt -> outgoingWireTransfersModels.add(outgoingWireTransfersModel));

    return BulkOutgoingWireTransfersModel.builder().outgoingWireTransfers(outgoingWireTransfersModels).build();
  }

  public static OutgoingWireTransfersModel createOwtScheduledPayment(final String scheduledTimestamp,
                                                                     final String profileId,
                                                                     final String managedAccountId,
                                                                     final String currency,
                                                                     final Long amount,
                                                                     final OwtType owtType) {

      return OutgoingWireTransfersModel
            .DefaultOutgoingWireTransfersModel(profileId, managedAccountId, currency, amount, owtType)
            .setScheduledTimestamp(scheduledTimestamp).build();
  }

  public static BulkOutgoingWireTransfersModel createOwtBulkScheduledPayments(final int numberOfOWTs,
                                                                              final String scheduledTimestamp,
                                                                              final String profileId,
                                                                              final String managedAccountId,
                                                                              final String currency,
                                                                              final Long amount,
                                                                              final OwtType owtType) {

    List<OutgoingWireTransfersModel> outgoingWireTransfersModels = new ArrayList<>();
    IntStream.range(0, numberOfOWTs).forEach(owt ->
        outgoingWireTransfersModels.add(OutgoingWireTransfersModel
            .DefaultOutgoingWireTransfersModel(profileId, managedAccountId, currency, amount, owtType)
            .setScheduledTimestamp(scheduledTimestamp).build()
        ));

    return BulkOutgoingWireTransfersModel.builder().outgoingWireTransfers(outgoingWireTransfersModels).build();
  }

}
