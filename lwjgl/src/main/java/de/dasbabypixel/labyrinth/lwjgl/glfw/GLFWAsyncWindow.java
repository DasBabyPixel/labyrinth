package de.dasbabypixel.labyrinth.lwjgl.glfw;

import static org.lwjgl.glfw.GLFW.*;

import java.util.Collection;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowCloseCallbackI;
import org.lwjgl.glfw.GLFWWindowPosCallbackI;
import org.lwjgl.glfw.GLFWWindowSizeCallbackI;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

public class GLFWAsyncWindow {

	private final AtomicLong windowId = new AtomicLong();
	private final AtomicBoolean shouldClose = new AtomicBoolean(false);
	private final AtomicBoolean visible = new AtomicBoolean(false);
	private final AtomicInteger border = new AtomicInteger(10);
	private final AtomicInteger width = new AtomicInteger(500);
	private final AtomicInteger height = new AtomicInteger(500);
	private final AtomicInteger framebufferWidth = new AtomicInteger(1);
	private final AtomicInteger framebufferHeight = new AtomicInteger(1);
	private final AtomicInteger x = new AtomicInteger(0);
	private final AtomicInteger y = new AtomicInteger(0);
	private final AtomicReference<Double> mousex = new AtomicReference<>(0D);
	private final AtomicReference<Double> mousey = new AtomicReference<>(0D);
	private final AtomicReference<Float> xscale = new AtomicReference<>(1F);
	private final AtomicReference<Float> yscale = new AtomicReference<>(1F);
	private final AtomicReference<WindowThread> windowThread = new AtomicReference<>();
	private final Collection<FramebufferSizeListener> framebufferSizeListeners = ConcurrentHashMap.newKeySet();

	public GLFWAsyncWindow() {
		this.windowThread.set(new WindowThread());

		this.windowThread.get().start();
	}

	public void addListener(Listener listener) {
		if (listener instanceof FramebufferSizeListener)
			framebufferSizeListeners.add((FramebufferSizeListener) listener);
	}

	public void removeListener(Listener listener) {
		framebufferSizeListeners.remove(listener);
	}

	public void setPosition(int x, int y) {
		this.x.set(x);
		this.y.set(y);
		queue(() -> {
			glfwSetWindowPos(windowId.get(), x, y);
		});
	}

	public CompletableFuture<?> createFuture() {
		return this.windowThread.get().createFuture;
	}

	public CompletableFuture<?> closeFuture() {
		return this.windowThread.get().closeFuture;
	}

	public CompletableFuture<?> setVisible(boolean visible) {
		if (this.visible.compareAndSet(!visible, visible)) {
			queue(visible ? () -> glfwShowWindow(windowId.get()) : () -> glfwHideWindow(windowId.get()));
		}
		return CompletableFuture.completedFuture(null);
	}

	public boolean isVisible() {
		return visible.get();
	}

	public CompletableFuture<?> show() {
		return setVisible(true);
	}

	public CompletableFuture<?> focus() {
		return queue(() -> glfwFocusWindow(windowId.get()));
	}

	public CompletableFuture<?> hide() {
		return setVisible(false);
	}

	public CompletableFuture<?> queue(Runnable runnable) {
		return this.windowThread.get().queue(runnable);
	}

	public boolean shouldClose() {
		return shouldClose.get();
	}

	private class RenderThread extends Thread {

		private final CompletableFuture<?> closeFuture = new CompletableFuture<>();
		private final AtomicBoolean shouldClose = new AtomicBoolean(false);
		private final Queue<Entry> queue = new ConcurrentLinkedQueue<>();
		private final FrameCounter frameCounter = new FrameCounter();
		private final FrameLimiter frameLimiter = new FrameLimiter(60);

		public RenderThread() {
			this.setName("GLFW-RenderThread");
		}

		private void updateViewport() {
			int b = border.get();
			GL11.glViewport(b, b, framebufferWidth.get(), framebufferHeight.get());
		}

		@Override
		public void run() {
			queue(() -> updateViewport());
			long windowId = GLFWAsyncWindow.this.windowId.get();
			glfwMakeContextCurrent(windowId);
			GL.createCapabilities();
			GL11.glLineWidth(5);
			GL11.glClearColor(1, 0, 0, 0.3F);
			while (!shouldClose.get()) {
				workQueue();
				frameLimiter.limit();
				GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
				GL11.glBegin(GL11.GL_LINES);
				GL11.glVertex2f(0, 0);
				GL11.glVertex2f(100, 100);
				GL11.glEnd();
				frameCounter.nextFrame();
				glfwSwapBuffers(windowId);
				System.out.println(System.currentTimeMillis() % 1000 + "frame");
			}
			closeFuture.complete(null);
		}

		public CompletableFuture<?> queue(Runnable runnable) {
			CompletableFuture<?> future = new CompletableFuture<>();
			queue.offer(new Entry(future, runnable));
			return future;
		}

		private void workQueue() {
			if (queue.isEmpty())
				return;
			Entry e;
			while ((e = queue.poll()) != null) {
				e.run.run();
				e.future.complete(null);
			}
		}

		private class Entry {
			private final CompletableFuture<?> future;
			private final Runnable run;

			public Entry(CompletableFuture<?> future, Runnable run) {
				this.future = future;
				this.run = run;
			}
		}
	}

	private class WindowThread extends Thread {

		private final Deque<Entry> queue = new ConcurrentLinkedDeque<>();
		private final CompletableFuture<?> closeFuture = new CompletableFuture<>();
		private final CompletableFuture<?> createFuture = new CompletableFuture<>();
		private final RenderThread renderThread = new RenderThread();
		private final AtomicBoolean changingSize = new AtomicBoolean(false);
		private final AtomicLong cursor = new AtomicLong(0L);

		private long arrowCursor;
		private long resizeCursorEW;
		private long resizeCursorNESW;
		private long resizeCursorNWSE;
		private long resizeCursorNS;
		private long resizeCursorAll;
		private final AtomicBoolean resizeTop = new AtomicBoolean(false);
		private final AtomicInteger resizeTopY = new AtomicInteger();
		private final AtomicBoolean resizeBot = new AtomicBoolean(false);
		private final AtomicInteger resizeBotY = new AtomicInteger();
		private final AtomicBoolean resizeLef = new AtomicBoolean(false);
		private final AtomicInteger resizeLefX = new AtomicInteger();
		private final AtomicBoolean resizeRig = new AtomicBoolean(false);
		private final AtomicInteger resizeRigX = new AtomicInteger();
		private final AtomicBoolean resize = new AtomicBoolean(false);

		private final AtomicBoolean move = new AtomicBoolean(false);
		private long windowId;

		public WindowThread() {
			this.setName("GLFW-WindowThread");
		}

		private void updateFramebufferSize(int fbw, int fbh) {
			framebufferWidth.set(fbw - Math.round((border.get() * 2) * xscale.get()));
			framebufferHeight.set(fbh - Math.round((border.get() * 2) * yscale.get()));
		}

		private void updateFramebufferSize() {
			int[] w = new int[1];
			int[] h = new int[1];
			glfwGetFramebufferSize(windowId, w, h);
			updateFramebufferSize(w[0], h[0]);
		}

		private void updateScale() {
			float[] axscale = new float[1];
			float[] ayscale = new float[1];
			glfwGetWindowContentScale(windowId, axscale, ayscale);
			xscale.set(axscale[0]);
			yscale.set(ayscale[0]);
		}

		private void setCursor(long c) {
			cursor.set(c);
			glfwSetCursor(windowId, c);
		}

		@Override
		public void run() {
			arrowCursor = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
			resizeCursorEW = glfwCreateStandardCursor(GLFW_RESIZE_EW_CURSOR);
			resizeCursorNS = glfwCreateStandardCursor(GLFW_RESIZE_NS_CURSOR);
			resizeCursorAll = glfwCreateStandardCursor(GLFW_RESIZE_ALL_CURSOR);

			resizeCursorNESW = glfwCreateStandardCursor(GLFW_RESIZE_NESW_CURSOR);
			if (resizeCursorNESW == 0L) {
				resizeCursorNESW = resizeCursorAll;
			}
			resizeCursorNWSE = glfwCreateStandardCursor(GLFW_RESIZE_NWSE_CURSOR);
			if (resizeCursorNWSE == 0L) {
				resizeCursorNWSE = resizeCursorAll;
			}

			glfwDefaultWindowHints();
			glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
			glfwWindowHint(GLFW_TRANSPARENT_FRAMEBUFFER, GLFW_TRUE);
			glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);
			glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
			GLFWAsyncWindow.this.windowId.set(windowId = glfwCreateWindow(width.get(), height.get(), "title", 0, 0));
			GLFWVidMode mode = glfwGetVideoMode(glfwGetPrimaryMonitor());
			setPosition(mode.width() / 2 - width.get() / 2, mode.height() / 2 - height.get() / 2);
			setCursor(arrowCursor);
			glfwSetWindowCloseCallback(windowId, new GLFWWindowCloseCallbackI() {
				@Override
				public void invoke(long window) {
					shouldClose.set(true);
				}
			});
			glfwSetWindowPosCallback(windowId, new GLFWWindowPosCallbackI() {
				@Override
				public void invoke(long window, int xpos, int ypos) {
					x.set(xpos);
					y.set(ypos);
				}
			});
			glfwSetWindowSizeCallback(windowId, new GLFWWindowSizeCallbackI() {
				@Override
				public void invoke(long window, int w, int h) {
					updateScale();
					width.set(w);
					height.set(h);
				}
			});
			glfwSetFramebufferSizeCallback(windowId, new GLFWFramebufferSizeCallbackI() {
				@Override
				public void invoke(long window, int width, int height) {
					updateScale();
					updateFramebufferSize(width, height);
					renderThread.queue(() -> renderThread.updateViewport());
				}
			});
			glfwSetCursorPosCallback(windowId, new GLFWCursorPosCallbackI() {

				private int r(double d) {
					return (int) Math.round(d);
				}

				private double olmx = 0;
				private double olmy = 0;

				@Override
				public void invoke(long window, double mx, double my) {
					if (olmx == mx && olmy == my) {
						return;
					}
					int w = width.get();
					int h = height.get();
					int b = border.get();
					boolean top = my < b;
					boolean bot = my > h - b;
					boolean lef = mx < b;
					boolean rig = mx > w - b;

					if (move.get()) {
						int newx = r(x.get() + mx - mousex.get());
						int newy = r(y.get() + my - mousey.get());
						glfwSetWindowPos(window, newx, newy);
						return;
					} else if (resize.get()) {
						if (resizeBot.get() && resizeRig.get()) {
							glfwSetWindowSize(window, r(width.get() + mx - mousex.getAndSet(mx)),
									r(height.get() + my - mousey.getAndSet(my)));
						} else if (resizeTop.get() && resizeRig.get()) {
							int ny = r(y.get() + my - mousey.get());
							int nh = resizeBotY.get() - ny;
							glfwSetWindowSize(window, r(width.get() + mx - mousex.getAndSet(mx)), nh);
							glfwSetWindowPos(window, x.get(), ny);
						} else if (resizeTop.get() && resizeLef.get()) {
							int nx = r(x.get() + mx - mousex.get());
							int ny = r(y.get() + my - mousey.get());
							int nw = resizeRigX.get() - nx;
							int nh = resizeBotY.get() - ny;
							glfwSetWindowSize(window, nw, nh);
							glfwSetWindowPos(window, nx, ny);
						} else if (resizeBot.get() && resizeLef.get()) {
							int nx = r(x.get() + mx - mousex.get());
							int nw = resizeRigX.get() - nx;
							glfwSetWindowSize(window, nw, r(height.get() + my - mousey.getAndSet(my)));
							glfwSetWindowPos(window, nx, y.get());
						} else if (resizeBot.get()) {
							glfwSetWindowSize(window, width.get(), r(height.get() + my - mousey.getAndSet(my)));
						} else if (resizeRig.get()) {
							glfwSetWindowSize(window, r(width.get() + mx - mousex.getAndSet(mx)), height.get());
						} else if (resizeLef.get()) {
							int nx = r(x.get() + mx - mousex.get());
							int nw = resizeRigX.get() - nx;
							glfwSetWindowSize(window, nw, height.get());
							glfwSetWindowPos(window, nx, y.get());
						} else if (resizeTop.get()) {
							int ny = r(y.get() + my - mousey.get());
							int nh = resizeBotY.get() - ny;
							glfwSetWindowSize(window, width.get(), nh);
							glfwSetWindowPos(window, x.get(), ny);
						}
						renderThread.queue(() -> {
							renderThread.updateViewport();
						});
						return;
					} else {
						mousex.set(mx);
						mousey.set(my);
					}

					resizeTop.set(top);
					resizeBot.set(bot);
					resizeLef.set(lef);
					resizeRig.set(rig);
					if ((top && lef) || (bot && rig)) {
						setCursor(resizeCursorNWSE);
					} else if ((top && rig) || (bot && lef)) {
						setCursor(resizeCursorNESW);
					} else if (top || bot) {
						setCursor(resizeCursorNS);
					} else if (lef || rig) {
						setCursor(resizeCursorEW);
					} else {
						if (cursor.get() != arrowCursor) {
							setCursor(arrowCursor);
						}
					}
				}
			});
			glfwSetMouseButtonCallback(windowId, new GLFWMouseButtonCallbackI() {
				@Override
				public void invoke(long window, int button, int action, int mods) {
					boolean resize = resizeTop.get() || resizeBot.get() || resizeLef.get() || resizeRig.get();
					if (resize) {
						if (button == 0) {
							if (action == GLFW_PRESS) {
								GLFWAsyncWindow.WindowThread.this.resize.set(true);
								resizeTopY.set(y.get());
								resizeBotY.set(y.get() + height.get());
								resizeLefX.set(x.get());
								resizeRigX.set(x.get() + width.get());
							} else if (action == GLFW_RELEASE) {
								GLFWAsyncWindow.WindowThread.this.resize.set(false);
							}
						}
					} else {
						if (button == 0) {
							if (action == GLFW_PRESS) {
								move.set(true);
							} else if (action == GLFW_RELEASE) {
								move.set(false);
							}
						}
					}
				}
			});
			updateScale();
			updateFramebufferSize();
			createFuture.complete(null);
			renderThread.start();
			workQueue();
			while (!shouldClose()) {
				glfwWaitEventsTimeout(1D / 100D);
				glfwPollEvents();
				workQueue();
			}
			closeFuture.complete(null);
			workQueue();
			renderThread.shouldClose.set(true);
			try {
				renderThread.closeFuture.get();
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			} catch (ExecutionException ex) {
				ex.printStackTrace();
			}
			glfwDestroyCursor(arrowCursor);
			glfwDestroyCursor(resizeCursorAll);
			glfwDestroyCursor(resizeCursorEW);
			glfwDestroyCursor(resizeCursorNS);
			if (resizeCursorNESW != 0L) {
				glfwDestroyCursor(resizeCursorNESW);
			}
			if (resizeCursorNWSE != 0L) {
				glfwDestroyCursor(resizeCursorNWSE);
			}
			glfwDestroyWindow(windowId);
		}

		private void workQueue() {
			if (queue.isEmpty())
				return;
			Entry e;
			while ((e = queue.poll()) != null) {
				e.run.run();
				e.future.complete(null);
			}
		}

		public CompletableFuture<?> queue(Runnable runnable) {
			CompletableFuture<?> future = new CompletableFuture<>();
			queue.offer(new Entry(future, runnable));
			return future;
		}

		private class Entry {
			private final CompletableFuture<?> future;
			private final Runnable run;

			public Entry(CompletableFuture<?> future, Runnable run) {
				this.future = future;
				this.run = run;
			}
		}
	}
}
