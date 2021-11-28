package de.dasbabypixel.labyrinth.lwjgl.resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public interface Resource {

	public static final Gson gson = new Gson();

	InputStream getInputStream();

	default String getUTF8() throws IOException {
		return new String(getBytes(), StandardCharsets.UTF_8);
	}

	default JsonObject getJsonObject() throws JsonSyntaxException, IOException {
		return gson.fromJson(getUTF8(), JsonObject.class);
	}

	default byte[] getBytes() throws IOException {
		InputStream in = getInputStream();
		byte[] buffer = new byte[1024];
		byte[] result = new byte[0];
		int count = 0;
		int read;
		while ((read = in.read(buffer, 0, buffer.length)) != -1) {
			if (count + read > result.length) {
				result = Arrays.copyOf(result, result.length + buffer.length);
			}
			System.arraycopy(buffer, 0, result, count, read);
			count += read;
		}
		return Arrays.copyOf(result, count);
	}
}
