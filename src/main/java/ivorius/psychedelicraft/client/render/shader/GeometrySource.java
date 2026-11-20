/* Copyright (c) 2014, Lukas Tenbrink. Modern shader composition originally by Sollace. */
package ivorius.psychedelicraft.client.render.shader;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/** Compose core stages without changing the native program's lighting, fog, textures or discard paths. */
public final class GeometrySource {
    private static final Pattern COMMENTS=Pattern.compile("(?s)/\\*.*?\\*/|//[^\\r\\n]*");
    private static final Pattern MAIN=Pattern.compile("\\bvoid\\s+main\\s*\\(\\s*(?:void\\s*)?\\)");
    private static final Pattern IRIS_CHUNK_POSITION = Pattern.compile(
            "\\bvec4\\s+getVertexPosition\\s*\\(\\s*\\)\\s*\\{\\s*return\\s+"
                    + "(vec4\\(\\s*(?:modelOffset\\s*\\+\\s*)?_vert_position[^;]*\\));");
    private GeometrySource(){}
    static String code(String source){return COMMENTS.matcher(source).replaceAll(match->{char[] chars=match.group().toCharArray();for(int i=0;i<chars.length;i++)if(chars[i]!='\r'&&chars[i]!='\n')chars[i]=' ';return new String(chars);});}
    static boolean declares(String code,String qualifier,String type,String name){return Pattern.compile("\\b"+qualifier+"\\s+"+type+"\\s+[^;{}]*\\b"+name+"\\b[^;{}]*;").matcher(code).find();}
    public static String combine(String source,String extension,boolean vertex){
        String code=code(source);if(code.contains("void psychedelicraft_parent_main"))return source;
        var main=MAIN.matcher(code);if(!main.find())return source;
        String parent=renameMain(source,main);
        if(vertex){
            boolean position=declares(code,"in","vec3","Position"),model=declares(code,"uniform","mat4","ModelViewMat");
            String point=position?"Position":"vec3(0.0)";
            if(position&&declares(code,"uniform","vec3","ChunkOffset"))point="(Position + ChunkOffset)";
            String view=model?"(ModelViewMat * vec4("+point+", 1.0)).xyz":point;
            extension=extension.replace("__PS_VIEW_POSITION__",view)
                    .replace("__PS_SURFACE_COLOR__",declares(code,"out","vec4","vertexColor")?"vertexColor.rgb = mix(vertexColor.rgb, PS_SurfaceColor, PS_SurfaceFractalStrength);":"");
        }else{
            var output=Pattern.compile("\\bout\\s+vec4\\s+(\\w+)\\s*;").matcher(code);
            if(!output.find())return parent+"\nvoid main() { psychedelicraft_parent_main(); }\n";
            String color=output.group(1);
            if(!declares(code,"in","vec4","vertexColor")&&declares(code,"uniform","vec4","ColorModulator")){
                int open=code.indexOf('{',main.end()),close=open,depth=0;
                do{char c=code.charAt(close++);if(c=='{')depth++;else if(c=='}')depth--;}while(depth>0&&close<code.length());
                String body=source.substring(main.end(),close).replaceAll("\\bColorModulator\\b","psychedelicraft_modulate(ColorModulator)");
                parent=source.substring(0,main.start())+"vec4 psychedelicraft_modulate(vec4 color);\nvoid psychedelicraft_parent_main()"+body+source.substring(close);
                extension=extension.replace("__PS_SURFACE_FALLBACK__","");
            }
            extension=extension.replace("__PS_FRAGMENT_COLOR__",color)
                    .replace("__PS_SURFACE_FALLBACK__",declares(code,"in","vec4","vertexColor")?"":color+".rgb = mix("+color+".rgb, PS_SurfaceColor, PS_SurfaceFractalStrength);");
        }
        return parent+"\n// Psychedelicraft geometry stage\n"+extension;
    }

    static String combineTerrain(String source, String extension, boolean vertex) {
        var main = MAIN.matcher(code(source));
        if (!main.find()) return source;
        if (vertex) {
            extension = extension.replace("__PS_VIEW_POSITION__",
                            "(u_ModelViewMatrix * vec4(_vert_position + u_RegionOffset + _get_draw_translation(_draw_id), 1.0)).xyz")
                    .replace("PS_SurfaceFractalSampler", "u_BlockTex")
                    .replace("__PS_SURFACE_COLOR__", "v_Color.rgb = mix(v_Color.rgb, PS_SurfaceColor, PS_SurfaceFractalStrength);");
        } else {
            extension = extension.replace("__PS_FRAGMENT_COLOR__", "fragColor")
                    .replace("__PS_SURFACE_FALLBACK__", "");
        }
        return renameMain(source, main) + "\n// Psychedelicraft terrain stage\n" + extension;
    }

    static String deformIrisTerrain(String source, String deformation) {
        var vertex = IRIS_CHUNK_POSITION.matcher(code(source));
        if (!vertex.find()) return source;
        String position = source.substring(vertex.start(1), vertex.end(1));
        return source.substring(0, vertex.start()) + "vec4 psychedelicraft_terrain_position(vec4 position);\n"
                + source.substring(vertex.start(), vertex.start(1))
                + "psychedelicraft_terrain_position(" + position + ")" + source.substring(vertex.end(1))
                + "\n" + deformation;
    }

    static String deformDistantTerrain(String source, String deformation) {
        String code = code(source);
        var main = MAIN.matcher(code);
        if (!main.find() || !declares(code, "out", "vec3", "vertexWorldPos")
                || !declares(code, "uniform", "mat4", "uCombinedMatrix")) return source;
        return renameMain(source, main) + "\n" + deformation;
    }

    static boolean supportsIrisMaterial(String vertex, String fragment) {
        String vertexCode = code(vertex);
        String fragmentCode = code(fragment);
        return MAIN.matcher(vertexCode).find() && declares(vertexCode, "out", "vec4", "color")
                && MAIN.matcher(fragmentCode).find() && declares(fragmentCode, "in", "vec4", "color")
                && declares(fragmentCode, "uniform", "sampler2D", "gtexture")
                && declares(fragmentCode, "out", "vec4", "iris_FragData0");
    }

    static String irisTerrainMaterial(String source, String extension, boolean vertex) {
        String code = code(source);
        var main = MAIN.matcher(code);
        if (!main.find()) return source;
        if (vertex) {
            String sampler = declares(code, "uniform", "sampler2D", "gtexture") ? "" : "uniform sampler2D gtexture;\n";
            extension = sampler + extension;
        } else {
            extension = extension.replace("__PS_FRAGMENT_COLOR__", "iris_FragData0")
                    .replace("__PS_SURFACE_FALLBACK__", "");
        }
        return renameMain(source, main) + "\n" + extension;
    }

    private static String renameMain(String source, Matcher main) {
        return source.substring(0, main.start()) + "void psychedelicraft_parent_main()" + source.substring(main.end());
    }
}
