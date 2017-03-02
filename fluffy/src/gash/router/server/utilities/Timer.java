package gash.router.server.utilities;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Timer {
    private static final Logger logger = LoggerFactory.getLogger("Timer");
    private long electionTimeout;
    private TimerThread timerThread;
    private String name;
    private ElectionTimeoutListener electionTimeoutListener;

    /**
     * Constructor
     *
     * @param electionTimeout specify the election timeout
     * @param name            Name for timer.
     */
    public Timer(long electionTimeout, String name) {
        this.electionTimeout = electionTimeout;
        this.name = name;

    }

    public void start() {
        timerThread = new TimerThread();
        timerThread.start();
    }

    public void start(long timeout) {
        this.electionTimeout = timeout;
        this.start();
    }

    public void cancel() {
        if (timerThread == null) {
            return;
        }
        timerThread.interrupt();
    }

    private class TimerThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                //logger.info("Timer started");
                synchronized (this) {
                    sleep(electionTimeout);
                }

                //logger.info("Timer over");
                electionTimeoutListener.onElectionTimeout();
            } catch (InterruptedException e) {
                //logger.info("Timer stopped");
            }
        }
    }

    public void setTimeoutListener(ElectionTimeoutListener electionTimeoutListener) {
        this.electionTimeoutListener = electionTimeoutListener;
    }
}
