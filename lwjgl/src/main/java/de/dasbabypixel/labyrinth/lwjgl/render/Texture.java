package de.dasbabypixel.labyrinth.lwjgl.render;

import static org.lwjgl.opengl.GL11.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import de.dasbabypixel.labyrinth.lwjgl.resource.ResourcePath;

public class Texture {

	private final int id;

	public Texture(ResourcePath path) throws IOException {
		this(readBufferedImage(path));
	}

	public Texture(BufferedImage image) {
		id = GL11.glGenTextures();
		bind();
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		int[] pixels = new int[image.getWidth() * image.getHeight()];
		image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
		ByteBuffer buf = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int pixel = pixels[y * image.getWidth() + x];
				buf.put((byte) ((pixel >> 16) & 0xFF)); // Red component
				buf.put((byte) ((pixel >> 8) & 0xFF)); // Green component
				buf.put((byte) (pixel & 0xFF)); // Blue component
				buf.put((byte) ((pixel >> 24) & 0xFF)); // Alpha component. Only for RGBA
			}
		}
		buf.flip();
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, image.getWidth(), image.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE,
				buf);
	}

	public static BufferedImage readBufferedImage(ResourcePath path) throws IOException {
		return ImageIO.read(path.getResource().getInputStream());
	}

	public void bind() {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
	}
}
