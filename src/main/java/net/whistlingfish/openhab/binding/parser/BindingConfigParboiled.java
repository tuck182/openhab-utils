package net.whistlingfish.openhab.binding.parser;

import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.support.StringBuilderVar;

public class BindingConfigParboiled extends BaseParser<BindingParseComponent> {
    public Rule bindingList() {
        return Sequence(push(new BindingParseResult()), //
                binding(), //
                push(new BindingParseResult(asResult(pop(1)), asBinding(pop()).withText(match()))), //
                ZeroOrMore(whitespace(), //
                        binding(), //
                        push(new BindingParseResult(asResult(pop(1)), asBinding(pop()).withText(match())))), //
                EOI);
    }

    public Rule whitespace() {
        return OneOrMore(AnyOf(" \t"));
    }

    public Rule binding() {
        return Sequence(direction(), //
                '[', //
                parameter(), //
                push(new ParsedBindingConfig(asDirection(pop(1)), asParameter(pop()))), //
                ZeroOrMore(':', //
                        parameter(), //
                        push(new ParsedBindingConfig(asBinding(pop(1)), asParameter(pop())))), //
                ']');
    }

    public Rule direction() {
        return FirstOf(Sequence('<', push(new DirectionInbound())),//
                Sequence('>', push(new DirectionOutbound())));
    }

    public Rule parameter() {
        StringBuilderVar text = new StringBuilderVar();
        return Sequence(//
                OneOrMore(FirstOf( //
                        Sequence(singleQuotedString(), text.append(asStringSegment(pop()).text())), //
                        Sequence(doubleQuotedString(), text.append(asStringSegment(pop()).text())), //
                        Sequence(bareParameterString(), text.append(match()))//
                )), //
                push(new Parameter(text.getString()))//
        );
    }

    public Rule singleQuotedString() {
        return Sequence('\'', ZeroOrMore(NoneOf("'")), push(new StringSegment(match())), '\'');
    }

    public Rule doubleQuotedString() {
        return Sequence('"', ZeroOrMore(NoneOf("\"")), push(new StringSegment(match())), '"');
    }

    public Rule bareParameterString() {
        return OneOrMore(NoneOf("'\"[]<>:"));
    }

    // casts
    protected BindingParseResult asResult(BindingParseComponent arg) {
        return (BindingParseResult) arg;
    }

    protected ParsedBindingConfig asBinding(BindingParseComponent arg) {
        return (ParsedBindingConfig) arg;
    }

    protected Parameter asParameter(BindingParseComponent arg) {
        return (Parameter) arg;
    }

    protected StringSegment asStringSegment(BindingParseComponent arg) {
        return (StringSegment) arg;
    }

    protected Direction asDirection(BindingParseComponent arg) {
        return (Direction) arg;
    }
}