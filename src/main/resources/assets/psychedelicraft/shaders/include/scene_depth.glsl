uniform sampler2D DepthSampler;
uniform sampler2D PS_DepthNoHand;
uniform sampler2D PS_DepthWithHand;
uniform float PS_HasHandMask;

float psychedelicraft_scene_depth(vec2 uv) {
    float depth = texture(DepthSampler, uv).r;
    if (PS_HasHandMask <= 0.0) return depth;
    float beforeHand = texture(PS_DepthNoHand, uv).r;
    float withHand = texture(PS_DepthWithHand, uv).r;
    // Preserve a nearer world translucent surface drawn after the solid hand.
    return withHand < beforeHand && depth >= withHand ? beforeHand : depth;
}
