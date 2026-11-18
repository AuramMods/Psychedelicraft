package ivorius.psychedelicraft.client.render.shader;

import ivorius.psychedelicraft.client.render.RenderPhase;
import org.junit.Test;
import static org.junit.Assert.*;

public class GeometryMaterialTest {
    @Test public void nativeArmorGlintEyesAndLabelsUseModelDistance(){
        for(String name:new String[]{"rendertype_entity_solid","rendertype_armor_cutout_no_cull","rendertype_armor_glint","rendertype_glint","rendertype_glint_direct","rendertype_glint_translucent","rendertype_eyes","rendertype_energy_swirl","rendertype_leash","rendertype_text","rendertype_text_background","rendertype_water_mask"})assertEquals(name,2,GeometryMaterial.pulseShape(RenderPhase.WORLD,name),0);
    }
    @Test public void seeThroughTextMovingGlassBeamsAndPortalLayersRetainUniformPulses(){
        for(String name:new String[]{"rendertype_text_see_through","rendertype_text_background_see_through","rendertype_translucent_moving_block","rendertype_beacon_beam","rendertype_end_portal","rendertype_end_gateway","rendertype_crumbling"})assertEquals(name,0,GeometryMaterial.pulseShape(RenderPhase.WORLD,name),0);
        assertEquals(3,GeometryMaterial.pulseShape(RenderPhase.WORLD,"particle"),0);assertEquals(3,GeometryMaterial.pulseShape(RenderPhase.WORLD,"rendertype_translucent"),0);assertEquals(1,GeometryMaterial.pulseShape(RenderPhase.WORLD,"psychedelicraft:rendertype_zero_matter"),0);
    }
    @Test public void nativeGlowMasksKeepTheirTeamColorAndSkipPulseColor(){
        assertFalse(GeometryMaterial.surface(RenderPhase.WORLD,"rendertype_outline"));assertFalse(GeometryMaterial.pulses("rendertype_outline"));assertTrue(GeometryMaterial.surface(RenderPhase.WORLD,"rendertype_lines"));assertTrue(GeometryMaterial.pulses("rendertype_entity_solid"));
    }
    @Test public void modelScopesCoverMovingSolidBlocksAndKeepSpecialMaterialRules(){
        assertEquals(2,GeometryMaterial.pulseShape(RenderPhase.MODEL,"rendertype_solid"),0);assertEquals(0,GeometryMaterial.pulseShape(RenderPhase.MODEL,"rendertype_translucent_moving_block"),0);assertEquals(0,GeometryMaterial.pulseShape(RenderPhase.MODEL,"rendertype_beacon_beam"),0);assertEquals(1,GeometryMaterial.pulseShape(RenderPhase.MODEL,"psychedelicraft:rendertype_zero_matter"),0);
        RenderPhase.reset();try(var world=RenderPhase.WORLD.enter();var model=RenderPhase.model()){assertEquals(RenderPhase.MODEL,RenderPhase.current());assertTrue(RenderPhase.current().deforms());}assertEquals(RenderPhase.NORMAL,RenderPhase.current());try(var gui=RenderPhase.model()){assertEquals(RenderPhase.NORMAL,RenderPhase.current());}
    }
    @Test public void skyStarsPostAndHandsKeepTheirAcceptedPhasePolicies(){
        assertFalse(GeometryMaterial.surface(RenderPhase.SKY,"position_tex"));assertFalse(GeometryMaterial.surface(RenderPhase.STARS,"position"));assertTrue(GeometryMaterial.surface(RenderPhase.SKY,"position"));assertTrue(GeometryMaterial.surface(RenderPhase.END_SKY,"position_tex"));assertFalse(GeometryMaterial.surface(RenderPhase.POST,"position_tex_color"));assertEquals(0,GeometryMaterial.pulseShape(RenderPhase.SKY,"position_tex"),0);assertEquals(2,GeometryMaterial.pulseShape(RenderPhase.STARS,"position"),0);assertEquals(2,GeometryMaterial.pulseShape(RenderPhase.HAND,"position_tex"),0);
    }
    @Test public void immersionAndThickFogUseSphericalBandsWithoutChangingUniformMaterials(){
        assertEquals(1,GeometryMaterial.pulseShape(RenderPhase.WORLD,"rendertype_solid",true),0);
        assertEquals(1,GeometryMaterial.pulseShape(RenderPhase.WORLD,"particle",true),0);
        assertEquals(1,GeometryMaterial.pulseShape(RenderPhase.MODEL,"rendertype_entity_solid",true),0);
        assertEquals(3,GeometryMaterial.pulseShape(RenderPhase.WORLD,"rendertype_solid",false),0);
        assertEquals(2,GeometryMaterial.pulseShape(RenderPhase.MODEL,"rendertype_entity_solid",false),0);
        for(boolean fog:new boolean[]{false,true}){
            assertEquals(0,GeometryMaterial.pulseShape(RenderPhase.WORLD,"rendertype_end_portal",fog),0);
            assertEquals(0,GeometryMaterial.pulseShape(RenderPhase.BORDER,"position_tex",fog),0);
            assertEquals(1,GeometryMaterial.pulseShape(RenderPhase.WORLD,"psychedelicraft:rendertype_zero_matter",fog),0);
        }
    }
}
