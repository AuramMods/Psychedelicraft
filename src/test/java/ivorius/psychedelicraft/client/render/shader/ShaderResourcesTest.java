package ivorius.psychedelicraft.client.render.shader;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.*;
import org.junit.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import static org.junit.Assert.*;

public class ShaderResourcesTest {
    private static final ResourceLocation ROOT=new ResourceLocation("psychedelicraft","shaders/program/probe.fsh");
    private static Resource resource(String source){return new Resource(null,()->new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)));}
    private static String read(Resource source)throws IOException{try(var reader=source.openAsReader()){return reader.lines().collect(java.util.stream.Collectors.joining("\n"));}}
    private static final class Memory implements ResourceManager {
        final Map<ResourceLocation,List<Resource>> files=new HashMap<>();
        Memory add(String id,String text){files.computeIfAbsent(new ResourceLocation(id),key->new ArrayList<>()).add(resource(text));return this;}
        @Override public Optional<Resource> getResource(ResourceLocation id){var stack=files.get(id);return stack==null?Optional.empty():Optional.of(stack.get(stack.size()-1));}
        @Override public List<Resource> getResourceStack(ResourceLocation id){return files.getOrDefault(id,List.of());}
        @Override public Set<String> getNamespaces(){return Set.of("psychedelicraft");}
        @Override public Map<ResourceLocation,Resource> listResources(String prefix,Predicate<ResourceLocation> filter){var result=new HashMap<ResourceLocation,Resource>();files.forEach((id,stack)->{if(id.getPath().startsWith(prefix)&&filter.test(id))result.put(id,stack.get(stack.size()-1));});return result;}
        @Override public Map<ResourceLocation,List<Resource>> listResourceStacks(String prefix,Predicate<ResourceLocation> filter){var result=new HashMap<ResourceLocation,List<Resource>>();files.forEach((id,stack)->{if(id.getPath().startsWith(prefix)&&filter.test(id))result.put(id,stack);});return result;}
        @Override public Stream<PackResources> listPacks(){return Stream.empty();}
    }
    @Test public void expandsNamespacedAndRelativeImportsAndKeepsOnlyRootVersion()throws Exception{
        var memory=new Memory().add(ROOT.toString(),"#version 150\n#moj_import <psychedelicraft:first.glsl>\nvoid main() {}\n")
                .add("psychedelicraft:shaders/include/first.glsl","#version 150\n#moj_import \"relative.glsl\"\nfloat first;\n")
                .add("psychedelicraft:shaders/include/relative.glsl","float relative;\n");
        assertEquals("#version 150\nfloat relative;\nfloat first;\nvoid main() {}",read(new ShaderResources(memory).getResourceOrThrow(ROOT)));
    }
    @Test public void deduplicatesDiamondImportsWithinEachIndependentRead()throws Exception{
        var memory=new Memory().add(ROOT.toString(),"#moj_import <psychedelicraft:first.glsl>\n#moj_import <psychedelicraft:second.glsl>\n")
                .add("psychedelicraft:shaders/include/first.glsl","#moj_import <psychedelicraft:common.glsl>\nfloat first;")
                .add("psychedelicraft:shaders/include/second.glsl","#moj_import <psychedelicraft:common.glsl>\nfloat second;")
                .add("psychedelicraft:shaders/include/common.glsl","float common;");
        var resources=new ShaderResources(memory);String first=read(resources.getResourceOrThrow(ROOT));assertEquals("float common;\nfloat first;\nfloat second;",first);assertEquals(first,read(resources.getResourceOrThrow(ROOT)));
    }
    @Test public void rejectsCircularAndMissingImportsWithTheirActualResourcePath()throws Exception{
        var memory=new Memory().add(ROOT.toString(),"#moj_import <psychedelicraft:loop.glsl>\n")
                .add("psychedelicraft:shaders/include/loop.glsl","#moj_import <psychedelicraft:loop.glsl>\n");
        var circular=assertThrows(IOException.class,()->read(new ShaderResources(memory).getResourceOrThrow(ROOT)));assertTrue(circular.getMessage().contains("psychedelicraft:shaders/include/loop.glsl"));
        var missing=new Memory().add(ROOT.toString(),"#moj_import <psychedelicraft:missing.glsl>\n");assertThrows(IOException.class,()->read(new ShaderResources(missing).getResourceOrThrow(ROOT)));
    }
    @Test public void retainsEachRootResourceInPackStacksRatherThanRepeatingTheTopPack()throws Exception{
        var memory=new Memory().add(ROOT.toString(),"float lower;").add(ROOT.toString(),"float upper;");
        var resources=new ShaderResources(memory);var stack=resources.getResourceStack(ROOT);assertEquals("float lower;",read(stack.get(0)));assertEquals("float upper;",read(stack.get(1)));assertEquals("float upper;",read(resources.getResourceOrThrow(ROOT)));
        var listed=resources.listResourceStacks("shaders",id->true).get(ROOT);assertEquals("float lower;",read(listed.get(0)));assertEquals("float upper;",read(resources.listResources("shaders",id->true).get(ROOT)));
    }
}
