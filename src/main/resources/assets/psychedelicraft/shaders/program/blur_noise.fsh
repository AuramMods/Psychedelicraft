#version 120

uniform sampler2D DiffuseSampler;
uniform vec2 TextureScale;
uniform vec2 PixelSize;
uniform float Seed;
uniform float Strength;
varying vec2 texCoord;

float randomFromVec(vec2 value) {
    return fract(sin(dot(value, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec2 uv = floor(texCoord / (TextureScale * PixelSize)) * PixelSize;
    float jitter = fract(mod(Seed * 12374.123814, 18034.805912)) - 0.5;
    uv.y += jitter * Strength * 0.04;

    vec4 scene = texture2D(DiffuseSampler, clamp(uv, 0.0, 1.0) * TextureScale);
    vec3 result = scene.rgb;
    for (float offset = -Strength * 40.0; offset < Strength * 40.0 + 0.5; offset += 1.0) {
        vec2 sampleUV = uv + vec2(0.0, offset * PixelSize.y);
        if (offset != 0.0 && sampleUV.y > 0.0 && sampleUV.y < 1.0
                && randomFromVec(sampleUV + vec2(0.0, Seed)) < Strength * 0.01) {
            vec3 sampleColor = texture2D(DiffuseSampler, sampleUV * TextureScale).rgb;
            result = mix(result, sampleColor, 1.0 / (offset * offset * 0.004 + 1.0));
        }
    }
    gl_FragColor = vec4(result, scene.a);
}
