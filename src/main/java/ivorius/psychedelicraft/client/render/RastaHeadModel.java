/* Copyright (c) 2014, Lukas Tenbrink. Techne model adapted to modern model parts by Sollace. */
package ivorius.psychedelicraft.client.render;

import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;

final class RastaHeadModel {
    private RastaHeadModel(){}
    static ModelPart create(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        var head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).mirror().addBox(-4,-8,-4,8,8,8),PartPose.ZERO);
        head.addOrReplaceChild("nose",CubeListBuilder.create().texOffs(33,0).mirror().addBox(-.5F,-4.8F,-5,1,2,1),PartPose.rotation(-.0743572F,0,0));
        head.addOrReplaceChild("nostrils",CubeListBuilder.create().texOffs(38,0).mirror().addBox(-1,-4.2F,-4.5F,2,1,1),PartPose.ZERO);
        head.addOrReplaceChild("hair",CubeListBuilder.create().texOffs(0,17).mirror().addBox(-4.5F,-7.5F,-4.5F,9,11,9),PartPose.ZERO);
        root.addOrReplaceChild("hat",CubeListBuilder.create().texOffs(0,38).mirror().addBox(-5,-10,-8.5F,10,7,12),PartPose.rotation(-.669215F,0,0));
        root.addOrReplaceChild("joint",CubeListBuilder.create().texOffs(0,0).mirror().addBox(-.5F,-3.5F,-5,1,1,2),PartPose.rotation(.4089647F,-.2602503F,0));
        return LayerDefinition.create(mesh,128,64).bakeRoot();
    }
}
