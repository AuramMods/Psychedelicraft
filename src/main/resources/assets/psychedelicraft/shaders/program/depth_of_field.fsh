#version 150

uniform sampler2D DiffuseSampler;
#moj_import <psychedelicraft:scene_depth.glsl>
uniform vec2 pixelSize;
uniform float vertical;
uniform float focalPointNear;
uniform float focalPointFar;
uniform float focalBlurNear;
uniform float focalBlurFar;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 scene = texture(DiffuseSampler, texCoord);
    float distance = psychedelicraft_scene_distance(texCoord);
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
