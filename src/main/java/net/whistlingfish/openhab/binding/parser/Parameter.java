package net.whistlingfish.openhab.binding.parser;


public class Parameter implements BindingParseComponent {
    private String str;

    public Parameter(String str) {
        this.str = str;
    }

    public String text() {
        return str;
    }

    @Override
    public String toString() {
        return text();
    }

}
