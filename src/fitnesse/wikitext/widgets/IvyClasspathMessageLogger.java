package fitnesse.wikitext.widgets;

import org.apache.ivy.util.AbstractMessageLogger;
import org.apache.ivy.util.Message;

public class IvyClasspathMessageLogger extends AbstractMessageLogger {
    private int level = Message.MSG_WARN;

    /**
     * @param level
     */
    public IvyClasspathMessageLogger() {
    }

    public void log(String msg, int level) {
        if (level <= this.level) {
            System.out.println(msg);
        }
    }

    public void rawlog(String msg, int level) {
        log(msg, level);
    }

    public void doProgress() {
    }

    public void doEndProgress(String msg) {
        System.out.println(msg);
    }

    public int getLevel() {
        return level;
    }
}
