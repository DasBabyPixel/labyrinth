package de.dasbabypixel.labyrinth.lwjgl.render;

import org.lwjgl.opengl.GL11;

import de.dasbabypixel.labyrinth.lwjgl.glfw.Renderer;

public class GameRenderer extends Renderer {
	
	public GameRenderer() {
		
	}
	
	@Override
	public void render() {
		GL11.glBegin(GL11.GL_LINES);
		GL11.glVertex2f(getX(), getY());
		GL11.glVertex2f(getX() + getWidth(), getY() + getHeight());
		GL11.glEnd();
	}
}
