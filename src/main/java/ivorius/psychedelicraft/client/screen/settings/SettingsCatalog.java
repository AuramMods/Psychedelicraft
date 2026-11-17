/* Copyright (c) 2014, Lukas Tenbrink. */
package ivorius.psychedelicraft.client.screen.settings;

import ivorius.psychedelicraft.config.*;
import com.electronwill.nightconfig.core.Config;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.common.ForgeConfigSpec;
import java.util.*;

/** Native Forge bindings for the accepted settings tree and original modern controls. */
public final class SettingsCatalog {
    private static final String OPTIONS="gui.psychedelicraft.options.",OPTION="gui.psychedelicraft.option.";
    public interface Entry {String titleKey();}
    public record Page(String id,String titleKey,List<Entry> entries) implements Entry {
        public Page {entries=List.copyOf(entries);}
        public List<Setting> settings(){var result=new ArrayList<Setting>();for(var e:entries)if(e instanceof Setting s)result.add(s);else result.addAll(((Page)e).settings());return List.copyOf(result);}
        public boolean worldSettings(){return settings().stream().anyMatch(Setting::worldSetting);}
        public boolean needsRestart(){return settings().stream().anyMatch(Setting::needsRestart);}
    }
    private final Runnable refreshCreative;
    private final Page root;
    public SettingsCatalog(boolean localWorld,Runnable refreshCreative){
        this.refreshCreative=refreshCreative;
        var visuals=page("visuals",OPTIONS+"visuals",
                page("shaders",OPTIONS+"shaders",client(PSClientConfig.SHADERS_2D,"shaders_2d"),client(PSClientConfig.SHADERS_3D,"shaders_3d"),
                        client(PSClientConfig.HEAT_DISTORTION,"heat_distortion"),client(PSClientConfig.WATER_DISTORTION,"water_distortion"),client(PSClientConfig.MOTION_BLUR,"motion_blur"),client(PSClientConfig.PAUSE_BLUR,"pause_menu_blur")),
                page("overlays",OPTIONS+"overlays",client(PSClientConfig.WATER_OVERLAY,"water_overlay"),client(PSClientConfig.HURT_OVERLAY,"hurt_overlay"),client(PSClientConfig.SUN_FLARE,"sun_glare_intensity")),
                page("dof",OPTIONS+"dof",page("focus_near",OPTION+"focal_point.near",client(PSClientConfig.FOCUS_NEAR,"focal_point.distance"),client(PSClientConfig.BLUR_NEAR,"focal_point.blur")),
                        page("focus_far",OPTION+"focal_point.far",client(PSClientConfig.FOCUS_FAR,"focal_point.distance"),client(PSClientConfig.BLUR_FAR,"focal_point.blur"))),
                page("digital",OPTIONS+"digital",client(PSClientConfig.DIGITAL_X,"pixel_scale_x"),client(PSClientConfig.DIGITAL_Y,"pixel_scale_y")));
        var themes=new ArrayList<Entry>();
        PSClientConfig.THEMES.forEach((type,value)->themes.add(new Setting(PSClientConfig.SPEC,value,"gui.psychedelicraft.theme."+type.id().substring(type.id().indexOf(':')+1),false,false)));
        var sounds=page("sounds",OPTIONS+"sounds",client(PSClientConfig.DRUG_MUSIC,"music_enabled"),new Page("themes",OPTIONS+"themes",themes));
        var chat=page("messages",OPTIONS+"message_distortion",common(PSConfig.DISTORT_INCOMING_MESSAGES,"gameplay.distort_incoming_messages"),common(PSConfig.DISTORT_OUTGOING_MESSAGES,"gameplay.distort_outgoing_messages"));
        var entries=new ArrayList<Entry>(List.of(visuals,sounds,chat));
        if(localWorld){
            var brewing=new ArrayList<Entry>();
            PSConfig.FLUID_TICKS.forEach((name,ticks)->brewing.add(page("brew_"+name,"gui.psychedelicraft.brew."+name,
                    common(ticks.fermentation(),"brew.fermentation"),common(ticks.distillation(),"brew.distillation"),
                    common(ticks.maturation(),"brew.maturation"),common(ticks.acetification(),"brew.acetification"))));
            brewing.add(common(PSConfig.FLOWER_EXTRACT_DISTILLATION_TICKS,"brew.flower_extract"));
            var generation=new ArrayList<Entry>();
            generation.add(page("generation_general",OPTIONS+"generation_general",common(PSConfig.FARMER_DRUG_DEALS,"generation.farmer_trades"),
                    common(PSConfig.VILLAGE_CHESTS,"generation.village_loot"),common(PSConfig.DUNGEON_CHESTS,"generation.dungeon_loot")));
            PSConfig.GENERATION.forEach((name,feature)->generation.add(page("generation_"+name,"gui.psychedelicraft.generation."+name,
                    common(feature.enabled(),"generation.enabled"),common(feature.included(),"generation.included"),common(feature.excluded(),"generation.excluded"))));
            entries.add(page("gameplay",OPTIONS+"gameplay",
                    page("features",OPTIONS+"features",common(PSConfig.ENABLE_HARMONIUM,"gameplay.harmonium"),common(PSConfig.ENABLE_RIFT_JARS,"gameplay.rift_jars"),
                            new Setting(PSConfig.COMMON,PSConfig.DISABLE_MOLOTOVS,OPTION+"gameplay.molotovs",true,false)),
                    page("balancing",OPTIONS+"balancing",new Setting(PSConfig.COMMON,PSConfig.RIFT_SPAWN_INTERVAL,OPTION+"gameplay.rift_spawn_interval",false,true),
                            common(PSConfig.DRYING_TABLE_TICKS,"gameplay.wood_drying_time"),common(PSConfig.IRON_DRYING_TABLE_TICKS,"gameplay.iron_drying_time"),common(PSConfig.SLURRY_HARDENING_TICKS,"gameplay.slurry_time")),
                    new Page("brewing",OPTIONS+"fluid_timings",brewing),new Page("world_generation",OPTIONS+"world_generation",generation)));
        }
        root=new Page("root",OPTIONS+"title",entries);
    }
    public Page root(){return root;}
    private Page page(String id,String key,Entry...entries){return new Page(id,key,List.of(entries));}
    private Setting client(ForgeConfigSpec.ConfigValue<?> value,String key){return new Setting(PSClientConfig.SPEC,value,OPTION+key,false,false);}
    private Setting common(ForgeConfigSpec.ConfigValue<?> value,String key){return new Setting(PSConfig.COMMON,value,OPTION+key,false,false);}
    /** Called once when the settings session closes, including Escape or an external screen replacement. */
    public void save(){
        RuntimeException failure=null;
        for(var spec:List.of(PSClientConfig.SPEC,PSConfig.COMMON))try{spec.save();}catch(RuntimeException error){if(failure==null)failure=error;else failure.addSuppressed(error);}
        if(failure!=null)throw failure;
    }
    public final class Setting implements Entry {
        private final ForgeConfigSpec spec;
        private final ForgeConfigSpec.ConfigValue<?> value;
        private final ForgeConfigSpec.ValueSpec definition;
        private final String key,path;
        private final boolean inverted,minutes;
        Setting(ForgeConfigSpec spec,ForgeConfigSpec.ConfigValue<?> value,String key,boolean inverted,boolean minutes){
            this.spec=spec;this.value=value;this.key=key;this.inverted=inverted;this.minutes=minutes;
            path=String.join(".",value.getPath());definition=spec.getSpec().get(value.getPath());
        }
        @Override public String titleKey(){return key;}
        public String id(){return (spec==PSClientConfig.SPEC?"client:":"common:")+path;}
        public boolean worldSetting(){return spec==PSConfig.COMMON&&!path.startsWith("balancing.messageDistortion.");}
        public boolean needsRestart(){return worldSetting()&&((path.startsWith("worldGeneration.")&&!path.endsWith("villageChests")&&!path.endsWith("dungeonChests"))||path.equals("balancing.enableHarmonium"));}
        public boolean toggle(){return definition.getDefault() instanceof Boolean;}
        public boolean slider(){return definition.getDefault() instanceof Double;}
        public boolean selectors(){return definition.getDefault() instanceof List<?>;}
        public boolean enabled(){boolean flag=(Boolean)value.get();return inverted?!flag:flag;}
        public double number(){return ((Number)value.get()).doubleValue();}
        public double minimum(){return ((Number)definition.getRange().getMin()).doubleValue();}
        public double maximum(){return ((Number)definition.getRange().getMax()).doubleValue();}
        public boolean isDefault(){return Objects.equals(value.get(),definition.getDefault());}
        public boolean zeroIsOff(){return path.contains("Blur")||path.endsWith("sunFlareIntensity");}
        public String text(){
            Object current=value.get();
            if(selectors())return String.join(", ",((List<?>)current).stream().map(Object::toString).toList());
            return minutes?SettingInput.minutes(((Number)current).intValue()):current.toString();
        }
        public String minimumText(){return minutes?SettingInput.minutes((int)minimum()):definition.getRange().getMin().toString();}
        public String maximumText(){return minutes?SettingInput.minutes((int)maximum()):definition.getRange().getMax().toString();}
        public void setEnabled(boolean flag){write(inverted?!flag:flag);}
        public void setNumber(double number){write(number);}
        public boolean setText(String text){
            try{
                Object candidate;
                if(selectors())candidate=SettingInput.selectors(text);
                else if(minutes)candidate=SettingInput.ticks(text);
                else if(definition.getDefault() instanceof Integer)candidate=Integer.valueOf(text.trim());
                else candidate=SettingInput.finiteDouble(text);
                return write(candidate);
            }catch(IllegalArgumentException|ArithmeticException error){return false;}
        }
        public void reset(){write(definition.getDefault());}
        private boolean write(Object candidate){
            if(!definition.test(candidate))return false;
            if(!Objects.equals(candidate,value.get())){
                // Forge's file root autosaves each set; its watcher can reload an older snapshot
                // during rapid edits. Mutate the loaded nested section directly, clear this
                // value's cache for live consumers, then save each file once on tree exit.
                Config section=ConfigTracker.INSTANCE.fileMap().values().stream()
                        .filter(config->config.getSpec()==spec).findFirst().orElseThrow().getConfigData();
                var parts=value.getPath();
                for(String part:parts.subList(0,parts.size()-1))section=section.get(part);
                section.set(List.of(parts.get(parts.size()-1)),candidate instanceof List<?> list?List.copyOf(list):candidate);
                value.clearCache();
                if(path.equals("balancing.enableHarmonium")||path.equals("balancing.enableRiftJars")||path.equals("balancing.disableMolotovs"))refreshCreative.run();
            }
            return true;
        }
    }
}
