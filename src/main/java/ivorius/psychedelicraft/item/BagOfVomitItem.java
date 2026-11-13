package ivorius.psychedelicraft.item;

import ivorius.psychedelicraft.Psychedelicraft;
import ivorius.psychedelicraft.entity.drug.DrugCapability;
import ivorius.psychedelicraft.entity.drug.DrugProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public final class BagOfVomitItem extends ItemFood {
    private final Item[] forms = {Items.COOKIE, Items.MUSHROOM_STEW, Items.GOLDEN_APPLE, Items.COOKED_BEEF, Items.COOKED_CHICKEN};
    private int form;

    public BagOfVomitItem() {
        super(8, 0.8F, true);
        setAlwaysEdible();
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase user) {
        if (user instanceof EntityPlayer && ((EntityPlayer) user).isCreative()) {
            super.onItemUseFinish(stack.copy(), world, user);
            return stack;
        }
        return super.onItemUseFinish(stack, world, user);
    }

    @SideOnly(Side.CLIENT)
    public void registerDisguise() {
        addPropertyOverride(Psychedelicraft.id("disguise"), (stack, world, entity) -> disguise());
    }

    @SideOnly(Side.CLIENT)
    private int disguise() {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        DrugProperties properties = player == null ? null : DrugCapability.get(player);
        if (properties == null || !properties.isTripping()) {
            form = 0;
        } else if (form == 0) {
            form = 1 + player.getRNG().nextInt(forms.length);
        }
        return form;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public String getItemStackDisplayName(ItemStack stack) {
        int disguise = disguise();
        return disguise == 0 ? super.getItemStackDisplayName(stack)
                : new ItemStack(forms[disguise - 1]).getDisplayName();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        int disguise = disguise();
        if (disguise != 0) {
            Item item = forms[disguise - 1];
            item.addInformation(new ItemStack(item), world, tooltip, flag);
        }
    }
}
