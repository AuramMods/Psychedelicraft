#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform vec2 pixelSize;
uniform float vertical;
uniform float focalPointNear;
uniform float focalPointFar;
uniform float focalBlurNear;
uniform float focalBlurFar;
uniform vec2 ProjectionDepth;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 scene = texture(DiffuseSampler, texCoord);
    float clipDepth = texture(DepthSampler, texCoord).r * 2.0 - 1.0;
    float distance = ProjectionDepth.y / (clipDepth + ProjectionDepth.x);
    float blur = 0.0;
    if (distance < focalPointNear) blur = (focalPointNear - distance) * focalBlurNear / focalPointNear;
    else if (distance > focalPointFar) blur = (distance - focalPointFar) * focalBlurFar / max(focalPointFar, 0.0001);
    vec3 result = scene.rgb;
    if (blur > 0.0) {
        vec2 direction = vertical == 0.0 ? vec2(pixelSize.x, 0.0) : vec2(0.0, pixelSize.y);
        vec3 samples = scene.rgb * 0.2;
        for (int side = -1; side <= 1; side += 2) {
            vec2 step = direction * float(side);
            samples += texture(DiffuseSampler, clamp(texCoord + step, vec2(0.0), vec2(1.0))).rgb * 0.15;
            samples += texture(DiffuseSampler, clamp(texCoord + 2.0 * step, vec2(0.0), vec2(1.0))).rgb * 0.11;
            samples += texture(DiffuseSampler, clamp(texCoord + 3.0 * step, vec2(0.0), vec2(1.0))).rgb * 0.09;
            samples += texture(DiffuseSampler, clamp(texCoord + 4.0 * step, vec2(0.0), vec2(1.0))).rgb * 0.05;
        }
        result = mix(scene.rgb, samples, min(blur, 1.0));
    }
    fragColor = vec4(result, scene.a);
}
