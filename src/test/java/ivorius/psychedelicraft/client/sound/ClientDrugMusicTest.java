package ivorius.psychedelicraft.client.sound;
import ivorius.psychedelicraft.entity.drug.*;
import net.minecraft.nbt.CompoundTag;
import org.junit.Test;
import static org.junit.Assert.*;
public class ClientDrugMusicTest {
    private static void active(DrugProperties properties,DrugType type,double value){var root=properties.toNbt();var drug=new CompoundTag();drug.putDouble("effect",value);drug.putDouble("effectActive",value);root.getCompound("Drugs").put(type.id(),drug);properties.fromNbt(root);}
    @Test public void strongestEnabledThemeUsesActiveValuesAndStableRegistryTieOrder(){
        var drugs=new DrugProperties();assertNull(ClientDrugMusic.strongestTheme(drugs,type->true));
        active(drugs,DrugType.CANNABIS,.2);active(drugs,DrugType.ALCOHOL,.4);assertSame(DrugType.ALCOHOL,ClientDrugMusic.strongestTheme(drugs,type->true));
        assertSame(DrugType.CANNABIS,ClientDrugMusic.strongestTheme(drugs,type->type!=DrugType.ALCOHOL));assertNull(ClientDrugMusic.strongestTheme(drugs,type->false));
        active(drugs,DrugType.CANNABIS,.4);var first=DrugType.values().stream().filter(type->type==DrugType.ALCOHOL||type==DrugType.CANNABIS).findFirst().orElseThrow();assertSame(first,ClientDrugMusic.strongestTheme(drugs,type->true));
    }
    @Test public void belowThresholdAndClearedProfilesCannotKeepAThemeAlive(){
        var drugs=new DrugProperties();active(drugs,DrugType.CAFFEINE,.009);assertNull(ClientDrugMusic.strongestTheme(drugs,type->true));active(drugs,DrugType.CAFFEINE,.01);assertNull(ClientDrugMusic.strongestTheme(drugs,type->true));active(drugs,DrugType.CAFFEINE,.011);assertSame(DrugType.CAFFEINE,ClientDrugMusic.strongestTheme(drugs,type->true));drugs.fromNbt(new CompoundTag());assertNull(ClientDrugMusic.strongestTheme(drugs,type->true));
        assertEquals(0,ClientDrugMusic.themeVolume(.01),0);assertEquals(.5F,ClientDrugMusic.themeVolume(.2),0);assertEquals(1,ClientDrugMusic.themeVolume(.4),0);assertEquals(1,ClientDrugMusic.themeVolume(2),0);
    }
}
