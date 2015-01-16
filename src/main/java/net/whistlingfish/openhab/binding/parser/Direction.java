package net.whistlingfish.openhab.binding.parser;

import net.whistlingfish.openhab.binding.BindingDirection;

public interface Direction extends BindingParseComponent {
    BindingDirection get();
}
