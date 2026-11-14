#version 120

uniform sampler2D ScreenSampler;
uniform sampler2D EndPortalSampler;
uniform float GameTime;
uniform float SurfaceFractalStrength;
uniform float WorldTicks;
uniform vec4 Pulses;

varying vec4 screenProjection;
varying vec4 vertexColor;
varying vec3 surfaceColor;

const mat4 SCALE_TRANSLATE = mat4(
    0.5, 0.0, 0.0, 0.25,
    0.0, 0.5, 0.0, 0.25,
    0.0, 0.0, 1.0, 0.0,
    0.0, 0.0, 0.0, 1.0
);

mat4 portalLayer(float layer) {
    mat4 shift = mat4(
        1.0, 0.0, 0.0, 17.0 / layer,
        0.0, 1.0, 0.0, (2.0 + layer / 1.5) * (GameTime * 1.5),
        0.0, 0.0, 1.0, 0.0,
        0.0, 0.0, 0.0, 1.0
    );
    float angle = radians((layer * layer * 4321.0 + layer * 9.0) * 2.0);
    mat2 rotation = mat2(cos(angle), sin(angle), -sin(angle), cos(angle));
    return mat4(rotation * ((4.5 - layer / 4.0) * 2.0)) * shift * SCALE_TRANSLATE;
}

void main() {
    vec4 scale = vec4(1.25, 1.25, 0.25, 0.25);
    vec3 color = texture2DProj(ScreenSampler, screenProjection * scale).rgb;
    for (int i = 1; i <= 15; ++i) {
        color += texture2DProj(EndPortalSampler, screenProjection * portalLayer(float(i)) * scale).rgb;
    }
    color *= mix(vertexColor.rgb, surfaceColor, SurfaceFractalStrength);
    // The donor Zero matter vertex shader supplies no distance to the pulse.
    float pulse = max(0.0, sin(-WorldTicks / 5.0) - 0.4) * Pulses.a;
    gl_FragColor = vec4(mix(color, (color + 1.0) * Pulses.rgb, pulse), vertexColor.a);
}
