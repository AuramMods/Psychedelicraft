#version 120

uniform sampler2D DiffuseSampler;
uniform vec2 TextureScale;
uniform float Strength;
uniform float Distance;
varying vec2 texCoord;

void main() {
    vec4 scene = texture2D(DiffuseSampler, texCoord);
    float center = 0.5 + (texCoord.x / TextureScale.x - 0.5) / (1.0 + Strength);
    vec2 left = vec2((center - Distance) * TextureScale.x, texCoord.y);
    vec2 right = vec2((center + Distance) * TextureScale.x, texCoord.y);
    vec3 ghost = scene.rgb * 0.35;
    ghost += texture2D(DiffuseSampler, clamp(left, vec2(0.0), TextureScale)).rgb * 0.325;
    ghost += texture2D(DiffuseSampler, clamp(right, vec2(0.0), TextureScale)).rgb * 0.325;
    gl_FragColor = vec4(mix(scene.rgb, ghost, Strength), scene.a);
}
