package opc.models.admin;

import opc.enums.opc.BlockType;

public class BlockTypeModel {

    private String blockType;

    public BlockTypeModel(BlockType blockType) {
        this.blockType = blockType.name();
    }

    public String getBlockType() {
        return blockType;
    }

    public BlockTypeModel setBlockType(String blockType) {
        this.blockType = blockType;
        return this;
    }
}
