package de.dasbabypixel.labyrinth.lwjgl;

import static org.lwjgl.glfw.GLFW.*;

import de.dasbabypixel.labyrinth.lwjgl.glfw.GLFWAsyncWindow;

public class Main {

	public static void main(String[] args) {
		glfwInit();
		GLFWAsyncWindow window = new GLFWAsyncWindow();
		window.closeFuture().thenAccept(c -> {
			glfwTerminate();
		});
		window.show();
	}
}
