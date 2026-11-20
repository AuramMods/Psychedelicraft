package ivorius.psychedelicraft.client.render.shader;

import com.mojang.logging.LogUtils;

import java.lang.reflect.Method;

/** Borrow Iris's two existing near-depth snapshots for the current frame. */
record OculusHandDepth(int beforeHand, int withSolidHand) {
    private record Access(Method beforeHand, Method withSolidHand, Method textureId) {}
    private static final ClassValue<Access> ACCESS = new ClassValue<>() {
        @Override
        protected Access computeValue(Class<?> type) {
            try {
                var beforeHand = type.getMethod("getDepthTextureNoHand");
                return new Access(beforeHand, type.getMethod("getDepthTextureNoTranslucents"),
                        beforeHand.getReturnType().getMethod("getTextureId"));
            } catch (ReflectiveOperationException error) {
                LogUtils.getLogger().debug("Oculus hand depth snapshots are unavailable", error);
                return null;
            }
        }
    };

    static OculusHandDepth read(Object targets) {
        var access = ACCESS.get(targets.getClass());
        if (access == null) return null;
        try {
            int beforeHand = (int)access.textureId.invoke(access.beforeHand.invoke(targets));
            int withHand = (int)access.textureId.invoke(access.withSolidHand.invoke(targets));
            return beforeHand > 0 && withHand > 0 ? new OculusHandDepth(beforeHand, withHand) : null;
        } catch (ReflectiveOperationException error) {
            return null;
        }
    }
}
