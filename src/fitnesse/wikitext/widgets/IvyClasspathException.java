package fitnesse.wikitext.widgets;

import java.util.ArrayList;
import java.util.List;

import util.StringUtil;

public class IvyClasspathException extends Exception {
	private static final long serialVersionUID = 1L;
	private List<String> problems;

	public IvyClasspathException(String s, Throwable t) {
		super(s, t);
	}

	public IvyClasspathException(String s) {
		super(s);
	}

	public IvyClasspathException(List<String> problems) {
		this.problems = problems;
	}

	@Override
	public String getMessage() {
		String msg = super.getMessage();
		if (msg == null) {
			return StringUtil.join(problems, "; ");
		}
		return msg;
	}

	public List<String> getProblems() {
		if (problems != null) {
			return problems;
		}
		List<String> p = new ArrayList<String>(1);
		p.add(super.getMessage());
		return p;
	}
}
