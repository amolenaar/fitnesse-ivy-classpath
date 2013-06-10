package fitnesse.wikitext.widgets;

import java.io.File;
import java.util.List;

import fitnesse.wikitext.parser.*;
import org.junit.BeforeClass;
import org.junit.Test;

import fitnesse.wikitext.test.ParserTestHelper;
import util.Maybe;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

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
        ParserTestHelper.assertParses("!resolve", "SymbolList[IvyClasspathSymbolType]");
        ParserTestHelper.assertParses("!resolve -s mysettings.xml", "SymbolList[IvyClasspathSymbolType[Text]]");
        ParserTestHelper.assertParses("!resolve -c config1,config2 ivy.xml", "SymbolList[IvyClasspathSymbolType[Text, Comma, Text, Text]]");
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
        String html = ParserTestHelper.translateToHtml(null, pageContents, mockVariableSource);
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
        String html = ParserTestHelper.translateToHtml(null, pageContents, variableSource);
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
        String html = ParserTestHelper.translateToHtml(null, pageContents, variableSource);
        assertTrue(html, html.startsWith("<p class='meta'>Classpath from \"ivy.xml\", with settings file \"ivysettings.xml\" and configuration \"default\":</p>"));
    }

    private Symbol optionSymbol(IvyClasspathSymbolType.OptionType configuration, String value) {
        return new Symbol(SymbolType.Text, value).putProperty(configuration.name(), "");
    }
}
