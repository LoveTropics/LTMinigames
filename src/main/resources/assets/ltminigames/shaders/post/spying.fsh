#version 330

#moj_import <minecraft:globals.glsl>

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

const vec3 Gray = vec3(0.3, 0.59, 0.11);
const vec3 SpectralTint = vec3(0.85, 0.7, 1.0);
const vec3 VignetteColor = vec3(0.22, 0.02, 0.42);

void main() {
    float time = GameTime * 1200.0;
    vec2 fromCenter = texCoord - 0.5;
    float edge = smoothstep(0.25, 0.72, length(fromCenter));

    vec2 wave = vec2(
        sin(texCoord.y * 14.0 + time * 1.7) + sin(texCoord.y * 31.0 - time * 2.3) * 0.5,
        cos(texCoord.x * 12.0 + time * 1.3) + cos(texCoord.x * 27.0 + time * 2.9) * 0.5
    ) * mix(0.0025, 0.009, edge);
    vec2 uv = texCoord + wave;

    vec2 fringe = fromCenter * edge * 0.015 + wave;
    vec3 color = vec3(
        texture(InSampler, uv + fringe).r,
        texture(InSampler, uv).g,
        texture(InSampler, uv - fringe).b
    );

    vec2 echo = vec2(sin(time * 0.9), cos(time * 0.7)) * 0.012;
    color = mix(color, texture(InSampler, uv + echo).rgb, 0.3);

    color = mix(color, dot(color, Gray) * SpectralTint, 0.5);

    float pulse = 0.85 + sin(time * 2.0) * 0.08 + sin(time * 13.0) * 0.02;
    color = mix(color, VignetteColor, clamp(edge * pulse, 0.0, 1.0));

    fragColor = vec4(color, 1.0);
}
