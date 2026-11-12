#version 120

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform sampler2D GlyphSampler;
uniform vec2 TextureScale;
uniform vec2 Resolution;
uniform float TextProgress;
uniform float MaxColors;
uniform float Saturation;
uniform float ViewDistance;
varying vec2 texCoord;

void main() {
    vec2 uv = texCoord / TextureScale;
    vec2 pixel = ceil(uv * Resolution) / Resolution;
    vec4 scene = texture2D(DiffuseSampler, pixel * TextureScale);
    float gray = dot(scene.rgb, vec3(0.3086, 0.6084, 0.0820));
    scene.rgb = mix(vec3(gray), scene.rgb, Saturation);
    if (MaxColors >= 0.0) scene = ceil(scene * MaxColors - 0.5) / MaxColors;

    if (TextProgress > 0.0) {
        float progress = min(TextProgress, 1.0);
        float background = sqrt(1.0 - clamp(progress - 0.2, 0.0, 0.8) * 1.25);
        float random = fract(sin(dot(pixel, vec2(12.9898, 78.233))) * 43758.5453) * 0.999;
        if (random < progress || background < 1.0) {
            float depth = texture2D(DepthSampler, pixel * TextureScale).r;
            float density = 0.1 / (ViewDistance + 0.05 - depth * (ViewDistance - 0.05));
            bool binary = random < TextProgress - progress;
            float glyph = binary ? min(ceil(density - 0.25), 1.0) : ceil(density * 94.0 - 0.5 / 95.0);
            vec2 inner = (pixel - uv) * Resolution;
            vec2 glyphUV = vec2((glyph + 1.0 - inner.x) / 95.0, inner.y * 0.5 + (binary ? 0.5 : 0.0));
            vec3 mask = texture2D(GlyphSampler, glyphUV).rgb;
            scene.rgb = mix(scene.rgb, scene.rgb * mask, max(progress * progress * progress, 1.0 - background));
        }
    }
    gl_FragColor = scene;
}
