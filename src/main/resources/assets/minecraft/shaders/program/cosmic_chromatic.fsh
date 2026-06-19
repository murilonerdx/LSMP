#version 150

// === COSMIC CHROMATIC ABERRATION ===
// RGB channel separation that intensifies toward screen edges.
// Simulates lens dispersion + dimensional reality break.

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Aberration;
uniform float Time;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

void main() {
    vec2 center = vec2(0.5, 0.5);
    vec2 dir = texCoord - center;
    float dist = length(dir);

    // Edge-weighted aberration (stronger nas bordas)
    float edgeFactor = smoothstep(0.0, 0.7, dist);
    float strength = Aberration * (0.3 + edgeFactor * 1.7);

    // Subtle time-based jitter
    float jitter = sin(Time * 7.3) * 0.001;
    strength += jitter;

    // Three samples — R shifted forward, G center, B shifted back
    vec2 shift = normalize(dir + 0.0001) * strength;

    float r = texture(DiffuseSampler, texCoord + shift).r;
    float g = texture(DiffuseSampler, texCoord).g;
    float b = texture(DiffuseSampler, texCoord - shift).b;
    float a = texture(DiffuseSampler, texCoord).a;

    fragColor = vec4(r, g, b, a);
}
