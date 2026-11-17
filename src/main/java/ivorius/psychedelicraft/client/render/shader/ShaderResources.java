package ivorius.psychedelicraft.client.render.shader;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** EffectInstance lacks the core shader import loader; expand native mod imports at resource reads. */
final class ShaderResources implements ResourceManager {
    private static final Pattern IMPORT=Pattern.compile("\\s*#moj_import\\s+([<\"])([^>\"]+)[>\"].*");
    private final ResourceManager parent;
    ShaderResources(ResourceManager parent){this.parent=parent;}
    private Resource wrap(ResourceLocation id,Resource resource){
        if(!id.getPath().endsWith(".vsh")&&!id.getPath().endsWith(".fsh"))return resource;
        return new Resource(resource.source(),()->new ByteArrayInputStream(expand(id,resource,new LinkedHashSet<>(),new HashSet<>(),false).getBytes(StandardCharsets.UTF_8)),resource::metadata);
    }
    private String expand(ResourceLocation id,Resource resource,Set<ResourceLocation> active,Set<ResourceLocation> included,boolean imported)throws IOException{
        if(active.contains(id))throw new IOException("Circular shader import: "+active+" -> "+id);
        if(!included.add(id))return "";
        active.add(id);var result=new StringBuilder();
        try(var reader=resource.openAsReader()){
            for(String line;(line=reader.readLine())!=null;){
                var match=IMPORT.matcher(line);
                if(match.matches()){
                    String value=match.group(2);ResourceLocation next;
                    if(match.group(1).equals("\"")&&!value.contains(":"))next=new ResourceLocation(id.getNamespace(),id.getPath().substring(0,id.getPath().lastIndexOf('/')+1)+value);
                    else {var raw=new ResourceLocation(value);next=new ResourceLocation(raw.getNamespace(),"shaders/include/"+raw.getPath());}
                    result.append(expand(next,parent.getResourceOrThrow(next),active,included,true));
                }else if(!imported||!line.stripLeading().startsWith("#version"))result.append(line).append('\n');
            }
        }finally{active.remove(id);}
        return result.toString();
    }
    @Override public Optional<Resource> getResource(ResourceLocation id){return parent.getResource(id).map(resource->wrap(id,resource));}
    @Override public Set<String> getNamespaces(){return parent.getNamespaces();}
    @Override public List<Resource> getResourceStack(ResourceLocation id){return parent.getResourceStack(id).stream().map(resource->wrap(id,resource)).toList();}
    @Override public Map<ResourceLocation,Resource> listResources(String path,Predicate<ResourceLocation> test){var result=new HashMap<ResourceLocation,Resource>();parent.listResources(path,test).forEach((id,resource)->result.put(id,wrap(id,resource)));return result;}
    @Override public Map<ResourceLocation,List<Resource>> listResourceStacks(String path,Predicate<ResourceLocation> test){var result=new HashMap<ResourceLocation,List<Resource>>();parent.listResourceStacks(path,test).forEach((id,list)->result.put(id,list.stream().map(resource->wrap(id,resource)).toList()));return result;}
    @Override public Stream<PackResources> listPacks(){return parent.listPacks();}
}
