#version 120

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform sampler2D NoiseSampler;
uniform vec2 TextureScale;
uniform float Ticks;
uniform float Strength;
varying vec2 texCoord;

void main() {
    vec2 uv = texCoord / TextureScale;
    vec2 noiseA = texture2D(NoiseSampler, fract(uv * 4.0 + Ticks * vec2(0.324823048, 0.48913801))).rg;
    vec2 noiseB = texture2D(NoiseSampler, fract(uv.yx * 4.0 + Ticks * vec2(0.52890348, 0.6318212))).rg;
    float depth = texture2D(DepthSampler, texCoord).r;
    float depthFactor = min(0.4 / sqrt(sqrt(sqrt(max(1.0 - depth, 0.000001)))) - 0.4, 1.0);
    vec2 displacement = (noiseA + noiseB - 1.0) * Strength * depthFactor;
    vec3 distorted = texture2D(DiffuseSampler, clamp(uv + displacement, 0.0, 1.0) * TextureScale).rgb;
    gl_FragColor = vec4(distorted, texture2D(DiffuseSampler, texCoord).a);
}
