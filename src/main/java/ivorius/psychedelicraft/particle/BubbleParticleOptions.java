/* Target-version particle data adapted from Sollace's BubbleParticleEffect. */
package ivorius.psychedelicraft.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import org.joml.Vector3f;
import java.util.function.Function;

public final class BubbleParticleOptions extends DustParticleOptions {
    public static final Codec<DustParticleOptions> CODEC=DustParticleOptions.CODEC.xmap(
            effect->new BubbleParticleOptions(effect.getColor(),effect.getScale()),Function.identity());
    public static final ParticleOptions.Deserializer<DustParticleOptions> DESERIALIZER=new ParticleOptions.Deserializer<>() {
        @Override public DustParticleOptions fromCommand(ParticleType<DustParticleOptions> type,StringReader reader)throws CommandSyntaxException{
            Vector3f color=readVector3f(reader);reader.expect(' ');return new BubbleParticleOptions(color,reader.readFloat());
        }
        @Override public DustParticleOptions fromNetwork(ParticleType<DustParticleOptions> type,FriendlyByteBuf buffer){return new BubbleParticleOptions(readVector3f(buffer),buffer.readFloat());}
    };
    public BubbleParticleOptions(Vector3f color,float scale){super(color,scale);}
    public BubbleParticleOptions(int color,float scale){this(new Vector3f((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F),scale);}
    @Override public ParticleType<DustParticleOptions> getType(){return PSParticles.BUBBLE.get();}
}
