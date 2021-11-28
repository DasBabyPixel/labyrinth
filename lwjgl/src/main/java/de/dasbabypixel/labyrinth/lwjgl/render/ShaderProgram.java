package de.dasbabypixel.labyrinth.lwjgl.render;

import static org.lwjgl.opengl.GL20.*;

import java.io.IOException;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import de.dasbabypixel.labyrinth.lwjgl.resource.ResourcePath;

public class ShaderProgram extends AbstractShaderProgram {

	private final ShaderInfo info;
	
	public ShaderProgram(ShaderInfo info) throws ShaderException, IOException {
		super(info.createVertexShader(), info.createFragmentShader());
		this.info = info;
	}

	@Override
	protected void bindAttributes() {

	}

	@Override
	protected void getAttributeLocations() {

	}

	public static class ShaderInfo {

		private final ResourcePath vertexPath;
		private final ResourcePath fragmentPath;

		public ShaderInfo(ResourcePath path) throws JsonSyntaxException, IOException {
			JsonObject json = path.getResource().getJsonObject();
			vertexPath = new ResourcePath(json.get("vertex").getAsString());
			fragmentPath = new ResourcePath(json.get("fragment").getAsString());
		}

		public Shader createVertexShader() throws ShaderException, IOException {
			return new Shader(GL_VERTEX_SHADER, vertexPath.getResource().getUTF8());
		}

		public Shader createFragmentShader() throws ShaderException, IOException {
			return new Shader(GL_FRAGMENT_SHADER, fragmentPath.getResource().getUTF8());
		}

		public ResourcePath getFragmentPath() {
			return fragmentPath;
		}

		public ResourcePath getVertexPath() {
			return vertexPath;
		}
	}
}
