package edu.cg;


import java.io.IOException;

public class Main {

    private Window window = new Window();
    private Viewer viewer = new Viewer(Window.WINDOW_INIT_WIDTH, Window.WINDOW_INIT_HEIGHT);
    private Controller controller = new Controller(window, viewer);
    private Timer timer = new Timer();

    private void loop() {

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.

        while (!window.shouldClose()) {
            viewer.render();
            window.refresh();
            sync(30);
        }
    }

    private void sync(int fps) {
        double lastLoopTime = timer.getLastLoopTime();
        double now = timer.getTime();
        float targetTime = 1f / fps;

        while (now - lastLoopTime < targetTime) {
            Thread.yield();

            /* This is optional if you want your game to stop consuming too much
             * CPU but you will loose some accuracy because Thread.sleep(1)
             * could sleep longer than 1 millisecond */
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {

            }

            now = timer.getTime();
        }
    }

    public void run() {
        window.init();
        viewer.init();
        controller.init();
        loop();
        window.dispose();
    }

    public static void main(String[] args) throws IOException{
        new Main().run();
    }
}
