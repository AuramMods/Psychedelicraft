/* Original wave equations: Lukas Tenbrink / Sollace. */
uniform float PS_WorldTicks;
uniform vec3 PS_WavesMatrix;
uniform float PS_DistantWorldDeformation;
uniform float PS_FractalFractureStrength;
uniform float PS_PeyoteStrength;
uniform vec4 PS_WorldUpClip;
uniform vec3 PS_PlayerPosition;
float psychedelicraft_gold_noise(vec2 xy, float seed) {
  const float phi = 1.61803398874989484820459;
  return fract(tan(distance(xy * phi, xy) * seed) * xy.x);
}

float psychedelicraft_peyote_height(vec2 position, float ticks) {
  float rise = sin(dot(position, vec2(0.052, 0.019)) - ticks * 0.023);
  float swell = sin(dot(position, vec2(-0.020, 0.038)) + ticks * 0.017);
  return 3.0 * rise + 1.5 * swell;
}

// The legacy distance-based waves measure camera-space distance before projection.
vec4 psychedelicraft_deform(vec4 clip, vec3 position, float viewDistance) {
  float worldTicks = PS_WorldTicks;
  float smallWaves = PS_WavesMatrix.x;
  float bigWaves = PS_WavesMatrix.y;
  float wiggleWaves = PS_WavesMatrix.z;
  float distantWorldDeformation = PS_DistantWorldDeformation;
  float fractalFractureStrength = PS_FractalFractureStrength;
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


  if (fractalFractureStrength > 0) {
    float fractalScollSpeed = worldTicks / 100;
    clip.x += sin(psychedelicraft_gold_noise(position.xy, position.x + fractalScollSpeed)) * fractalFractureStrength;
    clip.y += sin(psychedelicraft_gold_noise(position.xy, position.y + fractalScollSpeed)) * fractalFractureStrength;
  }

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
  if (PS_PeyoteStrength > 0.0) {
    float height = psychedelicraft_peyote_height(position.xz, worldTicks)
                 - psychedelicraft_peyote_height(playerPos.xz, worldTicks);
    clip += PS_WorldUpClip * (height * PS_PeyoteStrength);
  }
  return clip;
}
