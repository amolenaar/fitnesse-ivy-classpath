package fitnesse.wikitext.widgets;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.m2.PomModuleDescriptorParser;
import org.apache.ivy.util.url.CredentialsStore;
import org.apache.ivy.util.url.URLHandler;
import org.apache.ivy.util.url.URLHandlerDispatcher;
import org.apache.ivy.util.url.URLHandlerRegistry;

import util.Maybe;
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
 *  !resolve [-pom] [-s your-ivysettings.xml] [-c config] [ivy.xml|pom.xml]
 * </pre>
 *
 * <p><tt>-s</tt> defines the Ivy settings file to use. If not defined the Ivy defaults will be
 * used.
 * <p><tt>-c</tt> defines the configurations to load from the ivy file. The configurations can be separated by a comma, but should not contain spaces
 * <p><tt>-pom</tt> makes Ivy use pom file resolution. You should define a pom file as argument.
 * <p>The Ivy.xml file can also be defined. If not defined, <tt>ivy.xml</tt> is assumed.
 */
public class IvyClasspathSymbolType extends SymbolType implements Rule, Translation, PathsProvider {

	private static final String DEFAULT_CONFIGURATION = "*";
	private static final String DEFAULT_IVY_XML = "ivy.xml";

	enum OptionType {
		DEPENDENCY_FILE,
		IVYSETTINGS_XML,
		CONFIGURATION,
		IS_POM_XML
	};

	static class Report {
		List<String> classpath = new ArrayList<String>();
		List<String> errors = new ArrayList<String>();
	}

    public IvyClasspathSymbolType() {
        super("IvyClasspathSymbolType");

        wikiMatcher(new Matcher().startLineOrCell().string("!resolve"));

        wikiRule(this);
        htmlTranslation(this);
    }

	@Override
	public Collection<String> providePaths(Translator translator, Symbol symbol) {
		List<File> classpath;
		try {
			classpath = getClasspathElements(symbol);
		} catch (IvyClasspathException e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
		List<String> paths = new ArrayList<String>(classpath.size());
		for (File cp: classpath) {
			paths.add(cp.getAbsolutePath());
		}
		return paths;
	}

	@Override
	public String toTarget(Translator translator, Symbol symbol) {
        StringBuffer buf = new StringBuffer(256);
        buf.append("<p class='meta'>Classpath from \"")
        	.append(symbol.hasProperty(OptionType.DEPENDENCY_FILE.name()) ? symbol.getProperty(OptionType.DEPENDENCY_FILE.name()) : DEFAULT_IVY_XML)
        	.append("\"");
        if (symbol.hasProperty(OptionType.IVYSETTINGS_XML.name())) {
        	buf.append(", with settings file \"")
        		.append(symbol.getProperty(OptionType.IVYSETTINGS_XML.name()))
        		.append("\"");
        }
        if (symbol.hasProperty(OptionType.CONFIGURATION.name())) {
        	buf.append(", configuration \"")
    		.append(symbol.getProperty(OptionType.CONFIGURATION.name()))
    		.append("\"");
        }
        buf.append(":</p><ul class='meta'>");

        try {
 			for (File dep: getClasspathElements(symbol)) {
 				buf.append("<li>")
 					.append(dep.getAbsolutePath())
 					.append("</li>");
 			}
 		} catch (IvyClasspathException e) {
 			for (String problem: e.getProblems()) {
 				buf.append("<li class='error'>ERROR:")
 					.append(problem)
 					.append("</li>");
 			}
 		} catch (Exception e) {
 			buf.append("<li>ERROR:")
				.append(e.getMessage())
				.append("</li>");
 		}

        buf.append("</ul>");
        return buf.toString();
	}

	@Override
	public Maybe<Symbol> parse(Symbol symbol, Parser parser) {
		OptionType nextOption = OptionType.DEPENDENCY_FILE;

        Symbol body = parser.parseToEnd(SymbolType.Newline);
        for (Symbol option: body.getChildren()) {
            if (option.isType(SymbolType.Whitespace)) {
            	continue;
            } else if ("-s".equals(option.getContent())) {
            	nextOption = OptionType.IVYSETTINGS_XML;
            } else if ("-c".equals(option.getContent())) {
            	nextOption = OptionType.CONFIGURATION;
            } else if ("-pom".equals(option.getContent())) {
            	symbol.putProperty(OptionType.IS_POM_XML.name(), "true");
            } else {
            	if (symbol.hasProperty(nextOption.name())) {
            		// Deal with this
            	}
            	symbol.putProperty(nextOption.name(), option.getContent());
            	nextOption = OptionType.DEPENDENCY_FILE;
            }
        }

        return new Maybe<Symbol>(symbol);
	}

    @SuppressWarnings("unchecked")
	List<File> getClasspathElements(Symbol symbol) throws IvyClasspathException {
        Ivy ivy = Ivy.newInstance();
        initMessage(ivy);
        IvySettings settings = initSettings(ivy, symbol);
        ivy.pushContext();

        String[] confs = new String[] { symbol.getProperty(OptionType.CONFIGURATION.name(), DEFAULT_CONFIGURATION) };

        File ivyfile = new File(settings.substitute(symbol.getProperty(OptionType.DEPENDENCY_FILE.name(), DEFAULT_IVY_XML)));
        if (!ivyfile.exists()) {
        	throw new IvyClasspathException("Ivy/pom file not found: " + ivyfile);
        } else if (ivyfile.isDirectory()) {
        	throw new IvyClasspathException("Ivy/pom file is not a file: " + ivyfile);
        }

        ResolveOptions resolveOptions = new ResolveOptions()
        		.setConfs(confs)
                .setValidate(true);
        ResolveReport report;
		try {
			if (symbol.hasProperty(OptionType.IS_POM_XML.name())) {
				ModuleDescriptor md =  PomModuleDescriptorParser.getInstance().parseDescriptor(ivy.getSettings(), ivyfile.toURI().toURL(), true);
				report = ivy.resolve(md, resolveOptions);
			} else {
				report = ivy.resolve(ivyfile.toURI().toURL(), resolveOptions);
			}
		} catch (Exception e) {
			throw new IvyClasspathException("Unable to resolve dependencies for file " + ivyfile.getAbsolutePath(), e);
		}

        if (report.hasError()) {
            throw new IvyClasspathException(report.getAllProblemMessages());
        } else {
        	List<File> result = new ArrayList<File>(report.getAllArtifactsReports().length);
        	for (ArtifactDownloadReport adr: report.getAllArtifactsReports()) {
        		result.add(adr.getLocalFile());
        	}
        	return result;
        }
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
}
