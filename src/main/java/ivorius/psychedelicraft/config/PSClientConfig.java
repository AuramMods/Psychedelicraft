package ivorius.psychedelicraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import ivorius.psychedelicraft.entity.drug.DrugType;
import java.util.*;

/** Accepted client settings, stored locally through Forge's client config. */
public final class PSClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue DRUG_MUSIC;
    public static final Map<DrugType,ForgeConfigSpec.BooleanValue> THEMES;
    public static final ForgeConfigSpec.BooleanValue SHADERS_2D,SHADERS_3D,MOTION_BLUR,HEAT_DISTORTION,WATER_DISTORTION,WATER_OVERLAY,HURT_OVERLAY;
    public static final ForgeConfigSpec.BooleanValue PEYOTE_DISTORTION;
    public static final ForgeConfigSpec.DoubleValue PAUSE_BLUR,FOCUS_NEAR,FOCUS_FAR,BLUR_NEAR,BLUR_FAR,DIGITAL_X,DIGITAL_Y,SUN_FLARE;
    static {
        var b=new ForgeConfigSpec.Builder();b.push("visual");
        SHADERS_2D=b.define("shader2DEnabled",true);SHADERS_3D=b.define("shader3DEnabled",true);MOTION_BLUR=b.define("doMotionBlur",true);
        HEAT_DISTORTION=b.define("doHeatDistortion",true);WATER_DISTORTION=b.define("doWaterDistortion",true);WATER_OVERLAY=b.define("waterOverlayEnabled",true);
        PEYOTE_DISTORTION=b.comment("Apply the watery screen effect during peyote use.").define("peyoteScreenDistortion",true);
        HURT_OVERLAY=b.define("hurtOverlayEnabled",true);SUN_FLARE=b.defineInRange("sunFlareIntensity",.25D,0D,1D);
        PAUSE_BLUR=b.comment("Blur strength while the local singleplayer world is paused.").defineInRange("pauseMenuBlur",0D,0D,8D);
        FOCUS_NEAR=b.comment("Near focus distance in world blocks.").defineInRange("dofFocalPointNear",.2D,0D,99D);
        FOCUS_FAR=b.comment("Far focus distance in world blocks.").defineInRange("dofFocalPointFar",128D,100D,400D);
        BLUR_NEAR=b.defineInRange("dofFocalBlurNear",0D,0D,8D);BLUR_FAR=b.defineInRange("dofFocalBlurFar",0D,0D,8D);
        DIGITAL_X=b.defineInRange("digitalEffectPixelRescaleX",.05D,.01D,1D);DIGITAL_Y=b.defineInRange("digitalEffectPixelRescaleY",.05D,.01D,1D);
        b.pop();b.push("audio");DRUG_MUSIC=b.define("drugsBackgroundMusic",true);b.push("background_music");
        var themes=new LinkedHashMap<DrugType,ForgeConfigSpec.BooleanValue>();for(var type:DrugType.values())themes.put(type,b.define(new net.minecraft.resources.ResourceLocation(type.id()).getPath(),true));
        THEMES=Collections.unmodifiableMap(themes);b.pop();b.pop();SPEC=b.build();
    }
    private PSClientConfig(){}
}
