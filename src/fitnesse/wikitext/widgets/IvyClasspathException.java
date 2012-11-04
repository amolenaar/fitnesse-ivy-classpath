package fitnesse.wikitext.widgets;

public class IvyClasspathException extends Exception {
	private static final long serialVersionUID = 1L;

	public IvyClasspathException(String s, Throwable t) {
		super(s, t);
	}

	public IvyClasspathException(String s) {
		super(s);
	}

}
