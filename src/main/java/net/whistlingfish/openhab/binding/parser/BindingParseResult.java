package net.whistlingfish.openhab.binding.parser;

import java.util.ArrayList;
import java.util.List;

public class BindingParseResult implements BindingParseComponent {
    private List<ParsedBindingConfig> parsedBindingConfigs = new ArrayList<ParsedBindingConfig>();

    public BindingParseResult() {
        // default constructor
    }

    public BindingParseResult(BindingParseResult other, ParsedBindingConfig binding) {
        parsedBindingConfigs.addAll(other.parsedBindingConfigs);
        parsedBindingConfigs.add(binding);
    }

    public int getConfigCount() {
        return parsedBindingConfigs.size();
    }

    public ParsedBindingConfig getConfig(int i) {
        return parsedBindingConfigs.get(i);
    }

    public List<ParsedBindingConfig> getConfigs() {
        return parsedBindingConfigs;
    }
}
