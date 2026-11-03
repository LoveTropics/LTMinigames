#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

const vec3 Gray = vec3(0.3, 0.59, 0.11);
const vec3 SpectralTint = vec3(0.85, 0.7, 1.0);
const vec3 VignetteColor = vec3(0.22, 0.02, 0.42);

void main() {
    vec2 fromCenter = texCoord - 0.5;
    float edge = smoothstep(0.25, 0.72, length(fromCenter));

    vec2 fringe = fromCenter * edge * 0.015;
    vec3 color = vec3(
        texture(InSampler, texCoord + fringe).r,
        texture(InSampler, texCoord).g,
        texture(InSampler, texCoord - fringe).b
    );

    color = mix(color, dot(color, Gray) * SpectralTint, 0.5);
    color = mix(color, VignetteColor, edge * 0.85);

    fragColor = vec4(color, 1.0);
}
