package ivorius.psychedelicraft.client.render.shader;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class GeometrySourceTest {
    private static String extension(boolean vertex) throws Exception {
        String source = geometryResource(vertex ? "basic.gvsh" : "basic.gfsh");
        return vertex ? source.replace("__PS_DEFORMATION__", geometryResource("deform.glsl")) : source;
    }

    private static String geometryResource(String name) throws Exception {
        try (var in = GeometrySourceTest.class.getResourceAsStream("/assets/psychedelicraft/shaders/geometry/" + name)) {
            assertNotNull(in);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    @Test public void terrainCoordinatesIncludeNativeChunkOffsetBeforeRecoveringWorldPosition()throws Exception{
        String original="#version 150\nin vec3 Position;\nuniform vec3 ChunkOffset;\nuniform mat4 ModelViewMat;\nout vec4 vertexColor;\nvoid main() { gl_Position = ModelViewMat * vec4(Position + ChunkOffset, 1.0); vertexColor=vec4(1); }";
        String actual=GeometrySource.combine(original,extension(true),true);assertTrue(actual.contains("(ModelViewMat * vec4((Position + ChunkOffset), 1.0)).xyz"));assertTrue(actual.contains("vertexColor.rgb = mix(vertexColor.rgb, PS_SurfaceColor"));assertTrue(actual.contains("vec3 position = relative + PS_CameraPosition"));assertFalse(actual.contains("__PS_"));assertEquals(1,actual.split("void main\\(\\)",-1).length-1);
    }
    @Test public void entityStagesRetainNativeMatrixAndMaterialInputs()throws Exception{
        String original="#version 150\nin vec3 Position; uniform mat4 ModelViewMat; out vec4 vertexColor; void main(void) { gl_Position=ModelViewMat*vec4(Position,1); vertexColor=vec4(1); }";
        String actual=GeometrySource.combine(original,extension(true),true);assertTrue(actual.contains("(ModelViewMat * vec4(Position, 1.0)).xyz"));assertFalse(actual.contains("ChunkOffset"));assertTrue(actual.contains("vertexColor=vec4(1)"));assertEquals(actual,GeometrySource.combine(actual,extension(true),true));
    }
    @Test public void commentsAndMissingOptionalNativeVaryingsCannotCreateDuplicateDeclarations()throws Exception{
        String original="#version 150\n// 🌈 void main() { } Position vertexColor ModelViewMat\nin vec3 Position; /* out vec4 vertexColor; */ void main() { gl_Position=vec4(Position,1); }";
        String actual=GeometrySource.combine(original,extension(true),true);assertTrue(actual.contains("vec3 view = Position;"));assertFalse(actual.contains("vertexColor.rgb = mix"));assertTrue(actual.contains("// 🌈 void main() { }"));
    }
    @Test public void texturedFragmentKeepsDiscardLightingAndFogWithoutDoubleSurfaceMix()throws Exception{
        String original="#version 150\nin vec4 vertexColor; out vec4 colorOut; void main() { vec4 color=vertexColor; if(color.a<.1)discard; colorOut=color; }";
        String actual=GeometrySource.combine(original,extension(false),false);
        assertTrue(actual.contains("if(color.a<.1)discard"));
        assertTrue(actual.contains("colorOut=color"));
        assertTrue(actual.contains("colorOut.rgb = mix(colorOut.rgb, clamp(PS_Pulses.rgb, 0.0, 1.0), pulse *"));
        assertFalse(actual.contains("colorOut.rgb, PS_SurfaceColor"));
        assertFalse(actual.contains("__PS_"));
    }
    @Test public void uncoloredNativeMaterialsModulateBeforeTheirTextureAndFog()throws Exception{
        String original="#version 150\nuniform vec4 ColorModulator; out vec4 fragColor; void main() { fragColor = texture(Sampler0, uv) * ColorModulator; }";
        String actual=GeometrySource.combine(original,extension(false),false);assertTrue(actual.contains("uniform vec4 ColorModulator;"));assertTrue(actual.contains("texture(Sampler0, uv) * psychedelicraft_modulate(ColorModulator)"));assertTrue(actual.indexOf("vec4 psychedelicraft_modulate(vec4 color);")<actual.indexOf("void psychedelicraft_parent_main()"));assertFalse(actual.contains("fragColor.rgb = mix(fragColor.rgb, PS_SurfaceColor"));
    }
    @Test public void depthOnlyAndPositionFreeProgramsKeepValidEntryPoints()throws Exception{
        String vertex=GeometrySource.combine("#version 150\nvoid main() { gl_Position=vec4(0); }",extension(true),true);assertTrue(vertex.contains("vec3 view = vec3(0.0);"));assertFalse(vertex.contains("__PS_"));
        String depth=GeometrySource.combine("#version 150\nvoid main() { gl_FragDepth=.5; }",extension(false),false);assertTrue(depth.contains("gl_FragDepth=.5"));assertTrue(depth.contains("void main() { psychedelicraft_parent_main(); }"));assertFalse(depth.contains("fragColor"));
    }
}
