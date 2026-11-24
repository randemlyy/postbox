package me.randemlyy.postbox;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class MailboxBlockEntity extends BlockEntity {

    private UUID ownerId;
    private String ownerName = "";

    public MailboxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAILBOX, pos, state);
    }

    public UUID ownerId() {
        return ownerId;
    }

    public String ownerName() {
        return ownerName;
    }

    public void setOwner(UUID id, String name) {
        this.ownerId = id;
        this.ownerName = name;
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (ownerId != null) {
            out.store("owner", UUIDUtil.CODEC, ownerId);
        }
        out.putString("owner_name", ownerName);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        this.ownerId = in.read("owner", UUIDUtil.CODEC).orElse(null);
        this.ownerName = in.getStringOr("owner_name", "");
    }
}