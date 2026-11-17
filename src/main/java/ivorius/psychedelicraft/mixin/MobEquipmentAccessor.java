package ivorius.psychedelicraft.mixin;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mob.class)
public interface MobEquipmentAccessor {
    @Invoker("getEquipmentDropChance") float psychedelicraft$getEquipmentDropChance(EquipmentSlot slot);
}
