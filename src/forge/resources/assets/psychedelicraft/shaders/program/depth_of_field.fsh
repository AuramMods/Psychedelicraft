#version 120

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform vec2 TextureScale;
uniform vec2 Direction;
uniform vec2 ProjectionDepth;
uniform vec2 FocusDistance;
uniform vec2 BlurStrength;
varying vec2 texCoord;

void main() {
    vec4 scene = texture2D(DiffuseSampler, texCoord);
    float clipDepth = texture2D(DepthSampler, texCoord).r * 2.0 - 1.0;
    float distance = ProjectionDepth.y / (clipDepth + ProjectionDepth.x);
    float blur = 0.0;
    if (distance < FocusDistance.x) {
        blur = (FocusDistance.x - distance) * BlurStrength.x / FocusDistance.x;
    } else if (distance > FocusDistance.y) {
        blur = (distance - FocusDistance.y) * BlurStrength.y / max(FocusDistance.y, 0.0001);
    }

    vec3 result = scene.rgb;
    if (blur > 0.0) {
        vec3 blurred = scene.rgb * 0.2;
        for (int side = -1; side <= 1; side += 2) {
            vec2 step = Direction * float(side);
            blurred += texture2D(DiffuseSampler, clamp(texCoord + step, vec2(0.0), TextureScale)).rgb * 0.15;
            blurred += texture2D(DiffuseSampler, clamp(texCoord + 2.0 * step, vec2(0.0), TextureScale)).rgb * 0.11;
            blurred += texture2D(DiffuseSampler, clamp(texCoord + 3.0 * step, vec2(0.0), TextureScale)).rgb * 0.09;
            blurred += texture2D(DiffuseSampler, clamp(texCoord + 4.0 * step, vec2(0.0), TextureScale)).rgb * 0.05;
        }
        result = mix(scene.rgb, blurred, min(blur, 1.0));
    }
    gl_FragColor = vec4(result, scene.a);
}
