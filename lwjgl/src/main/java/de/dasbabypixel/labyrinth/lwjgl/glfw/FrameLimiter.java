package de.dasbabypixel.labyrinth.lwjgl.glfw;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class FrameLimiter {

	private final int maxfps;
	private final long oneFrameNanos;
	private long lastFrameNanos = -1;
	private final long second = TimeUnit.SECONDS.toNanos(1);

	public FrameLimiter(int maxfps) {
		this.maxfps = maxfps;
		this.oneFrameNanos = second / maxfps;
	}

	public void limit() {
		long currentNanos = System.nanoTime();
		if (lastFrameNanos == -1) {
			lastFrameNanos = currentNanos;
			return;
		}
		long nanoDifference = currentNanos - lastFrameNanos;
		long sleepTime = oneFrameNanos - nanoDifference;
		if (sleepTime > 0) {
			sleep(sleepTime);
		}
		lastFrameNanos += oneFrameNanos;
		if (lastFrameNanos < currentNanos - second) {
			lastFrameNanos = currentNanos;
		}
	}

	private void sleep(long nanos) {
		LockSupport.parkNanos(nanos);
	}
}
