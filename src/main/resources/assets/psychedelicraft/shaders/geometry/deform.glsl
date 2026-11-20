/* Original wave equations: Lukas Tenbrink / Sollace. */
uniform float PS_WorldTicks;
uniform vec3 PS_WavesMatrix;
uniform float PS_DistantWorldDeformation;
uniform float PS_FractalFractureStrength;
uniform float PS_PeyoteStrength;
uniform mat4 PS_WorldToClip;
uniform vec3 PS_PlayerPosition;
uniform float PS_NativeTerrainRadius;
float psychedelicraft_gold_noise(vec2 xy, float seed) {
  const float phi = 1.61803398874989484820459;
  return fract(tan(distance(xy * phi, xy) * seed) * xy.x);
}

vec2 psychedelicraft_fracture(vec3 position) {
  if (PS_FractalFractureStrength <= 0.0) return vec2(0.0);
  float scroll = PS_WorldTicks / 100.0;
  return sin(vec2(psychedelicraft_gold_noise(position.xy, position.x + scroll),
                  psychedelicraft_gold_noise(position.xy, position.y + scroll))) * PS_FractalFractureStrength;
}

// The legacy distance-based waves measure camera-space distance before projection.
vec4 psychedelicraft_legacy_waves(vec4 clip, vec3 position, float viewDistance, vec2 fracture) {
  float worldTicks = PS_WorldTicks;
  float smallWaves = PS_WavesMatrix.x;
  float bigWaves = PS_WavesMatrix.y;
  float wiggleWaves = PS_WavesMatrix.z;
  float distantWorldDeformation = PS_DistantWorldDeformation;
  vec3 playerPos = PS_PlayerPosition;
  if (smallWaves > 0.0) {
    float w1 = 8.0;

    clip[1] += sin((position.x + worldTicks / 5.0) / w1 * 3.14159 * 2.0) * sin((position.z + worldTicks / 5.0) / w1 * 3.14159 * 2.0) * smallWaves * 1.5;
    clip[1] -= sin((playerPos.x + worldTicks / 5.0) / w1 * 3.14159 * 2.0) * sin((playerPos.z + worldTicks / 5.0) / w1 * 3.14159 * 2.0) * smallWaves * 1.5;

    float w2 = 16.0;

    clip.y += sin((position.x + worldTicks / 8.0) / w2 * 3.14159 * 2.0) * sin((position.z) / w2 * 3.14159 * 2.0) * smallWaves * 3.0;
    clip.y -= sin((playerPos.x + worldTicks / 8.0) / w2 * 3.14159 * 2.0) * sin((playerPos.z) / w2 * 3.14159 * 2.0) * smallWaves * 3.0;

    clip.x = mix(clip.x, clip[0] * (1.0 + viewDistance / 20.0), smallWaves);
    clip.y = mix(clip.y, clip[1] * (1.0 + viewDistance / 20.0), smallWaves);
  }


  clip.xy += fracture;

  if (wiggleWaves > 0.0) {
    float w1 = 8.0;
    clip.x += sin((position.y + worldTicks / 8.0) / w1 * 3.14159 * 2.0)
                    * sin((position.z + worldTicks / 5.0) / w1 * 3.14159 * 2.0)
                    * wiggleWaves;
  }

  if (distantWorldDeformation > 0.0 && viewDistance > 5.0) {
    clip.y += (sin(viewDistance / 8.0 * 3.14159 * 2.0) + 1.0)
                     * distantWorldDeformation
                     * (viewDistance - 5.0) / 8.0;
  }

  if (bigWaves > 0.0) {
    if (clip[2] > 0.1) {
      float dDist = (clip[2] - 0.1) * bigWaves;
      if (clip[2] > 20.0) {
        dDist = (20.0 - 0.1) * bigWaves + (clip.z - 20.0) * bigWaves * 0.3;
      }

      float inf1 = sin(worldTicks * 0.0086465563) * dDist;
      float inf2 = cos(worldTicks * 0.0086465563) * dDist;
      float inf3 = sin(worldTicks * 0.0091033941) * dDist;
      float inf4 = cos(worldTicks * 0.0091033941) * dDist;
      float inf5 = sin(worldTicks * 0.0064566190) * dDist;
      float inf6 = cos(worldTicks * 0.0064566190) * dDist;

      float pMul = 1.3;

      clip.x += sin(clip.z * 0.1 * sin(worldTicks * 0.001849328) + worldTicks * 0.014123412) * 0.5 * inf1 * pMul;
      clip.y += cos(clip.z * 0.1 * sin(worldTicks * 0.001234728) + worldTicks * 0.017481893) * 0.4 * inf1 * pMul;

      clip.x += sin(clip.y * 0.1 * sin(worldTicks * 0.001523784) + worldTicks * 0.021823911) * 0.2 * inf2 * pMul;
      clip.y += sin(clip.x * 0.1 * sin(worldTicks * 0.001472387) + worldTicks * 0.023193141) * 0.08 * inf2 * pMul;

      clip.x += sin(clip.z * 0.15 * sin(worldTicks * 0.001284923) + worldTicks * 0.019404289) * 0.25 * inf3 * pMul;
      clip.y += cos(clip.z * 0.15 * sin(worldTicks * 0.001482938) + worldTicks * 0.018491238) * 0.15 * inf3 * pMul;

      clip.x += sin(clip.y * 0.05 * sin(worldTicks * 0.001283942) + worldTicks * 0.012942342) * 0.4 * inf4 * pMul;
      clip.y += sin(clip.x * 0.05 * sin(worldTicks * 0.001829482) + worldTicks * 0.012981328) * 0.35 * inf4 * pMul;

      clip.z += sin(clip.y * 0.13 * sin(worldTicks * 0.02834472) + worldTicks * 0.023482934) * 0.1 * inf5 * pMul;
      clip.z += sin(clip.x * 0.124 * sin(worldTicks * 0.00184298) + worldTicks * 0.018394082) * 0.05 * inf6 * pMul;
      clip.w += sin(clip.y * 0.13 * sin(worldTicks * 0.02834472) + worldTicks * 0.023482934) * 0.1 * inf5 * pMul;
      clip.w += sin(clip.x * 0.124 * sin(worldTicks * 0.00184298) + worldTicks * 0.018394082) * 0.05 * inf6 * pMul;
    }
  }
  return clip;
}

vec3 psychedelicraft_broad_waves(vec3 position, float scale) {
  vec3 point = position / scale;
  float ticks = PS_WorldTicks;
  float small = sin(point.x * 0.068 + ticks * 0.046) * sin(point.z * 0.063 + ticks * 0.033);
  float wiggle = sin(point.y * 0.073 + ticks * 0.028) * sin(point.z * 0.051 + ticks * 0.047);
  float big = sin(dot(point.xz, vec2(0.029, 0.022)) + ticks * 0.017);
  float distant = sin(dot(point.xz, vec2(-0.024, 0.044)) + ticks * 0.022);
  return scale * vec3(4.0 * wiggle * PS_WavesMatrix.z + 5.0 * big * PS_WavesMatrix.y,
                      5.0 * small * PS_WavesMatrix.x + 9.0 * big * PS_WavesMatrix.y
                          + 8.0 * distant * PS_DistantWorldDeformation, 0.0);
}

float psychedelicraft_peyote_height(vec2 position, float scale) {
  vec2 point = position / scale;
  float rise = sin(dot(point, vec2(0.052, 0.019)) - PS_WorldTicks * 0.023);
  float swell = sin(dot(point, vec2(-0.020, 0.038)) + PS_WorldTicks * 0.017);
  return scale * (3.0 * rise + 1.5 * swell);
}

vec3 psychedelicraft_world_offset(vec3 position, float scale, float broadBlend) {
  vec3 offset = vec3(0.0);
  if (broadBlend > 0.0) {
    offset = (psychedelicraft_broad_waves(position, scale) - psychedelicraft_broad_waves(PS_PlayerPosition, scale))
             * broadBlend;
  }
  if (PS_PeyoteStrength > 0.0) {
    offset.y += (psychedelicraft_peyote_height(position.xz, scale) - psychedelicraft_peyote_height(PS_PlayerPosition.xz, scale))
                * PS_PeyoteStrength;
  }
  return offset;
}

vec4 psychedelicraft_deform(vec4 clip, vec3 position, float viewDistance, float horizontalDistance) {
  bool movement = any(greaterThan(PS_WavesMatrix, vec3(0.0))) || PS_DistantWorldDeformation > 0.0;
  float broadBlend = 0.0;
  float coarseBlend = 0.0;
  float horizonBlend = 0.0;
  if (PS_NativeTerrainRadius > 0.0) {
    // Use the same spatial bands on both sides of the handoff, including during chunk fades.
    float transitionEnd = max(48.0, PS_NativeTerrainRadius - 16.0);
    if (movement) broadBlend = smoothstep(max(16.0, transitionEnd - 48.0), transitionEnd, horizontalDistance);
    coarseBlend = smoothstep(max(160.0, transitionEnd), max(384.0, transitionEnd + 192.0), horizontalDistance);
    horizonBlend = smoothstep(max(672.0, transitionEnd + 192.0), max(1536.0, transitionEnd + 576.0), horizontalDistance);
  }

  vec2 fracture = psychedelicraft_fracture(position);
  if (broadBlend < 1.0) {
    vec4 legacy = psychedelicraft_legacy_waves(clip, position, viewDistance, fracture);
    clip = mix(legacy, clip + vec4(fracture, 0.0, 0.0), broadBlend);
  } else {
    clip.xy += fracture;
  }

  if (broadBlend > 0.0 || PS_PeyoteStrength > 0.0) {
    vec3 offset;
    if (horizonBlend > 0.0) {
      offset = psychedelicraft_world_offset(position, 16.0, broadBlend);
      if (horizonBlend < 1.0) offset = mix(psychedelicraft_world_offset(position, 4.0, broadBlend), offset, horizonBlend);
    } else if (coarseBlend > 0.0) {
      offset = psychedelicraft_world_offset(position, 4.0, broadBlend);
      if (coarseBlend < 1.0) offset = mix(psychedelicraft_world_offset(position, 1.0, broadBlend), offset, coarseBlend);
    } else {
      offset = psychedelicraft_world_offset(position, 1.0, broadBlend);
    }
    clip += PS_WorldToClip * vec4(offset, 0.0);
  }
  return clip;
}
