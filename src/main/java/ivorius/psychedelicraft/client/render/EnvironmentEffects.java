/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.client.render;

import ivorius.psychedelicraft.config.PSClientConfig;
import ivorius.psychedelicraft.fluid.PSFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

public final class EnvironmentEffects {
    private float experiencedHealth=5;
    private float heat;
    private boolean underwater;
    private int wetTicks;
    public void update(Player player){
        var level=player.level();var pos=player.blockPosition();var eyes=BlockPos.containing(player.getEyePosition());
        boolean water=player.isEyeInFluid(FluidTags.WATER);
        for(var family:PSFluids.all())if(family.waterLike()&&player.getEyeInFluidType()==family.get().getFluidType()){water=true;break;}
        var biome=level.getBiome(pos).value();boolean rain=level.getRainLevel(1)>0&&biome.getPrecipitationAt(pos)==Biome.Precipitation.RAIN&&level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING,pos).getY()<=player.getY();
        float target=biome.getBaseTemperature();if(!level.dimensionType().hasCeiling())target*=sunIntensity(level.getTimeOfDay(1),level.isRaining(),level.isThundering())*level.getBrightness(LightLayer.SKY,eyes)/15F;
        update(player.getHealth(),target,water,rain);
    }
    public void update(float target,boolean water,boolean rain){update(20,target,water,rain);}
    public void update(float health,float target,boolean water,boolean rain){underwater=water;heat=smooth(heat,water?0:target);if(PSClientConfig.HURT_OVERLAY.get())experiencedHealth=smooth(experiencedHealth,health);if(PSClientConfig.WATER_OVERLAY.get())wetTicks=Mth.clamp(wetTicks-1+(water?20:0)+(rain?4:0),0,100);}
    public float hurtOverlay(int hurtTime,int maxHurtTime,float heartbeat){
        if(!PSClientConfig.HURT_OVERLAY.get())return 0;
        float flash=Math.max(hurtTime>0&&maxHurtTime>0?(float)hurtTime/maxHurtTime:0,heartbeat);
        return flash>0?flash:Math.max(0,(5-experiencedHealth)/6);
    }
    public float heatDistortion(){return underwater||!PSClientConfig.HEAT_DISTORTION.get()?0:Mth.clamp((heat-1)*.0015F,0,.01F);}
    public float waterDistortion(float peyote) {
        float water = underwater && PSClientConfig.WATER_DISTORTION.get() ? .025F : 0;
        float peyoteDistortion = PSClientConfig.PEYOTE_DISTORTION.get() ? peyote * .01073F : 0;
        return Math.max(water, peyoteDistortion);
    }
    public float wetOverlay(){return underwater||!PSClientConfig.WATER_OVERLAY.get()?0:Math.min(1,wetTicks/80F);}
    private static float smooth(float value,float target){float next=value+(target-value)*.01F;return next+Mth.clamp(target-next,-.01F,.01F);}
    private static float sunIntensity(float time,boolean rain,boolean thunder){float angle=((time+.25F)%1)*2;if(angle>1)return 0;float sun=Mth.cos((angle-.5F)*Mth.PI);return sun*(rain?.5F:1)*(thunder?.5F:1);}
}
