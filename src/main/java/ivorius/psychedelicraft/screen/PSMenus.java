package ivorius.psychedelicraft.screen;

import ivorius.psychedelicraft.Psychedelicraft;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class PSMenus {
    public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,Psychedelicraft.MOD_ID);
    public static final RegistryObject<MenuType<DryingTableMenu>> DRYING_TABLE=MENUS.register("drying_table",()->IForgeMenuType.create(DryingTableMenu::new));
    private PSMenus(){}
}
