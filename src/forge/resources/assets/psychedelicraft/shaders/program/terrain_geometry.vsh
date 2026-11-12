#version 120

uniform sampler2D AtlasSampler;
uniform float WorldTicks;
uniform vec3 Waves;
uniform vec2 PlayerXZ;
uniform float DistantDeformation;
uniform float FractureStrength;
uniform float SurfaceFractalStrength;
uniform vec4 SurfaceFractalCoords;

varying vec2 atlasUv;
varying vec2 lightmapUv;
varying vec4 vertexColor;
varying vec3 viewPosition;
varying vec3 surfaceColor;

float goldNoise(vec2 xy, float seed) {
  const float phi = 1.61803398874989484820459;
  return fract(tan(distance(xy * phi, xy) * seed) * xy.x);
}

void main() {
  vec3 position = gl_Vertex.xyz;
  vec4 view = gl_ModelViewMatrix * gl_Vertex;
  gl_Position = ftransform();
  viewPosition = view.xyz;
  atlasUv = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
  lightmapUv = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
  vertexColor = gl_Color;
  surfaceColor = vec3(0.0);
  if (SurfaceFractalStrength > 0.0) {
    vec2 portalUv = mod(position.xz + position.yy, 4.0) / 4.0;
    surfaceColor = texture2D(AtlasSampler, mix(SurfaceFractalCoords.xy, SurfaceFractalCoords.zw, portalUv)).rgb;
  }

  float worldTicks = WorldTicks;
  float smallWaves = Waves.x;
  float bigWaves = Waves.y;
  float wiggleWaves = Waves.z;
  float clipDistance = length(gl_Position);

  if (smallWaves > 0.0) {
    float w1 = 8.0;

    gl_Position[1] += sin((position.x + worldTicks / 5.0) / w1 * 3.14159 * 2.0) * sin((position.z + worldTicks / 5.0) / w1 * 3.14159 * 2.0) * smallWaves * 1.5;
    gl_Position[1] -= sin((PlayerXZ.x + worldTicks / 5.0) / w1 * 3.14159 * 2.0) * sin((PlayerXZ.y + worldTicks / 5.0) / w1 * 3.14159 * 2.0) * smallWaves * 1.5;

    float w2 = 16.0;

    gl_Position.y += sin((position.x + worldTicks / 8.0) / w2 * 3.14159 * 2.0) * sin((position.z) / w2 * 3.14159 * 2.0) * smallWaves * 3.0;
    gl_Position.y -= sin((PlayerXZ.x + worldTicks / 8.0) / w2 * 3.14159 * 2.0) * sin((PlayerXZ.y) / w2 * 3.14159 * 2.0) * smallWaves * 3.0;

    gl_Position.x = mix(gl_Position.x, gl_Position[0] * (1.0 + clipDistance / 20.0), smallWaves);
    gl_Position.y = mix(gl_Position.y, gl_Position[1] * (1.0 + clipDistance / 20.0), smallWaves);
  }

  if (FractureStrength > 0) {
    float fractureScroll = worldTicks / 100;
    gl_Position.x += sin(goldNoise(position.xy, position.x + fractureScroll)) * FractureStrength;
    gl_Position.y += sin(goldNoise(position.xy, position.y + fractureScroll)) * FractureStrength;
  }

  if (wiggleWaves > 0.0) {
    float w1 = 8.0;
    gl_Position.x += sin((position.y + worldTicks / 8.0) / w1 * 3.14159 * 2.0)
                    * sin((position.z + worldTicks / 5.0) / w1 * 3.14159 * 2.0)
                    * wiggleWaves;
  }

  if (DistantDeformation > 0.0 && clipDistance > 5.0) {
    gl_Position.y += (sin(clipDistance / 8.0 * 3.14159 * 2.0) + 1.0)
                     * DistantDeformation
                     * (clipDistance - 5.0) / 8.0;
  }

  if (bigWaves > 0.0) {
    if (gl_Position[2] > 0.1) {
      float dDist = (gl_Position[2] - 0.1) * bigWaves;
      if (gl_Position[2] > 20.0) {
        dDist = (20.0 - 0.1) * bigWaves + (gl_Position.z - 20.0) * bigWaves * 0.3;
      }

      float inf1 = sin(worldTicks * 0.0086465563) * dDist;
      float inf2 = cos(worldTicks * 0.0086465563) * dDist;
      float inf3 = sin(worldTicks * 0.0091033941) * dDist;
      float inf4 = cos(worldTicks * 0.0091033941) * dDist;
      float inf5 = sin(worldTicks * 0.0064566190) * dDist;
      float inf6 = cos(worldTicks * 0.0064566190) * dDist;

      float pMul = 1.3;

      gl_Position.x += sin(gl_Position.z * 0.1 * sin(worldTicks * 0.001849328) + worldTicks * 0.014123412) * 0.5 * inf1 * pMul;
      gl_Position.y += cos(gl_Position.z * 0.1 * sin(worldTicks * 0.001234728) + worldTicks * 0.017481893) * 0.4 * inf1 * pMul;

      gl_Position.x += sin(gl_Position.y * 0.1 * sin(worldTicks * 0.001523784) + worldTicks * 0.021823911) * 0.2 * inf2 * pMul;
      gl_Position.y += sin(gl_Position.x * 0.1 * sin(worldTicks * 0.001472387) + worldTicks * 0.023193141) * 0.08 * inf2 * pMul;

      gl_Position.x += sin(gl_Position.z * 0.15 * sin(worldTicks * 0.001284923) + worldTicks * 0.019404289) * 0.25 * inf3 * pMul;
      gl_Position.y += cos(gl_Position.z * 0.15 * sin(worldTicks * 0.001482938) + worldTicks * 0.018491238) * 0.15 * inf3 * pMul;

      gl_Position.x += sin(gl_Position.y * 0.05 * sin(worldTicks * 0.001283942) + worldTicks * 0.012942342) * 0.4 * inf4 * pMul;
      gl_Position.y += sin(gl_Position.x * 0.05 * sin(worldTicks * 0.001829482) + worldTicks * 0.012981328) * 0.35 * inf4 * pMul;

      gl_Position.z += sin(gl_Position.y * 0.13 * sin(worldTicks * 0.02834472) + worldTicks * 0.023482934) * 0.1 * inf5 * pMul;
      gl_Position.z += sin(gl_Position.x * 0.124 * sin(worldTicks * 0.00184298) + worldTicks * 0.018394082) * 0.05 * inf6 * pMul;
      gl_Position.w += sin(gl_Position.y * 0.13 * sin(worldTicks * 0.02834472) + worldTicks * 0.023482934) * 0.1 * inf5 * pMul;
      gl_Position.w += sin(gl_Position.x * 0.124 * sin(worldTicks * 0.00184298) + worldTicks * 0.018394082) * 0.05 * inf6 * pMul;
    }
  }
}
