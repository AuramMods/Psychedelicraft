#version 150

vec3 getIntensifiedColor(vec3 color) {
    float luma = dot(color, vec3(0.2126, 0.7152, 0.0722));
    float lowest = min(color.r, min(color.g, color.b));
    float highest = max(color.r, max(color.g, color.b));
    float darkLimit = luma / max(luma - lowest, 0.0001);
    float lightLimit = (1.0 - luma) / max(highest - luma, 0.0001);
    // Use part of the available chroma range so highlights and shadows retain detail.
    float extraChroma = clamp((min(darkLimit, lightLimit) - 1.0) * 0.85, 0.0, 0.55);
    return color + (color - vec3(luma)) * extraChroma;
}
