#version 150
#moj_import <psychedelicraft:get_rotated_color.glsl>
#moj_import <psychedelicraft:get_intensified_color.glsl>
#moj_import <psychedelicraft:get_desaturated_color.glsl>
#moj_import <psychedelicraft:get_inverted_color.glsl>

uniform sampler2D DiffuseSampler;

in vec2 texCoord;

uniform float ticks;

uniform float slowColorRotation;
uniform float quickColorRotation;
uniform float colorIntensification;
uniform float desaturation;
uniform float inversion;

out vec4 fragColor;

void main() {
  vec4 texel = texture(DiffuseSampler, texCoord);
  vec3 outcolor = texel.rgb;

  if (slowColorRotation > 0.0) {
    outcolor = mix(outcolor, getRotatedColor(outcolor, mod(ticks, 300.0) / 300.0), clamp(slowColorRotation, 0.0, 1.0) * 0.4);
  }

  if (quickColorRotation > 0.0) {
     outcolor = mix(outcolor, getRotatedColor(outcolor, mod(ticks, 50.0) / 50.0), clamp(quickColorRotation, 0.0, 1.0) * 0.45);
  }

  if (colorIntensification != 0.0) {
    outcolor = mix(outcolor, getIntensifiedColor(outcolor), clamp(colorIntensification, 0.0, 1.0));
  }

  if (desaturation != 0.0) {
    outcolor = mix(outcolor, getDesaturatedColor(outcolor), clamp(desaturation, 0.0, 1.0));
  }

  if (inversion > 0) {
    outcolor = mix(outcolor, getInvertedColor(outcolor), inversion);
  }

  fragColor = vec4(outcolor, texel.a);
}
