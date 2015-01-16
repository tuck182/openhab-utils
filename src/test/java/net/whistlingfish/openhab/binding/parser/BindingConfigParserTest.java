package net.whistlingfish.openhab.binding.parser;

import org.junit.Test;
import org.openhab.model.item.binding.BindingConfigParseException;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertThat;

public class BindingConfigParserTest {

    private BindingConfigParser parser = new BindingConfigParser();

    @Test
    public void parserPreservesConfigString() throws Exception {
        String config1 = "<['one:two':three]";
        String config2 = ">[four:\"five:six\"]";
        BindingParseResult result = parser.parseBindingConfig(config1 + "  " + config2);

        assertThat(result.getConfig(0).getText(), is(config1));
        assertThat(result.getConfig(1).getText(), is(config2));
    }

    @Test
    public void splitConfigSeparatesComponents() throws Exception {
        ParsedBindingConfig elements = getElements(parser.parseBindingConfig("<[one:two:three]"), 0);
        assertThat(newArrayList(elements.getParameters()), is(newArrayList("one", "two", "three")));
    }

    @Test
    public void splitConfigSupportsSinqleQuotedStrings() throws Exception {
        ParsedBindingConfig elements = getElements(parser.parseBindingConfig("<['one:[two]':three]"), 0);
        assertThat(newArrayList(elements.getParameters()), is(newArrayList("one:[two]", "three")));
    }

    @Test
    public void splitConfigSupportsDoubleQuotedStrings() throws Exception {
        ParsedBindingConfig elements = getElements(parser.parseBindingConfig("<[one:\"two:[three]\"]"), 0);
        assertThat(newArrayList(elements.getParameters()), is(newArrayList("one", "two:[three]")));
    }

    @Test
    public void splitConfigHandlesMultipleEntries() throws Exception {
        BindingParseResult result = parser.parseBindingConfig("<[one:two:three] >[four:five:six]");
        ParsedBindingConfig elements = getElements(result, 0);
        assertThat(newArrayList(elements.getParameters()), is(newArrayList("one", "two", "three")));

        elements = getElements(result, 1);
        assertThat(newArrayList(elements.getParameters()), is(newArrayList("four", "five", "six")));
    }

    @Test(expected = BindingConfigParseException.class)
    public void bindingDefintionMustNotBeEmpty() throws Exception {
        parser.parseBindingConfig("<[]");
    }

    @Test(expected = BindingConfigParseException.class)
    public void multipleEntriesMustBeSeparatedByWhitespace() throws Exception {
        parser.parseBindingConfig("<[one:two:three]>[four:five:six]");
    }

    @Test(expected = BindingConfigParseException.class)
    public void bindingDefintionMustStartWithAngle() throws Exception {
        parser.parseBindingConfig("[one:two:three]");
    }

    @Test(expected = BindingConfigParseException.class)
    public void bindingDefintionMustStartWithAngleThenBracket() throws Exception {
        parser.parseBindingConfig("<one:two:three]");
    }

    @Test(expected = BindingConfigParseException.class)
    public void bindingDefintionMustEndWithBracket() throws Exception {
        parser.parseBindingConfig("<[one:two:three");
    }

    @Test(expected = BindingConfigParseException.class)
    public void firstOfMultipleEntriesMustEndWithBracket() throws Exception {
        BindingParseResult parseResult = parser.parseBindingConfig("<[one:two:three >[four:five:six]");
        parseResult.toString();
    }

    private ParsedBindingConfig getElements(BindingParseResult result, int i) throws Exception {
        return result.getConfig(i);
    }
}
