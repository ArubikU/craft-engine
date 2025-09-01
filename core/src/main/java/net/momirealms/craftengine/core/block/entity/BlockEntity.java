package net.momirealms.craftengine.core.block.entity;

import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.CEWorld;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.SectionPos;
import net.momirealms.sparrow.nbt.CompoundTag;

public abstract class BlockEntity {
    protected final BlockPos pos;
    protected final ImmutableBlockState blockState;
    protected BlockEntityType<? extends BlockEntity> type;
    protected CEWorld world;
    protected boolean valid;

    protected BlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, ImmutableBlockState blockState) {
        this.pos = pos;
        this.blockState = blockState;
        this.type = type;
    }

    public final CompoundTag saveAsTag() {
        CompoundTag tag = new CompoundTag();
        this.savePos(tag);
        this.saveCustomData(tag);
        return tag;
    }

    public CEWorld world() {
        return world;
    }

    public void setWorld(CEWorld world) {
        this.world = world;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    private void savePos(CompoundTag tag) {
        tag.putInt("x", this.pos.x());
        tag.putInt("y", this.pos.y());
        tag.putInt("z", this.pos.z());
    }

    protected void saveCustomData(CompoundTag tag) {
    }

    protected void readCustomData(CompoundTag tag) {
    }

    public static BlockPos readPos(CompoundTag tag) {
        return new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
    }

    public BlockEntityType<? extends BlockEntity> type() {
        return type;
    }

    public static BlockPos readPosAndVerify(CompoundTag tag, ChunkPos chunkPos) {
        int x = tag.getInt("x", 0);
        int y = tag.getInt("y", 0);
        int z = tag.getInt("z", 0);
        int sectionX = SectionPos.blockToSectionCoord(x);
        int sectionZ = SectionPos.blockToSectionCoord(z);
        if (sectionX != chunkPos.x || sectionZ != chunkPos.z) {
            x = chunkPos.x * 16 + SectionPos.sectionRelative(x);
            z = chunkPos.z * 16 + SectionPos.sectionRelative(z);
        }
        return new BlockPos(x, y, z);
    }

    public BlockPos pos() {
        return pos;
    }

    public boolean isValidBlockState(ImmutableBlockState blockState) {
        return blockState.blockEntityType() == this.type;
    }

    public interface Factory<T extends BlockEntity> {

        T create(BlockPos pos, ImmutableBlockState state);
    }
}
