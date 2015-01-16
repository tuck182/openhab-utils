package net.whistlingfish.openhab.binding.parser;

import net.whistlingfish.openhab.binding.BindingDirection;
import static net.whistlingfish.openhab.binding.BindingDirection.OUT;

public class DirectionOutbound implements Direction {
    @Override
    public BindingDirection get() {
        return OUT;
    }
}
