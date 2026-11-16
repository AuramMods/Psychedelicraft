#version 150


vec3 getRotatedColor(vec3 color, float rot) {
	vec3 returnColor = vec3(0);

	for (int i = 0; i < 3; i++) {
        float colorAffected = mod(float(i) + rot * 3.0, 3.0);

        int col1 = int(floor(colorAffected));

        returnColor[col1] += color[i] * (1.0 - (colorAffected - float(col1)));
        returnColor[int(mod(float(col1 + 1), 3.0))] += color[i] * (colorAffected - float(col1));
	}

	return returnColor;
}




vec3 getIntensifiedColor(vec3 color) {
    float s = 2.0;
    float cR = 0.3086;
    float cG = 0.6084;
    float cB = 0.0820;

    float rr = (1.0 - s) * cR + s;
    float rg = (1.0 - s) * cG;
    float rb = (1.0 - s) * cB;
    float gr = (1.0 - s) * cR;
    float gg = (1.0 - s) * cG + s;
    float gb = (1.0 - s) * cB;
    float br = (1.0 - s) * cR;
    float bg = (1.0 - s) * cG;
    float bb = (1.0 - s) * cB + s;

    vec3 rVec = vec3(color.r * rr + color.g * rg + color.b * rb, color.r * gr + color.g * gg + color.b * gb, color.r * br + color.g * bg + color.b * bb);

    return clamp(rVec * rVec * 10.0, 0.0, 1.0);
}



float getBrightness(vec3 color) {
    float cR = 0.3086;
    float cG = 0.6084;
    float cB = 0.0820;

    return (color.r * cR + color.g * cG + color.b * cB);
}

vec3 getDesaturatedColor(vec3 color) {
    return vec3(getBrightness(color));
}



vec3 getInvertedColor(vec3 color) {
    return vec3(1, 1, 1) - color;
}


uniform sampler2D DiffuseSampler;

in vec2 texCoord;

uniform float ticks;

uniform float slowColorRotation;
uniform float quickColorRotation;
uniform float colorIntensification;
uniform float desaturation;
uniform float inversion;

out vec4 fragColor;

void main() {
  vec4 texel = texture(DiffuseSampler, texCoord);
  vec3 outcolor = texel.rgb;

  if (slowColorRotation > 0.0) {
    outcolor = mix(outcolor, getRotatedColor(outcolor, mod(ticks, 300.0) / 300.0), slowColorRotation / 2.0);
  }

  if (quickColorRotation > 0.0) {
     outcolor = mix(outcolor, getRotatedColor(outcolor, mod(ticks/* + fogFragCoord[0]*/, 50.0) / 50.0), clamp(quickColorRotation * 1.5, 0.0, 1.0));
  }

  if (colorIntensification != 0.0) {
    outcolor = mix(outcolor, getIntensifiedColor(outcolor), colorIntensification);
  }

  if (desaturation != 0.0) {
    outcolor = mix(outcolor, getDesaturatedColor(outcolor), desaturation);
  }

  if (inversion > 0) {
    outcolor = mix(outcolor, getInvertedColor(outcolor), inversion);
  }

  fragColor = vec4(outcolor, texel.a);
}
