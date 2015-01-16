package net.whistlingfish.openhab.binding.parser;

import net.whistlingfish.openhab.binding.BindingDirection;
import static net.whistlingfish.openhab.binding.BindingDirection.IN;

public class DirectionInbound implements Direction {
    @Override
    public BindingDirection get() {
        return IN;
    }
}
