package opc.models.shared;

import java.util.Map;

public class BlockDetails {

    Map<String, Integer> block_types;

    public BlockDetails() {
    }

    public Map<String, Integer> getBlock_types() {
        return block_types;
    }

    public void setBlock_types(final Map<String, Integer> block_types) {
        this.block_types = block_types;
    }
}
