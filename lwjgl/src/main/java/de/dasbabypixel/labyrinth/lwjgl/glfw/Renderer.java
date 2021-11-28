package de.dasbabypixel.labyrinth.lwjgl.glfw;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class Renderer {

	final AtomicInteger x = new AtomicInteger();
	final AtomicInteger y = new AtomicInteger();
	final AtomicInteger w = new AtomicInteger();
	final AtomicInteger h = new AtomicInteger();

	public abstract void render();
	
	public int getX() {
		return x.get();
	}

	public int getY() {
		return y.get();
	}

	public int getWidth() {
		return w.get();
	}

	public int getHeight() {
		return h.get();
	}
}
