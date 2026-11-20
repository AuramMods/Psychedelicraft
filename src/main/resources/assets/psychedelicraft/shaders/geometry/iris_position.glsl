uniform float PS_Enabled;
uniform mat4 PS_WorldToClip;
uniform mat4 PS_ClipToWorld;
uniform vec3 PS_CameraPosition;

// Use the viewer's projection for the shared waves, including when this draw writes the shadow map.
vec4 psychedelicraft_terrain_position(vec4 position) {
  if (PS_Enabled <= 0.0 || (all(equal(PS_WavesMatrix, vec3(0.0)))
      && PS_DistantWorldDeformation <= 0.0 && PS_FractalFractureStrength <= 0.0)) return position;
  vec4 clip = PS_WorldToClip * position;
  vec4 deformed = PS_ClipToWorld * psychedelicraft_deform(clip, position.xyz + PS_CameraPosition);
  if (abs(deformed.w) < 0.0001) return position;
  return vec4(deformed.xyz / deformed.w, 1.0);
}
