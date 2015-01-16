package net.whistlingfish.openhab.binding.parser;

public class ParseResultAndRemainder {
    private ParseResult parseResult;
    private String remainder;

    public ParseResult getParseResult() {
        return parseResult;
    }

    public void setParseResult(ParseResult parseResult) {
        this.parseResult = parseResult;
    }

    public String getRemainder() {
        return remainder;
    }

    public void setRemainder(String remainder) {
        this.remainder = remainder;
    }
}
