#version 120

uniform sampler2D DiffuseSampler;
uniform vec2 TextureScale;
uniform vec2 PixelSize;
uniform vec2 BlurStrength;
varying vec2 texCoord;

vec3 blurredSamples(vec2 direction) {
    vec3 samples = vec3(0.0);
    for (int side = -1; side <= 1; side += 2) {
        vec2 step = direction * float(side);
        samples += texture2D(DiffuseSampler, clamp(texCoord + step, vec2(0.0), TextureScale)).rgb * 0.15;
        samples += texture2D(DiffuseSampler, clamp(texCoord + 2.0 * step, vec2(0.0), TextureScale)).rgb * 0.11;
        samples += texture2D(DiffuseSampler, clamp(texCoord + 3.0 * step, vec2(0.0), TextureScale)).rgb * 0.09;
        samples += texture2D(DiffuseSampler, clamp(texCoord + 4.0 * step, vec2(0.0), TextureScale)).rgb * 0.05;
    }
    return samples;
}

void main() {
    vec4 scene = texture2D(DiffuseSampler, texCoord);
    vec3 result = scene.rgb;
    vec3 horizontal = vec3(0.0);
    vec3 vertical = vec3(0.0);
    if (BlurStrength.x > 0.0) horizontal = blurredSamples(vec2(PixelSize.x * BlurStrength.x, 0.0));
    if (BlurStrength.y > 0.0) vertical = blurredSamples(vec2(0.0, PixelSize.y * BlurStrength.y));

    float repeats = ceil(max(BlurStrength.x, BlurStrength.y));
    for (float pass = 0.0; pass < repeats; pass += 1.0) {
        vec2 activeBlur = clamp(BlurStrength - pass, 0.0, 1.0);
        vec3 hBlur = mix(result, result * 0.2 + horizontal, activeBlur.x);
        vec3 vBlur = mix(result, result * 0.2 + vertical, activeBlur.y);
        if (activeBlur.x > 0.0 && activeBlur.y > 0.0) result = (hBlur + vBlur) * 0.5;
        else if (activeBlur.x > 0.0) result = hBlur;
        else result = vBlur;
    }
    gl_FragColor = vec4(result, scene.a);
}
