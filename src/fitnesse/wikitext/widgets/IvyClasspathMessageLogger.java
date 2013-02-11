package fitnesse.wikitext.widgets;

import org.apache.ivy.util.AbstractMessageLogger;
import org.apache.ivy.util.Message;

/**
 * A simplified message logger. It's not as verbose as the default message loggers.
 */
class IvyClasspathMessageLogger extends AbstractMessageLogger {
    private int level;

    /**
     * @param level
     */
    public IvyClasspathMessageLogger() {
    	final String loglevel = System.getProperty("ivy.loglevel", "i");
    	setLoglevel(loglevel);
    }

	public void setLoglevel(final String loglevel) {
    	if (loglevel == null || "".equals(loglevel)) {
    		level = Message.MSG_INFO;
    		return;
    	}
		switch (loglevel.charAt(0)) {
    	case 'e':
    	case 'E':
    		level = Message.MSG_ERR;
    		break;
    	case 'w':
    	case 'W':
    		level = Message.MSG_WARN;
    		break;
    	case 'v':
    	case 'V':
    		level = Message.MSG_VERBOSE;
    		break;
    	case 'd':
    	case 'D':
    		level = Message.MSG_DEBUG;
    		break;
		default:
			level = Message.MSG_INFO;
    	}
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
