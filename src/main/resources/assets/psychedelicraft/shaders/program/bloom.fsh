#version 120

uniform sampler2D DiffuseSampler;
uniform vec2 TextureScale;
uniform vec2 Direction;
uniform float TotalAlpha;
varying vec2 texCoord;

void main() {
    vec4 scene = texture2D(DiffuseSampler, texCoord);
    float influence = 15.0 / (5.0 + scene.r + scene.g + scene.b);
    influence *= influence;
    vec3 glow = vec3(0.0);

    for (int side = -1; side <= 1; side += 2) {
        vec2 step = Direction * (3.0 * float(side));
        glow += texture2D(DiffuseSampler, clamp(texCoord + step, vec2(0.0), TextureScale)).rgb * 0.028 * influence;
        glow += texture2D(DiffuseSampler, clamp(texCoord + 2.0 * step, vec2(0.0), TextureScale)).rgb * 0.020 * influence;
        glow += texture2D(DiffuseSampler, clamp(texCoord + 3.0 * step, vec2(0.0), TextureScale)).rgb * 0.016 * influence;
        glow += texture2D(DiffuseSampler, clamp(texCoord + 4.0 * step, vec2(0.0), TextureScale)).rgb * 0.012 * influence;
    }
    gl_FragColor = vec4(scene.rgb + glow * glow * glow * TotalAlpha, scene.a);
}
