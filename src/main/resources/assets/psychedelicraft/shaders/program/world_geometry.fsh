#version 120

uniform sampler2D ColorSampler;
uniform sampler2D LightmapSampler;
uniform float LightmapMode;
uniform float WorldTicks;
uniform float SurfaceFractalStrength;
uniform vec4 Pulses;
uniform float FogMode;
uniform float RadialFog;
uniform float RiftCenter;
uniform float EntityModel;
uniform float Untextured;
uniform float PortalTexGen;
uniform float EntityTextured;
uniform vec4 EntityOverlay;
uniform vec4 EntityOutline;
uniform vec4 EntityTint;

varying vec4 colorUv;
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
  vec4 base = Untextured > 0.0 ? vec4(1.0) : PortalTexGen > 0.0
      ? texture2DProj(ColorSampler, colorUv) : texture2D(ColorSampler, colorUv.xy);
  vec4 color;
  if (EntityModel > 0.0) {
    if (EntityTextured == 0.0) base = vec4(1.0);
    if (EntityOutline.a > 0.0) {
      gl_FragColor = vec4(EntityOutline.rgb, base.a);
      return;
    }
    color = vec4(mix(vertexColor.rgb, surfaceColor, SurfaceFractalStrength), vertexColor.a) * base;
    if (EntityOverlay.a > 0.0) {
      color = vec4(mix(color.rgb, EntityOverlay.rgb, EntityOverlay.a), base.a);
    }
    color *= light * EntityTint;
  } else {
    color = vec4(mix(vertexColor.rgb * light.rgb, surfaceColor, SurfaceFractalStrength),
                 vertexColor.a * light.a) * base;
  }

  float distanceToEye = RadialFog > 0.0 ? length(viewPosition) : abs(viewPosition.z);
  float fog = 1.0;
  if (FogMode == 1.0) {
    fog = RiftCenter > 0.0
        ? 1.0 - smoothstep(gl_Fog.start, gl_Fog.end, length(viewPosition))
        : clamp((gl_Fog.end - distanceToEye) * gl_Fog.scale, 0.0, 1.0);
  } else if (FogMode == 2.0) {
    fog = exp(-distanceToEye * gl_Fog.density);
  } else if (FogMode == 3.0) {
    float density = distanceToEye * gl_Fog.density;
    fog = exp(-density * density);
  }
  if (RiftCenter > 0.0) {
    if (base.a < 0.1) discard;
    color *= fog;
  } else {
    color.rgb = mix(gl_Fog.color.rgb, color.rgb, fog);
  }

  float pulse = max(0.0, sin((distanceToEye - WorldTicks) / 5.0) - 0.4) * Pulses.a;
  color.rgb = mix(color.rgb, (color.rgb + 1.0) * Pulses.rgb, pulse);
  gl_FragColor = color;
}
