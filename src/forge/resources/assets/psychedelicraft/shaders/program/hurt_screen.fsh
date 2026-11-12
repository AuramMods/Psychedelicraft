#version 120

uniform sampler2D DiffuseSampler;
uniform sampler2D OverlaySampler;
uniform vec2 TextureScale;
uniform vec2 OverlayScale;
uniform float Opacity;
varying vec2 texCoord;

void main() {
    vec2 uv = texCoord / TextureScale;
    vec2 overlayUv = (vec2(uv.x, 1.0 - uv.y) - 0.5) * OverlayScale + 0.5;
    vec4 scene = texture2D(DiffuseSampler, texCoord);
    vec4 overlay = texture2D(OverlaySampler, overlayUv);
    gl_FragColor = vec4(mix(scene.rgb, overlay.rgb, overlay.a * Opacity), scene.a);
}
