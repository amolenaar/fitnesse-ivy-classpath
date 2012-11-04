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
        ParserTestHelper.assertParses("!ivy", "SymbolList[IvyClasspathSymbolType]");
        ParserTestHelper.assertParses("!ivy -s mysettings.xml", "SymbolList[IvyClasspathSymbolType]");
        ParserTestHelper.assertParses("!ivy -c config1,config2 ivy.xml", "SymbolList[IvyClasspathSymbolType]");
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

}
