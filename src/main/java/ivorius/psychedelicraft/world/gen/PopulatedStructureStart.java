package ivorius.psychedelicraft.world.gen;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.common.util.Constants;

import java.util.HashSet;
import java.util.Set;

public abstract class PopulatedStructureStart extends StructureStart {
    private final Set<ChunkPos> populated = new HashSet<>();

    protected PopulatedStructureStart() {
    }

    protected PopulatedStructureStart(int x, int z) {
        super(x, z);
    }

    @Override
    public boolean isValidForPostProcess(ChunkPos pos) {
        return !populated.contains(pos);
    }

    @Override
    public void notifyPostProcessAt(ChunkPos pos) {
        populated.add(pos);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        NBTTagList chunks = new NBTTagList();
        for (ChunkPos pos : populated) {
            NBTTagCompound chunk = new NBTTagCompound();
            chunk.setInteger("X", pos.x);
            chunk.setInteger("Z", pos.z);
            chunks.appendTag(chunk);
        }
        tag.setTag("Populated", chunks);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        populated.clear();
        NBTTagList chunks = tag.getTagList("Populated", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < chunks.tagCount(); i++) {
            NBTTagCompound chunk = chunks.getCompoundTagAt(i);
            populated.add(new ChunkPos(chunk.getInteger("X"), chunk.getInteger("Z")));
        }
    }
}
