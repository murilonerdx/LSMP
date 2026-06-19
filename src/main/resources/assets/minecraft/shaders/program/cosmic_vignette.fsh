#version 150

// === COSMIC VIGNETTE + COLOR GRADING ===
// Darkens edges + tints whole screen with cosmic purple (Tint uniforms).
// Bottom of cosmic horror post-chain.

uniform sampler2D DiffuseSampler;
uniform float Strength;
uniform float TintR;
uniform float TintG;
uniform float TintB;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);

    // Vignette — radial darken
    vec2 center = vec2(0.5, 0.5);
    float dist = length(texCoord - center) * 1.414;
    float vignette = 1.0 - smoothstep(0.3, 1.1, dist) * Strength;
    color.rgb *= vignette;

    // Cosmic tint — multiply blend with purple
    vec3 tint = vec3(TintR, TintG, TintB);
    color.rgb = mix(color.rgb, color.rgb * (0.7 + tint * 2.0), Strength * 0.5);

    // Slight desaturation in dark areas (further sense of dread)
    float luma = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    color.rgb = mix(vec3(luma) * tint, color.rgb, 0.7 + luma * 0.3);

    fragColor = color;
}
