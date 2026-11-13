#version 120

uniform sampler2D DiffuseSampler;
uniform vec2 TextureScale;
uniform vec2 Direction;
uniform vec3 BloomColor;
uniform float TotalAlpha;
varying vec2 texCoord;

float influence(vec2 offset) {
    vec3 distance = abs(texture2D(DiffuseSampler, clamp(texCoord + offset, vec2(0.0), TextureScale)).rgb - BloomColor);
    return clamp(1.0 - (distance.r + distance.g + distance.b) * 2.0, 0.0, 1.0);
}

void main() {
    vec4 texel = texture2D(DiffuseSampler, texCoord);
    float weight = 0.0;
    for (int side = -1; side <= 1; side += 2) {
        vec2 step = Direction * float(side);
        weight += influence(step) * 0.056;
        weight += influence(step * 2.0) * 0.040;
        weight += influence(step * 3.0) * 0.032;
        weight += influence(step * 4.0) * 0.024;
    }
    vec3 bloom = mix(texel.rgb, BloomColor, clamp(weight, 0.0, 1.0));
    gl_FragColor = vec4(mix(texel.rgb, bloom, TotalAlpha), texel.a);
}
