uniform sampler2D DepthSampler;
uniform sampler2D PS_DepthNoHand;
uniform sampler2D PS_DepthWithHand;
uniform sampler2D PS_DistantDepth;
uniform sampler2D PS_DistantOpaqueDepth;
uniform float PS_HasHandMask;
uniform float PS_DistantDepthMode;
uniform vec2 PS_NearProjection;
uniform vec4 PS_DistantProjections;

float psychedelicraft_near_depth(vec2 uv) {
    float depth = texture(DepthSampler, uv).r;
    if (PS_HasHandMask <= 0.0) return depth;
    float beforeHand = texture(PS_DepthNoHand, uv).r;
    float withHand = texture(PS_DepthWithHand, uv).r;
    // Preserve a nearer world translucent surface drawn after the solid hand.
    return withHand < beforeHand && depth >= withHand ? beforeHand : depth;
}

float psychedelicraft_depth_distance(float depth, vec2 projection) {
    return projection.y / (2.0 * depth - 1.0 + projection.x);
}

float psychedelicraft_distant_distance(vec2 uv) {
    if (PS_DistantDepthMode <= 0.0) return -1.0;
    float depth = texture(PS_DistantDepth, uv).r;
    if (depth >= 1.0) return -1.0;
    vec2 projection = PS_DistantProjections.xy;
    if (PS_DistantDepthMode > 1.0 && abs(depth - texture(PS_DistantOpaqueDepth, uv).r) > 0.00000012) {
        projection = PS_DistantProjections.zw;
    }
    return psychedelicraft_depth_distance(depth, projection);
}

float psychedelicraft_scene_depth(vec2 uv) {
    float nearDepth = psychedelicraft_near_depth(uv);
    float distant = psychedelicraft_distant_distance(uv);
    if (distant <= 0.0 || (nearDepth < 1.0 && psychedelicraft_depth_distance(nearDepth, PS_NearProjection) <= distant)) {
        return nearDepth;
    }
    return clamp((PS_NearProjection.y / distant - PS_NearProjection.x + 1.0) * 0.5, 0.0, 1.0);
}

float psychedelicraft_scene_distance(vec2 uv) {
    float nearDepth = psychedelicraft_near_depth(uv);
    float nearDistance = psychedelicraft_depth_distance(nearDepth, PS_NearProjection);
    float distant = psychedelicraft_distant_distance(uv);
    if (distant <= 0.0) return nearDistance;
    return nearDepth < 1.0 ? min(nearDistance, distant) : distant;
}
