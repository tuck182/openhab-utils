package net.whistlingfish.openhab.binding.parser;

import org.openhab.model.item.binding.BindingConfigParseException;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

import static java.lang.String.format;

public class BindingConfigParser {
    private final BindingConfigParboiled parser;
    private final ReportingParseRunner<BindingParseComponent> parserRunner;

    public BindingConfigParser() {
        ClassLoader saved = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            parser = Parboiled.createParser(BindingConfigParboiled.class);
            parserRunner = new ReportingParseRunner<BindingParseComponent>(
                    parser.bindingList());
        } finally {
            Thread.currentThread().setContextClassLoader(saved);
        }
    }

    public BindingParseResult parseBindingConfig(String bindingConfig) throws BindingConfigParseException {
        ParsingResult<BindingParseComponent> result = parserRunner.run(bindingConfig);
        BindingParseResult parseResult = (BindingParseResult) result.resultValue;
        if (parseResult == null || parseResult.getConfigCount() < 1) {
            throw new BindingConfigParseException(format("Invalid binding definition '%s'", bindingConfig));
        }
        return parseResult;
    }
}
