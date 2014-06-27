package fitnesse.wikitext.widgets;

import java.io.File;
import java.util.List;

import fitnesse.wiki.PageData;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageUtil;
import fitnesse.wiki.mem.InMemoryPage;
import fitnesse.wikitext.parser.*;
import org.junit.BeforeClass;
import org.junit.Test;

import util.Maybe;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class IvyClasspathSymbolTypeTest {

	static final IvyClasspathSymbolType symbolType = new IvyClasspathSymbolType();

    VariableSource mockVariableSource = new VariableSource() {

        @Override
        public Maybe<String> findVariable(String s) {
            throw new AssertionError("Should not be called");
        }
    };

    Translator mockTranslator = new Translator(null) {

        @Override
        public String translate(Symbol symbol) {
            return symbol.getContent();
        }

        @Override
        protected Translation getTranslation(SymbolType symbolType) {
            return null;
        }
    };

    @BeforeClass
	public static void registerIvyClasspathSymbolType() {
		SymbolProvider.wikiParsingProvider.add(symbolType);
	}

    @Test
    public void parsesIvy() throws Exception {
        assertParses("!resolve", "SymbolList[IvyClasspathSymbolType]");
        assertParses("!resolve -s mysettings.xml", "SymbolList[IvyClasspathSymbolType[Text]]");
        assertParses("!resolve -c config1,config2 ivy.xml", "SymbolList[IvyClasspathSymbolType[Text, Comma, Text, Text]]");
    }

    @Test
    public void testInvalidIvySettingsFile() {
    	Symbol symbol = new Symbol(symbolType);
    	symbol.add(optionSymbol(IvyClasspathSymbolType.OptionType.IVY_SETTINGS_XML, "ivysettings-not-there.xml"));

    	Exception exc = null;
    	try {
            symbolType.getClasspathElements(mockTranslator, symbol);
    	} catch (IvyClasspathException e) {
    		exc = e;
    	}
    	assertNotNull(exc);
    	assertEquals("Ivy configuration file not found: ivysettings-not-there.xml",
    			exc.getMessage());
    }

    @Test
    public void testIvyParsing() throws IvyClasspathException {
    	Symbol symbol = new Symbol(symbolType);

   	    List<File> classpath = symbolType.getClasspathElements(mockTranslator, symbol);
    	assertEquals(11, classpath.size());
    	for (File f: classpath) {
    		System.out.println(f.getAbsolutePath());
    	}
    }

    @Test
    public void testIvyParsingWithConfigurations() throws IvyClasspathException {
    	Symbol symbol = new Symbol(symbolType);
    	symbol.add(optionSymbol(IvyClasspathSymbolType.OptionType.CONFIGURATION, "default,test"));

    	List<File> classpath = symbolType.getClasspathElements(mockTranslator, symbol);
    	assertEquals(11, classpath.size());
    	for (File f: classpath) {
    		System.out.println(f.getAbsolutePath());
    	}
    }

    @Test
    public void testIvyParsingWithOneConfiguration() throws IvyClasspathException {
    	Symbol symbol = new Symbol(symbolType);
    	symbol.add(optionSymbol(IvyClasspathSymbolType.OptionType.CONFIGURATION, "default"));

    	List<File> classpath = symbolType.getClasspathElements(mockTranslator, symbol);
    	assertEquals(9, classpath.size());
    	for (File f: classpath) {
    		System.out.println(f.getAbsolutePath());
    	}
    }

    @Test
    public void testIvyParsingWithSettings() throws IvyClasspathException {
    	Symbol symbol = new Symbol(symbolType);
    	symbol.add(optionSymbol(IvyClasspathSymbolType.OptionType.IVY_SETTINGS_XML, "ivysettings-test.xml"));

    	List<File> classpath = symbolType.getClasspathElements(mockTranslator, symbol);
    	assertEquals(11, classpath.size());
    	for (File f: classpath) {
    		System.out.println(f.getAbsolutePath());
    	}
    }

    @Test
    public void testIvyParsingOfPomFile() throws IvyClasspathException {
    	Symbol symbol = new Symbol(symbolType);
    	symbol.putProperty(IvyClasspathSymbolType.IS_POM_XML, "true");
    	symbol.add(optionSymbol(IvyClasspathSymbolType.OptionType.DEPENDENCY_FILE, "pom-test.xml"));

    	List<File> classpath = symbolType.getClasspathElements(mockTranslator, symbol);
    	assertEquals(3, classpath.size());
    	for (File f: classpath) {
    		System.out.println(f.getAbsolutePath());
    	}
    }

    @Test
    public void testIvyParsingOfPomFileWithConfigurations() throws IvyClasspathException {
    	Symbol symbol = new Symbol(symbolType);

        // !resolve -c compile -pom pom-test.xml
    	symbol.putProperty(IvyClasspathSymbolType.IS_POM_XML, "true");
    	symbol.add(optionSymbol(IvyClasspathSymbolType.OptionType.DEPENDENCY_FILE, "pom-test.xml"));
    	symbol.add(optionSymbol(IvyClasspathSymbolType.OptionType.CONFIGURATION, "compile"));

    	List<File> classpath = symbolType.getClasspathElements(mockTranslator, symbol);
    	assertEquals(1, classpath.size());
    	for (File f: classpath) {
    		System.out.println(f.getAbsolutePath());
    	}
    }

    @Test
    public void loadIvyXml() throws Exception {
        String pageContents = "!resolve ivy.xml\n";
        String html = translateToHtml(null, pageContents, mockVariableSource);
        assertTrue(html, html.startsWith("<p class='meta'>Classpath from \"ivy.xml\" and configuration \"*\":</p>"));
    }

    @Test
    public void loadIvyXmlFromVariable() throws Exception {
        String pageContents = "!resolve -c config1 -s ivysettings.xml ${IVY_XML}\n";
        VariableSource variableSource = new VariableSource() {

            @Override
            public Maybe<String> findVariable(String s) {
                assertThat(s, is("IVY_XML"));
                return new Maybe<String>("ivy.xml");
            }
        };
        String html = translateToHtml(null, pageContents, variableSource);
        assertTrue(html, html.startsWith("<p class='meta'>Classpath from \"ivy.xml\", with settings file \"ivysettings.xml\" and configuration \"config1\":</p>"));
    }

    @Test
    public void loadIvyXmlWithConfigs() throws Exception {
        String pageContents = "!resolve -c default -s ivysettings.xml ${IVY_XML}\n";
        VariableSource variableSource = new VariableSource() {

            @Override
            public Maybe<String> findVariable(String s) {
                assertThat(s, is("IVY_XML"));
                return new Maybe<String>("ivy.xml");
            }
        };
        String html = translateToHtml(null, pageContents, variableSource);
        assertTrue(html, html.startsWith("<p class='meta'>Classpath from \"ivy.xml\", with settings file \"ivysettings.xml\" and configuration \"default\":</p>"));
    }

    private Symbol optionSymbol(IvyClasspathSymbolType.OptionType configuration, String value) {
        return new Symbol(SymbolType.Text, value).putProperty(configuration.name(), "");
    }

    // Test code lend from fitnesse.wikitext.parser.ParserTestHelper
    public static String translateToHtml(WikiPage page, String input, VariableSource variableSource) {
        Symbol list = Parser.make(new ParsingPage(new WikiSourcePage(page), variableSource), input, SymbolProvider.wikiParsingProvider).parse();
        return new HtmlTranslator(new WikiSourcePage(page), new ParsingPage(new WikiSourcePage(page))).translateTree(list);
    }

    public static void assertParses(String input, String expected) throws Exception {
        WikiPage page = new TestRoot().makePage("TestPage", input);
        Symbol result = parse(page, input);
        assertEquals(expected, serialize(result));
    }

    public static Symbol parse(WikiPage page, String input) {
        return Parser.make(new ParsingPage(new WikiSourcePage(page)), input).parse();
    }

    public static String serialize(Symbol symbol) {
        StringBuilder result = new StringBuilder();
        result.append(symbol.getType() != null ? symbol.getType().toString() : "?no type?");
        int i = 0;
        for (Symbol child : symbol.getChildren()) {
            result.append(i == 0 ? "[" : ", ");
            result.append(serialize(child));
            i++;
        }
        if (i > 0) result.append("]");
        return result.toString();
    }

    public static class TestRoot {
        public WikiPage root;

        public TestRoot() {
            root = InMemoryPage.makeRoot("root");
        }

        public WikiPage makePage(String pageName) {
            return makePage(root, pageName);
        }

        public WikiPage makePage(WikiPage parent, String pageName) {
            return WikiPageUtil.addPage(parent, PathParser.parse(pageName), "");
        }

        public WikiPage makePage(String pageName, String content) {
            return makePage(root, pageName, content);
        }

        public WikiPage makePage(WikiPage parent, String pageName, String content) {
            WikiPage page = makePage(parent, pageName);
            setPageData(page, content);
            return page;
        }

        public void setPageData(WikiPage page, String content) {
            PageData data = page.getData();
            data.setContent(content);
            page.commit(data);
        }
    }

}
