package ivorius.psychedelicraft.gametest;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.gson.*;
import ivorius.psychedelicraft.client.screen.settings.*;
import ivorius.psychedelicraft.config.*;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.gametest.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@GameTestHolder("psychedelicrafttests") @PrefixGameTestTemplate(false)
public final class SettingsCatalogGameTests {
    private static void check(boolean value,String message){if(!value)throw new GameTestAssertException(message);}
    private static Set<String> ids(SettingsCatalog catalog){return catalog.root().settings().stream().map(SettingsCatalog.Setting::id).collect(Collectors.toSet());}
    private static void collect(UnmodifiableConfig config,String scope,Set<String> out){
        for(var entry:config.entrySet()){
            Object value=entry.getValue();
            if(value instanceof UnmodifiableConfig nested)collect(nested,scope,out);
            else out.add(scope+String.join(".",((ForgeConfigSpec.ConfigValue<?>)value).getPath()));
        }
    }
    private static SettingsCatalog.Setting setting(SettingsCatalog catalog,String id){return catalog.root().settings().stream().filter(s->s.id().equals(id)).findFirst().orElseThrow();}
    @GameTest(template="empty") public static void allNativeConfigFieldsHaveControlsAndRemoteClientsKeepOnlyLocalChoices(GameTestHelper h){
        var expected=new HashSet<String>();collect(PSConfig.COMMON.getValues(),"common:",expected);collect(PSClientConfig.SPEC.getValues(),"client:",expected);
        var local=new SettingsCatalog(true,()->{});check(ids(local).equals(expected)&&local.root().settings().size()==expected.size(),"every actual native config field has exactly one local-world control");
        var remote=new SettingsCatalog(false,()->{});var localOnly=expected.stream().filter(id->id.startsWith("client:")||id.startsWith("common:balancing.messageDistortion.")).collect(Collectors.toSet());
        check(ids(remote).equals(localOnly)&&!remote.root().worldSettings(),"remote mode exposes client settings and both local message switches without pretending to edit server world configuration");h.succeed();
    }
    @GameTest(template="empty") public static void commonControlConversionsKeepExactTicksAndRestoreCatalogDefaults(GameTestHelper h){
        int ticks=PSConfig.RIFT_SPAWN_INTERVAL.get();boolean hidden=PSConfig.DISABLE_MOLOTOVS.get();var previous=List.copyOf(PSConfig.GENERATION.get("coca").included().get());
        var refresh=new AtomicInteger();var catalog=new SettingsCatalog(true,refresh::incrementAndGet);
        try{
            var interval=setting(catalog,"common:balancing.randomTicksUntilRiftSpawn");
            for(int value:new int[]{1,1199,1201,Integer.MAX_VALUE}){PSConfig.RIFT_SPAWN_INTERVAL.set(value);check(interval.setText(interval.text())&&PSConfig.RIFT_SPAWN_INTERVAL.get()==value,"opening/editing the displayed minutes preserves all configured ticks");}
            check(!interval.setText("-1")&&!interval.setText("Infinity")&&!interval.setText("1789570")&&PSConfig.RIFT_SPAWN_INTERVAL.get()==Integer.MAX_VALUE,"invalid numeric input leaves the prior native setting intact");
            PSConfig.DISABLE_MOLOTOVS.set(false);var molotovs=setting(catalog,"common:balancing.disableMolotovs");
            check(molotovs.enabled(),"inverted display says enabled for the default stored false flag");molotovs.setEnabled(false);molotovs.setEnabled(false);
            check(PSConfig.DISABLE_MOLOTOVS.get()&&refresh.get()==1,"a changed catalog toggle invalidates contents once");molotovs.reset();check(!PSConfig.DISABLE_MOLOTOVS.get()&&refresh.get()==2,"reset restores the actual shipped default and refreshes native contents");
            var included=setting(catalog,"common:worldGeneration.coca.included");check(included.setText("minecraft:forest, #forge:is_jungle"),"native list input is accepted");
            check(PSConfig.GENERATION.get("coca").included().get().equals(List.of("minecraft:forest","#forge:is_jungle")),"biome selector order/data reach the real config value");included.reset();check(PSConfig.GENERATION.get("coca").included().get().isEmpty(),"reset clears selectors to their real default");
            check(!setting(catalog,"common:worldGeneration.villageChests").needsRestart()&&included.needsRestart(),"restart descriptions distinguish live loot from baked biome selection");
        }finally{PSConfig.RIFT_SPAWN_INTERVAL.set(ticks);PSConfig.DISABLE_MOLOTOVS.set(hidden);PSConfig.GENERATION.get("coca").included().set(previous);}
        h.succeed();
    }
    private static void keys(SettingsCatalog.Entry entry,Set<String> out){out.add(entry.titleKey());if(entry instanceof SettingsCatalog.Page page)page.entries().forEach(child->keys(child,out));}
    @GameTest(template="empty") public static void everyNativeSettingsPageAndLabelHasEnglishAndPolishText(GameTestHelper h){
        var keys=new HashSet<String>();keys(new SettingsCatalog(true,()->{}).root(),keys);
        for(String locale:List.of("en_us","pl_pl"))try(var stream=SettingsCatalog.class.getResourceAsStream("/assets/psychedelicraft/lang/"+locale+".json")){
            var language=JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(stream),StandardCharsets.UTF_8)).getAsJsonObject();
            for(String key:keys)check(language.has(key)&&!language.get(key).getAsString().isBlank(),"localized settings label "+locale+" "+key);
        }catch(java.io.IOException error){throw new GameTestAssertException(error.toString());}
        h.succeed();
    }
}
