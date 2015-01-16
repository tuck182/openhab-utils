package net.whistlingfish.openhab.binding.parser;


public class StringSegment implements BindingParseComponent {
    private final String str;

    public StringSegment(String str) {
        this.str = str;
    }

    public String text() {
        return str;
    }
}
