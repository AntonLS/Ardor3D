#version 330 core

in vec3 vertex;
in vec4 color;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

out vec4 DiffuseColor;
out vec3 WorldPos;

void main()
{
    WorldPos = vec3(model * vec4(vertex, 1.0));
    DiffuseColor = color;

    gl_Position =  projection * view * vec4(WorldPos, 1.0);
}
