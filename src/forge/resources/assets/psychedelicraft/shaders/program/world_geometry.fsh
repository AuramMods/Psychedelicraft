#version 120

uniform sampler2D ColorSampler;
uniform sampler2D LightmapSampler;
uniform float LightmapMode;
uniform float WorldTicks;
uniform float SurfaceFractalStrength;
uniform vec4 Pulses;
uniform float FogMode;
uniform float RadialFog;

varying vec2 atlasUv;
varying vec2 lightmapUv;
varying vec4 vertexColor;
varying vec3 viewPosition;
varying vec3 surfaceColor;

void main() {
  vec4 light = vec4(1.0);
  if (LightmapMode == 1.0) {
    light = texture2D(LightmapSampler, lightmapUv);
  } else if (LightmapMode == 2.0) {
    light = texture2D(LightmapSampler, vec2(0.5));
  }
  vec4 base = texture2D(ColorSampler, atlasUv);
  vec4 color = vec4(mix(vertexColor.rgb * light.rgb, surfaceColor, SurfaceFractalStrength),
                    vertexColor.a * light.a) * base;

  float distanceToEye = RadialFog > 0.0 ? length(viewPosition) : abs(viewPosition.z);
  float fog = 1.0;
  if (FogMode == 1.0) {
    fog = clamp((gl_Fog.end - distanceToEye) * gl_Fog.scale, 0.0, 1.0);
  } else if (FogMode == 2.0) {
    fog = exp(-distanceToEye * gl_Fog.density);
  } else if (FogMode == 3.0) {
    float density = distanceToEye * gl_Fog.density;
    fog = exp(-density * density);
  }
  color.rgb = mix(gl_Fog.color.rgb, color.rgb, fog);

  float pulse = max(0.0, sin((distanceToEye - WorldTicks) / 5.0) - 0.4) * Pulses.a;
  color.rgb = mix(color.rgb, (color.rgb + 1.0) * Pulses.rgb, pulse);
  gl_FragColor = color;
}
