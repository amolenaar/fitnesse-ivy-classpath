package fitnesse.wikitext.widgets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import fitnesse.wikitext.parser.Symbol;
import fitnesse.wikitext.parser.SymbolProvider;
import fitnesse.wikitext.test.ParserTestHelper;

public class IvyClasspathSymbolTypeTest {

	static final IvyClasspathSymbolType symbolType = new IvyClasspathSymbolType();

	@BeforeClass
	public static void registerIvyClasspathSymbolType() {
		SymbolProvider.wikiParsingProvider.add(symbolType);
	}

    @Test
    public void parsesIvy() throws Exception {
        ParserTestHelper.assertParses("!resolve", "SymbolList[IvyClasspathSymbolType]");
        ParserTestHelper.assertParses("!resolve -s mysettings.xml", "SymbolList[IvyClasspathSymbolType]");
        ParserTestHelper.assertParses("!resolve -c config1,config2 ivy.xml", "SymbolList[IvyClasspathSymbolType]");
    }

    @Test
    public void testInvalidIvySettingsFile() {
    	Symbol symbol = new Symbol(symbolType);
    	symbol.putProperty(IvyClasspathSymbolType.OptionType.IVYSETTINGS_XML.name(), "ivysettings-not-there.xml");

    	Exception exc = null;
    	try {
    		symbolType.getClasspathElements(symbol);
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
    	//symbol.putProperty(IvyClasspathSymbolType.OptionType.IVYSETTINGS_XML.name(), "ivysettings-test.xml");

    	 List<File> classpath = symbolType.getClasspathElements(symbol);
    	assertEquals(5, classpath.size());
    	for (File f: classpath) {
    		System.out.println(f.getAbsolutePath());
    	}
    }

    @Test
    public void testIvyParsingWithConfigurations() throws IvyClasspathException {
    	Symbol symbol = new Symbol(symbolType);
    	symbol.putProperty(IvyClasspathSymbolType.OptionType.CONFIGURATION.name(), "default,test");

    	List<File> classpath = symbolType.getClasspathElements(symbol);
    	assertEquals(5, classpath.size());
    	for (File f: classpath) {
    		System.out.println(f.getAbsolutePath());
    	}
    }

    @Test
    public void testIvyParsingWithOneConfiguration() throws IvyClasspathException {
    	Symbol symbol = new Symbol(symbolType);
    	symbol.putProperty(IvyClasspathSymbolType.OptionType.CONFIGURATION.name(), "default");

    	List<File> classpath = symbolType.getClasspathElements(symbol);
    	assertEquals(2, classpath.size());
    	for (File f: classpath) {
    		System.out.println(f.getAbsolutePath());
    	}
    }

    @Test
    public void testIvyParsingWithSettings() throws IvyClasspathException {
    	Symbol symbol = new Symbol(symbolType);
    	symbol.putProperty(IvyClasspathSymbolType.OptionType.IVYSETTINGS_XML.name(), "ivysettings-test.xml");

    	List<File> classpath = symbolType.getClasspathElements(symbol);
    	assertEquals(5, classpath.size());
    	for (File f: classpath) {
    		System.out.println(f.getAbsolutePath());
    	}
    }

    @Test
    public void testIvyParsingOfPomFile() throws IvyClasspathException {
    	Symbol symbol = new Symbol(symbolType);
    	symbol.putProperty(IvyClasspathSymbolType.OptionType.IS_POM_XML.name(), "true");
    	symbol.putProperty(IvyClasspathSymbolType.OptionType.DEPENDENCY_FILE.name(), "pom-test.xml");

    	List<File> classpath = symbolType.getClasspathElements(symbol);
    	assertEquals(3, classpath.size());
    	for (File f: classpath) {
    		System.out.println(f.getAbsolutePath());
    	}
    }

    @Test
    public void testIvyParsingOfPomFileWithConfigurations() throws IvyClasspathException {
    	Symbol symbol = new Symbol(symbolType);
    	symbol.putProperty(IvyClasspathSymbolType.OptionType.IS_POM_XML.name(), "true");
    	symbol.putProperty(IvyClasspathSymbolType.OptionType.DEPENDENCY_FILE.name(), "pom-test.xml");
    	symbol.putProperty(IvyClasspathSymbolType.OptionType.CONFIGURATION.name(), "compile");

    	List<File> classpath = symbolType.getClasspathElements(symbol);
    	assertEquals(1, classpath.size());
    	for (File f: classpath) {
    		System.out.println(f.getAbsolutePath());
    	}
    }

}
