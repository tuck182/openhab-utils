package net.whistlingfish.openhab.binding.parser;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import static com.google.common.collect.Lists.newArrayList;

public class ParsedBindingConfig implements BindingParseComponent {
    private final String text;
    private final Direction direction;
    private final List<Parameter> parameters = new ArrayList<>();

    public ParsedBindingConfig(String text, ParsedBindingConfig config) {
        this.text = text;
        this.direction = config.direction;
        this.parameters.addAll(config.parameters);
    }

    public ParsedBindingConfig(Direction direction, Parameter parameter) {
        this.text = null;
        this.direction = direction;
        parameters.add(parameter);
    }

    public ParsedBindingConfig(ParsedBindingConfig config, Parameter parameter) {
        this.text = null;
        this.direction = config.direction;
        this.parameters.addAll(config.parameters);
        this.parameters.add(parameter);
    }

    public Direction getDirection() {
        return direction;
    }

    public List<String> getParameters() {
        return newArrayList(Collections2.transform(parameters, new Function<Parameter, String>() {
            @Override
            public String apply(Parameter input) {
                return input.text();
            }
        }));
    }

    public String getText() {
        return text;
    }

    public ParsedBindingConfig withText(String text) {
        return new ParsedBindingConfig(text, this);
    }
}
