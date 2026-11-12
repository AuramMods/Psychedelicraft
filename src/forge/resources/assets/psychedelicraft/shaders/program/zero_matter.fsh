#version 120

uniform sampler2D ScreenSampler;
uniform sampler2D PortalSampler;
uniform float GameTime;

varying vec4 screenProjection;
varying vec4 vertexColor;

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
        color += texture2DProj(PortalSampler, screenProjection * portalLayer(float(i)) * scale).rgb;
    }
    gl_FragColor = vec4(color, 1.0) * vertexColor;
}
