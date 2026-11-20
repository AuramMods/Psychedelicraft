#version 150
#moj_import <psychedelicraft:random_from_vec.glsl>
#moj_import <psychedelicraft:get_desaturated_color.glsl>
#moj_import <psychedelicraft:reduce_pallete.glsl>
#moj_import <psychedelicraft:pixelate.glsl>
#moj_import <psychedelicraft:linear.glsl>

#moj_import <psychedelicraft:scene_depth.glsl>

uniform vec2 depthRange;

float getPixelDensity(vec2 newUV, vec4 newColor) {
  float textureDepth = psychedelicraft_scene_depth(newUV);
  return linearize(textureDepth, depthRange.x, depthRange.y);
}

#moj_import <psychedelicraft:apply_digitize.glsl>

void main() {
  apply_digitize();
}
