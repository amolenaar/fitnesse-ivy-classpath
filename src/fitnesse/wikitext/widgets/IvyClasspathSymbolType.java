package fitnesse.wikitext.widgets;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.util.url.CredentialsStore;
import org.apache.ivy.util.url.URLHandler;
import org.apache.ivy.util.url.URLHandlerDispatcher;
import org.apache.ivy.util.url.URLHandlerRegistry;

import util.Maybe;
import fitnesse.html.HtmlUtil;
import fitnesse.wikitext.parser.Matcher;
import fitnesse.wikitext.parser.Parser;
import fitnesse.wikitext.parser.PathsProvider;
import fitnesse.wikitext.parser.Rule;
import fitnesse.wikitext.parser.Symbol;
import fitnesse.wikitext.parser.SymbolType;
import fitnesse.wikitext.parser.Translation;
import fitnesse.wikitext.parser.Translator;

/**
 * <p>This symbol type adds Ivy support to FitNesse.
 *
 * <p>Usage:
 * <pre>
 *  !ivy [-s ivysettings.xml] [-c config1,config2,..] [ivy.xml]
 * </pre>
 *
 * <p><tt>-s</tt> defines the Ivy settings file to use. If not defined the Ivy defaults will be
 * used.
 * <p><tt>-c</tt> defines the configurations to load from the ivy file. The configurations can be separated by a comma, but should not contain spaces
 * <p>The Ivy.xml file can also be defined. If not defined, <tt>ivy.xml</tt> is assumed.
 */
public class IvyClasspathSymbolType extends SymbolType implements Rule, Translation, PathsProvider {

	enum OptionType {
		IVY_XML,
		IVYSETTINGS_XML,
		CONFIGURATION
	};

	static class Report {
		List<String> classpath = new ArrayList<String>();
		List<String> errors = new ArrayList<String>();
	}

    public IvyClasspathSymbolType() {
        super("IvyClasspathSymbolType");

        wikiMatcher(new Matcher().startLineOrCell().string("!ivy"));

        wikiRule(this);
        htmlTranslation(this);
    }

	@Override
	public Collection<String> providePaths(Translator translator, Symbol symbol) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toTarget(Translator translator, Symbol symbol) {
        List<File> classpathElements;
		try {
			classpathElements = getClasspathElements(symbol);
		} catch (IvyClasspathException e) {
			return HtmlUtil.metaText("ERROR: " + e.getMessage());
		}

        String classpathForRender = "";
        for (File element : classpathElements) {
            classpathForRender += HtmlUtil.metaText("classpath: " + element.getAbsolutePath()) + HtmlUtil.BRtag;
        }
        return classpathForRender;
	}

	@Override
	public Maybe<Symbol> parse(Symbol symbol, Parser parser) {
		OptionType nextOption = OptionType.IVY_XML;

        Symbol body = parser.parseToEnd(SymbolType.Newline);
        for (Symbol option: body.getChildren()) {
            if (option.isType(SymbolType.Whitespace)) {
            	continue;
            } else if ("-s".equals(option.getContent())) {
            	nextOption = OptionType.IVYSETTINGS_XML;
            } else if ("-c".equals(option.getContent())) {
            	nextOption = OptionType.CONFIGURATION;
            } else {
            	symbol.putProperty(nextOption.name(), option.getContent());
            	nextOption = OptionType.IVY_XML;
            }
        }

        return new Maybe<Symbol>(symbol);
	}

    List<File> getClasspathElements(Symbol symbol) throws IvyClasspathException {
        Ivy ivy = Ivy.newInstance();
        initMessage(ivy);
        IvySettings settings = initSettings(ivy, symbol);
        ivy.pushContext();

        String[] confs;
        confs = getConfigurations(symbol);

        File ivyfile = new File(settings.substitute(symbol.getProperty(OptionType.IVY_XML.name(), "ivy.xml")));
        if (!ivyfile.exists()) {
        	throw new IvyClasspathException("Ivy file not found: " + ivyfile);
        } else if (ivyfile.isDirectory()) {
        	throw new IvyClasspathException("Ivy file is not a file: " + ivyfile);
        }

        ResolveOptions resolveOptions = new ResolveOptions()
        		.setConfs(confs)
                .setValidate(true);
        ResolveReport report;
		try {
			report = ivy.resolve(ivyfile.toURI().toURL(), resolveOptions);
		} catch (Exception e) {
			throw new IvyClasspathException("Unable to resolve dependencies for file " + ivyfile.getAbsolutePath(), e);
		}

        if (report.hasError()) {
            throw new IvyClasspathException(join(report.getAllProblemMessages(), HtmlUtil.BRtag));
        } else {
        	List<File> result = new ArrayList<File>(report.getAllArtifactsReports().length);
        	for (ArtifactDownloadReport adr: report.getAllArtifactsReports()) {
        		result.add(adr.getLocalFile());
        	}
        	return result;
        }
    }

	private String[] getConfigurations(Symbol symbol) {
		String[] confs;
		if (symbol.hasProperty(OptionType.CONFIGURATION.name())) {
            confs =symbol.getProperty(OptionType.CONFIGURATION.name()).split(",");
        } else {
            confs = new String[] {"*"};
        }
		return confs;
	}

    private static void initMessage(Ivy ivy) {
            ivy.getLoggerEngine().pushLogger(new IvyClasspathMessageLogger());
    }

    private static IvySettings initSettings(Ivy ivy, Symbol symbol)
            throws IvyClasspathException {
        IvySettings settings = ivy.getSettings();
        settings.addAllVariables(System.getProperties());
        configureURLHandler(null, null, null, null);

        String settingsPath = symbol.getProperty(OptionType.IVYSETTINGS_XML.name());
        if ("".equals(settingsPath)) {
        	try {
        		ivy.configureDefault();
			} catch (Exception e) {
				throw new IvyClasspathException("Unable to set default configuration", e);
			}
        } else {
            File conffile = new File(settingsPath);
            if (!conffile.exists()) {
                throw new IvyClasspathException("Ivy configuration file not found: " + conffile);
            } else if (conffile.isDirectory()) {
            	throw new IvyClasspathException("Ivy configuration file is not a file: " + conffile);
            }
            try {
				ivy.configure(conffile);
			} catch (Exception e) {
				throw new IvyClasspathException("Unable to configure ivy with file " + conffile.getAbsolutePath(), e);
			}
        }
        return settings;
    }

    private static void configureURLHandler(String realm, String host, String username,
            String passwd) {
        CredentialsStore.INSTANCE.addCredentials(realm, host, username, passwd);

        URLHandlerDispatcher dispatcher = new URLHandlerDispatcher();
        URLHandler httpHandler = URLHandlerRegistry.getHttp();
        dispatcher.setDownloader("http", httpHandler);
        dispatcher.setDownloader("https", httpHandler);
        URLHandlerRegistry.setDefault(dispatcher);
    }

    private static String join(List<?> objects, String sep) {
    	StringBuffer buf = new StringBuffer(256);
    	for (Object o: objects) {
    		buf.append(o != null ? o.toString() : "null");
    		buf.append(sep);
    	}
    	return buf.toString();
    }
}
