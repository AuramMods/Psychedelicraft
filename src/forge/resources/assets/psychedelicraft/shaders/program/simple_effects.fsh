#version 120

uniform sampler2D DiffuseSampler;
uniform float Ticks;
uniform vec4 ColorStrengths;
varying vec2 texCoord;

vec3 rotateColor(vec3 color, float phase) {
    float offset = phase * 3.0;
    if (offset < 1.0) {
        return mix(color, color.brg, offset);
    }
    if (offset < 2.0) {
        return mix(color.brg, color.gbr, offset - 1.0);
    }
    return mix(color.gbr, color, offset - 2.0);
}

float brightness(vec3 color) {
    return dot(color, vec3(0.3086, 0.6084, 0.0820));
}

void main() {
    vec4 texel = texture2D(DiffuseSampler, texCoord);
    vec3 color = texel.rgb;
    if (ColorStrengths.x > 0.0) {
        color = mix(color, rotateColor(color, mod(Ticks, 300.0) / 300.0), ColorStrengths.x / 2.0);
    }
    if (ColorStrengths.y > 0.0) {
        color = mix(color, rotateColor(color, mod(Ticks, 50.0) / 50.0), clamp(ColorStrengths.y * 1.5, 0.0, 1.0));
    }
    if (ColorStrengths.z > 0.0) {
        vec3 intense = color * 2.0 - vec3(brightness(color));
        color = mix(color, clamp(intense * intense * 10.0, 0.0, 1.0), ColorStrengths.z);
    }
    if (ColorStrengths.w > 0.0) {
        color = mix(color, vec3(brightness(color)), ColorStrengths.w);
    }
    gl_FragColor = vec4(color, texel.a);
}
