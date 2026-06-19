#version 150

// === COSMIC UV DISTORTION ===
// Simplex-style noise warps the entire screen — reality bending.
// Stronger in center of screen vs edges (gravitational lensing feel).

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Strength;
uniform float Time;

in vec2 texCoord;
out vec4 fragColor;

// === Simplex-style hash noise ===
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i + vec2(0.0, 0.0)), hash(i + vec2(1.0, 0.0)), u.x),
        mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x),
        u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
        v += amp * noise(p);
        p *= 2.0;
        amp *= 0.5;
    }
    return v;
}

void main() {
    vec2 center = vec2(0.5, 0.5);
    vec2 dir = texCoord - center;
    float dist = length(dir);

    // Gravitational lensing — pull toward center proportional to dist
    float lensFactor = exp(-dist * 3.0) * Strength;

    // Animated noise warp
    vec2 noisePos = texCoord * 4.0 + Time * 0.3;
    float warp = fbm(noisePos) - 0.5;
    vec2 warpVec = vec2(
        warp,
        fbm(noisePos + vec2(7.3, 13.7)) - 0.5
    ) * Strength * 1.5;

    // Combined distortion
    vec2 finalUV = texCoord - dir * lensFactor + warpVec;

    fragColor = texture(DiffuseSampler, finalUV);
}
