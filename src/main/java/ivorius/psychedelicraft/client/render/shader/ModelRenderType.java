package ivorius.psychedelicraft.client.render.shader;

import com.mojang.blaze3d.vertex.*;
import ivorius.psychedelicraft.client.render.RenderPhase;
import net.minecraft.client.renderer.RenderType;
import java.util.*;

/** Moving blocks share terrain programs; carry model context until their deferred buffer is drawn. */
public final class ModelRenderType extends RenderType {
    private static final Map<RenderType,RenderType> TYPES=new IdentityHashMap<>();
    private final RenderType delegate;
    private ModelRenderType(RenderType delegate){
        super("psychedelicraft_moving_model",delegate.format(),delegate.mode(),delegate.bufferSize(),delegate.affectsCrumbling(),false,delegate::setupRenderState,delegate::clearRenderState);
        this.delegate=delegate;
    }
    public static RenderType of(RenderType delegate){return TYPES.computeIfAbsent(delegate,ModelRenderType::new);}
    @Override public void end(BufferBuilder builder,VertexSorting sorting){try(var scope=RenderPhase.model()){delegate.end(builder,sorting);}}
    @Override public Optional<RenderType> outline(){return delegate.outline();}
    @Override public boolean isOutline(){return delegate.isOutline();}
}
