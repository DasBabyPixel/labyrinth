package de.dasbabypixel.labyrinth.lwjgl.render;

import static org.lwjgl.opengl.GL20.*;

public abstract class AbstractShaderProgram {

	private final int id;

	public AbstractShaderProgram(Shader vertexShader, Shader fragmentShader) throws ShaderException {
		id = glCreateProgram();
		glAttachShader(id, vertexShader.getId());
		glAttachShader(id, fragmentShader.getId());
		bindAttributes();
		glLinkProgram(id);
		getAttributeLocations();
		if (glGetProgrami(id, GL_LINK_STATUS) == 0) {
			String log = glGetProgramInfoLog(id);
			throw new ShaderException("Unable to link program: " + log);
		}
	}

	protected abstract void bindAttributes();

	protected abstract void getAttributeLocations();

	protected int getAttributeLocation(String attributeName) {
		return glGetAttribLocation(id, attributeName);
	}

	protected int getUniformLocation(String uniformName) {
		return glGetUniformLocation(id, uniformName);
	}

	protected void bindAttribute(int attribute, String attributeName) {
		glBindAttribLocation(id, attribute, attributeName);
	}

	public void use() {
		glUseProgram(id);
	}

	public void delete() {
		glDeleteProgram(id);
	}

	public int getId() {
		return id;
	}

	public static class Shader {

		private final int id;

		public Shader(int type, String source) throws ShaderException {
			id = glCreateShader(type);
			glShaderSource(id, source);
			glCompileShader(id);
			if (glGetShaderi(id, GL_COMPILE_STATUS) == 0) {
				String log = glGetShaderInfoLog(id);
				throw new ShaderException("Unable to compile shader: " + log);
			}
		}

		public void delete() {
			glDeleteShader(id);
		}

		public int getId() {
			return id;
		}
	}

	public static class ShaderException extends Exception {

		public ShaderException() {
			super();
		}

		public ShaderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
		}

		public ShaderException(String message, Throwable cause) {
			super(message, cause);
		}

		public ShaderException(String message) {
			super(message);
		}

		public ShaderException(Throwable cause) {
			super(cause);
		}
	}
}
