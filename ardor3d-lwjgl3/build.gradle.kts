
description = "Ardor 3D LWJGL3"

val lwjglVersion: String? = project.findProperty("lwjglVersion") as String?
val lwjglNatives: String? = project.findProperty("lwjglNatives") as String?

dependencies {
	api(project(":ardor3d-core"))
  	
	api("org.lwjgl:lwjgl:$lwjglVersion")
	api("org.lwjgl:lwjgl-assimp:$lwjglVersion")
	api("org.lwjgl:lwjgl-glfw:$lwjglVersion")
	api("org.lwjgl:lwjgl-jawt:$lwjglVersion")
	api("org.lwjgl:lwjgl-openal:$lwjglVersion")
	api("org.lwjgl:lwjgl-opengl:$lwjglVersion")
	api("org.lwjgl:lwjgl-stb:$lwjglVersion")

	implementation("org.lwjgl:lwjgl:$lwjglVersion:$lwjglNatives")
	implementation("org.lwjgl:lwjgl-assimp:$lwjglVersion:$lwjglNatives")
	implementation("org.lwjgl:lwjgl-glfw:$lwjglVersion:$lwjglNatives")
	implementation("org.lwjgl:lwjgl-openal:$lwjglVersion:$lwjglNatives")
	implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion:$lwjglNatives")
	implementation("org.lwjgl:lwjgl-stb:$lwjglVersion:$lwjglNatives")
}