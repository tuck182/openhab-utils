package net.whistlingfish.openhab.binding;

import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

public abstract class AnnotationBasedBindingConfig<//
C extends AnnotationBasedBindingConfig<C, B, P>, //
B extends AnnotationBasedBinding<C, B, P>, //
P extends AnnotationBasedBindingProvider<C, B, P>>
        implements BindingConfig {
    protected Item item;

    public void bind(B binding) {
    }

    public void receiveCommand(B binding, Command command) {
    }

    public void receiveUpdate(B binding, State newState) {
    }
}
