package opc.models.secure;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeviceIdModel {
    private final String deviceId;

    public DeviceIdModel(final String deviceId){
        this.deviceId = deviceId;
    }
}
