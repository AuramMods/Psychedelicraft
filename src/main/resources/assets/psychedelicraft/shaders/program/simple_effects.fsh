#version 120

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform float Ticks;
uniform vec4 ColorStrengths;
uniform float Inversion;
uniform vec4 WorldColor;
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

vec3 harmonize(vec3 color, float phase) {
    vec3 distance = abs(color - WorldColor.rgb);
    float strength = 3.0 * pow(clamp(distance.r + distance.g + distance.b, 0.0, 1.0), 16.0);
    strength += (sin(phase * 0.1434234) - 0.4) * 0.8;
    strength += (sin(phase * -0.12313) - 0.2) * 0.8;
    strength += sin(phase * -0.051233) * 0.2 * sin(Ticks * 0.1321334);
    strength = clamp(strength, 0.0, 3.0);
    // The animated palette can exceed white. Reflect its complement at zero so
    // a peak on all three channels cannot turn the contrast pass solid black.
    vec3 complement = abs(vec3(1.0) - WorldColor.rgb);
    vec3 tint = strength < 1.0 ? mix(WorldColor.rgb, vec3(0.5), strength)
            : mix(vec3(0.5), complement, (strength - 1.0) * 0.5);
    return mix(color, tint, WorldColor.a);
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
    if (Inversion > 0.0) {
        color = mix(color, vec3(1.0) - color, Inversion);
    }
    if (WorldColor.a > 0.0) {
        color = harmonize(color, texture2D(DepthSampler, texCoord).r - Ticks);
    }
    gl_FragColor = vec4(color, texel.a);
}
