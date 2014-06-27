package fitnesse.wikitext.widgets;

import fitnesse.wikitext.parser.*;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.m2.PomModuleDescriptorParser;
import util.Maybe;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    // Properties put on the "current" symbol
    static final String IS_POM_XML = "IS_POM_XML";
    private static final String PARSE_ERROR = "PARSE_ERROR";

    private final Map<String, SoftReference<CacheElement>> cache = new ConcurrentHashMap<String, SoftReference<CacheElement>>();

    // OptionType is used to identify child symbols
    enum OptionType {
		DEPENDENCY_FILE("ivy.xml"),
		IVY_SETTINGS_XML(null),
		CONFIGURATION("*");

        private final String defaultValue;

        private OptionType(final String defaultValue) {
            this.defaultValue = defaultValue;
        }
        public Maybe<Symbol> fromSymbol(final Symbol symbol) {
            for (Symbol child : symbol.getChildren()) {
                if (child.hasProperty(this.name())) {
                    return new Maybe<Symbol>(child);
                }
            }
            return defaultValue != null ? new Maybe<Symbol>(new Symbol(SymbolType.Text, defaultValue)) : Symbol.nothing;
        }
	};

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
			classpath = getClasspathElements(translator, symbol);
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
        Maybe<Symbol> dependencyFile = OptionType.DEPENDENCY_FILE.fromSymbol(symbol);
        Maybe<Symbol> ivySettingsXml = OptionType.IVY_SETTINGS_XML.fromSymbol(symbol);
        Maybe<Symbol> configuration = OptionType.CONFIGURATION.fromSymbol(symbol);

        buf.append("<p class='meta'>Classpath from \"")
        	.append(translator.translate(dependencyFile.getValue()))
        	.append("\"");

        if (!ivySettingsXml.isNothing()) {
            buf.append(", with settings file \"")
        		.append(translator.translate(ivySettingsXml.getValue()))
        		.append("\"");
        }

        buf.append(" and configuration \"")
    		.append(translator.translate(configuration.getValue()))
    		.append("\"")
            .append(":</p><ul class='meta'>");

        if (symbol.hasProperty(PARSE_ERROR)) {
 			buf.append("<li class='error'>")
				.append(symbol.getProperty(PARSE_ERROR))
				.append("</li>");
        } else {
	        try {
	 			for (File dep: getClasspathElements(translator, symbol)) {
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
            	nextOption = OptionType.IVY_SETTINGS_XML;
            } else if ("-c".equals(option.getContent())) {
            	nextOption = OptionType.CONFIGURATION;
            } else if ("-pom".equals(option.getContent())) {
            	symbol.putProperty(IS_POM_XML, "true");
            } else {
//            	if (nextOption.fromSymbol(symbol) != Symbol.nothing) {
//            		symbol.putProperty(PARSE_ERROR, "Syntax error: Configuration option " + nextOption.name() + " is already defined.");
//            		break;
//            	}
                option.putProperty(nextOption.name(), "");
            	symbol.add(option);
            	nextOption = OptionType.DEPENDENCY_FILE;
            }
        }

        return new Maybe<Symbol>(symbol);
	}

    @SuppressWarnings("unchecked")
	List<File> getClasspathElements(Translator translator, Symbol symbol) throws IvyClasspathException {
        Maybe<Symbol> dependencyFile = OptionType.DEPENDENCY_FILE.fromSymbol(symbol);
        Maybe<Symbol> ivySettingsXml = OptionType.IVY_SETTINGS_XML.fromSymbol(symbol);
        Maybe<Symbol> configuration = OptionType.CONFIGURATION.fromSymbol(symbol);

        Ivy ivy = Ivy.newInstance();
        initMessage(ivy);
        File settingsFile = initSettings(ivy, translator, ivySettingsXml);
        File ivyFile = new File(ivy.getSettings().substitute(translator.translate(dependencyFile.getValue())));
        String configs = translator.translate(configuration.getValue());

        String cacheKey = createCacheKey(settingsFile, ivyFile, configs);
        System.out.println("Cache key is: " + cacheKey);
        if (cache.containsKey(cacheKey)) {
            CacheElement element = cache.get(cacheKey).get();
            if (element != null && !element.modified()) {
                return element.dependencies;
            }
        }


        if (!ivyFile.exists()) {
            throw new IvyClasspathException("Ivy/pom file not found: " + ivyFile);
        } else if (ivyFile.isDirectory()) {
            throw new IvyClasspathException("Ivy/pom file is not a file: " + ivyFile);
        }

        ResolveOptions resolveOptions = new ResolveOptions()
        		.setConfs(configs.split(","))
                .setValidate(true);
        ResolveReport report;
		try {
			if (symbol.hasProperty(IS_POM_XML)) {
				ModuleDescriptor md =  PomModuleDescriptorParser.getInstance().parseDescriptor(ivy.getSettings(), ivyFile.toURI().toURL(), true);
				report = ivy.resolve(md, resolveOptions);
			} else {
				report = ivy.resolve(ivyFile.toURI().toURL(), resolveOptions);
			}
		} catch (Exception e) {
			throw new IvyClasspathException("Unable to resolve dependencies for file " + ivyFile.getAbsolutePath(), e);
		}

        if (report.hasError()) {
            throw new IvyClasspathException(report.getAllProblemMessages());
        } else {
        	List<File> dependencies = new ArrayList<File>(report.getAllArtifactsReports().length);
        	for (ArtifactDownloadReport adr: report.getAllArtifactsReports()) {
        		dependencies.add(adr.getLocalFile());
        	}

            cache.put(cacheKey, new SoftReference<CacheElement>(new CacheElement(settingsFile, ivyFile, dependencies)));

            return dependencies;
        }
    }

    private String createCacheKey(File settingsFile, File ivyFile, String configs) {
        return (ivyFile != null ? ivyFile.getAbsolutePath() : "**") + "#" +
                (settingsFile != null ? settingsFile.getAbsolutePath() : "**") + "#" +
                (configs != null ? configs : "**");
    }

    private static void initMessage(Ivy ivy) {
            ivy.getLoggerEngine().pushLogger(new IvyClasspathMessageLogger());
    }

    private static File initSettings(Ivy ivy, Translator translator, Maybe<Symbol> ivySettingsXml)
            throws IvyClasspathException {
        IvySettings settings = ivy.getSettings();
        settings.addAllVariables(System.getProperties());
        File settingsFile = null;
        if (ivySettingsXml.isNothing()) {
        	try {
        		ivy.configureDefault();
			} catch (Exception e) {
				throw new IvyClasspathException("Unable to set default configuration", e);
			}
        } else {
            settingsFile = new File(translator.translate(ivySettingsXml.getValue()));
            if (!settingsFile.exists()) {
                throw new IvyClasspathException("Ivy configuration file not found: " + settingsFile);
            } else if (settingsFile.isDirectory()) {
            	throw new IvyClasspathException("Ivy configuration file is not a file: " + settingsFile);
            }
            try {
				ivy.configure(settingsFile);
			} catch (Exception e) {
				throw new IvyClasspathException("Unable to configure ivy with file " + settingsFile.getAbsolutePath(), e);
			}
        }
        return settingsFile;
    }

    private static class CacheElement {

        private final List<File> dependencies;
        private final File ivyFile;
        private File settingsFile;
        private final long ivyFileLastModified;
        private final long settingsFileLastModified;

        private CacheElement(File settingsFile, File ivyFile, List<File> dependencies) {
            this.dependencies = dependencies;
            this.ivyFile = ivyFile;
            this.settingsFile = settingsFile;
            this.ivyFileLastModified = ivyFile.lastModified();
            this.settingsFileLastModified = settingsFile != null ? settingsFile.lastModified() : 0;
        }

        private boolean modified() {
            return (ivyFile.lastModified() != ivyFileLastModified) &&
                    (settingsFile == null || settingsFile.lastModified() != settingsFileLastModified);
        }
    }
}
