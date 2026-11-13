#version 120

uniform sampler2D DiffuseSampler;
uniform sampler2D NoiseSampler;
uniform vec2 TextureScale;
uniform float Ticks;
uniform float Wetness;
varying vec2 texCoord;

void main() {
    vec2 uv = texCoord / TextureScale;
    vec3 noiseA = texture2D(NoiseSampler, fract(uv + vec2(0.0, Ticks * 0.005))).rgb;
    vec3 noiseB = texture2D(NoiseSampler, fract(uv + vec2(0.5, Ticks * 0.007))).rgb;
    vec2 displacement = clamp(noiseA.rg + noiseB.rg - 1.0, 0.0, 1.0);
    displacement *= mix(vec2(1.0), abs(noiseA.rg - 0.5) * 2.0, noiseA.b);
    displacement *= mix(vec2(1.0), abs(noiseB.rg - 0.5) * 2.0, noiseB.b);
    vec4 original = texture2D(DiffuseSampler, texCoord);
    vec4 distorted = texture2D(DiffuseSampler, clamp(uv + displacement * Wetness * 0.2, 0.0, 1.0) * TextureScale);
    gl_FragColor = mix(original, distorted, Wetness);
}
