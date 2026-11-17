package ivorius.psychedelicraft.client.screen.settings;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class SettingInputTest {
    @Test public void minuteDisplayRoundTripsEveryTestedTickWithoutRoundingItToAWholeMinute(){
        for(int ticks:new int[]{0,1,2,599,1199,1200,1201,216000,2160001,Integer.MAX_VALUE})assertEquals(ticks,SettingInput.ticks(SettingInput.minutes(ticks)));
        assertEquals(600,SettingInput.ticks("0.5"));assertEquals(2,SettingInput.ticks("0.00125"));
    }
    @Test public void invalidAndOverflowingMinuteInputsAreRejected(){
        for(String text:new String[]{"","-1","NaN","Infinity","1789570","2147483647","abc"}){
            try{SettingInput.ticks(text);fail(text);}catch(IllegalArgumentException|ArithmeticException expected){}
        }
    }
    @Test public void doubleInputsRejectNonFiniteValues(){
        assertEquals(.05,SettingInput.finiteDouble(" 0.05 "),0);
        for(String text:new String[]{"NaN","Infinity","-Infinity","1e999"})try{SettingInput.finiteDouble(text);fail(text);}catch(IllegalArgumentException expected){}
    }
    @Test public void biomeListsKeepOrderAndDuplicatesAndCanBeCleared(){
        assertEquals(List.of("minecraft:forest","#forge:is_jungle","minecraft:forest"),SettingInput.selectors(" minecraft:forest, #forge:is_jungle,,minecraft:forest "));
        assertEquals(List.of(),SettingInput.selectors(" , , "));assertEquals("0.05",SettingInput.decimal(.05000000001));
    }
}
