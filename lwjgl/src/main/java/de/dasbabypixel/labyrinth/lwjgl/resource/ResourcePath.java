package de.dasbabypixel.labyrinth.lwjgl.resource;

import java.util.Objects;

public class ResourcePath {

	private final String path;

	public ResourcePath(String path) {
		this.path = path;
	}

	@Override
	public String toString() {
		return path;
	}

	public String toFilePath() {
		return String.format("assets/%s", path);
	}

	public Resource getResource() {
		return () -> Resource.class.getClassLoader().getResourceAsStream(toFilePath());
	}

	@Override
	public int hashCode() {
		return Objects.hash(path);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResourcePath other = (ResourcePath) obj;
		return Objects.equals(path, other.path);
	}
}
