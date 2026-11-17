package ivorius.psychedelicraft.client.screen.settings;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

/** Preserve the accepted minute display without losing an existing partial-minute tick value. */
public final class SettingInput {
    private static final BigDecimal MINUTE=BigDecimal.valueOf(1200);
    private SettingInput(){}
    public static String minutes(int ticks){return BigDecimal.valueOf(ticks).divide(MINUTE,12,RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();}
    public static int ticks(String minutes){
        BigDecimal value=new BigDecimal(minutes.trim());
        if(value.signum()<0)throw new IllegalArgumentException("Negative interval");
        return value.multiply(MINUTE).setScale(0,RoundingMode.HALF_UP).intValueExact();
    }
    public static double finiteDouble(String text){double value=Double.parseDouble(text.trim());if(!Double.isFinite(value))throw new IllegalArgumentException("Non-finite value");return value;}
    public static String decimal(double value){return BigDecimal.valueOf(value).setScale(2,RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();}
    public static List<String> selectors(String text){return Arrays.stream(text.split(",",-1)).map(String::trim).filter(s->!s.isEmpty()).toList();}
}
