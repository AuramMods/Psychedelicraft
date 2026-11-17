package ivorius.psychedelicraft.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Accepted visual settings; stored in Forge's client config rather than on the server. */
public final class PSClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SHADERS_2D,MOTION_BLUR,HEAT_DISTORTION,WATER_DISTORTION,WATER_OVERLAY;
    public static final ForgeConfigSpec.DoubleValue PAUSE_BLUR,FOCUS_NEAR,FOCUS_FAR,BLUR_NEAR,BLUR_FAR,DIGITAL_X,DIGITAL_Y;
    static {
        var b=new ForgeConfigSpec.Builder();b.push("visual");
        SHADERS_2D=b.define("shader2DEnabled",true);MOTION_BLUR=b.define("doMotionBlur",true);
        HEAT_DISTORTION=b.define("doHeatDistortion",true);WATER_DISTORTION=b.define("doWaterDistortion",true);WATER_OVERLAY=b.define("waterOverlayEnabled",true);
        PAUSE_BLUR=b.comment("Blur strength while the local singleplayer world is paused.").defineInRange("pauseMenuBlur",0D,0D,8D);
        FOCUS_NEAR=b.comment("Near focus distance in world blocks.").defineInRange("dofFocalPointNear",.2D,0D,99D);
        FOCUS_FAR=b.comment("Far focus distance in world blocks.").defineInRange("dofFocalPointFar",128D,100D,400D);
        BLUR_NEAR=b.defineInRange("dofFocalBlurNear",0D,0D,8D);BLUR_FAR=b.defineInRange("dofFocalBlurFar",0D,0D,8D);
        DIGITAL_X=b.defineInRange("digitalEffectPixelRescaleX",.05D,.01D,1D);DIGITAL_Y=b.defineInRange("digitalEffectPixelRescaleY",.05D,.01D,1D);
        b.pop();SPEC=b.build();
    }
    private PSClientConfig(){}
}
