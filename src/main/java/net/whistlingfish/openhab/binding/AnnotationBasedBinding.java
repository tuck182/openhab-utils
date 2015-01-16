package net.whistlingfish.openhab.binding;

import org.openhab.core.binding.AbstractBinding;
import org.openhab.core.binding.BindingProvider;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

public class AnnotationBasedBinding<//
C extends AnnotationBasedBindingConfig<C, B, P>, //
B extends AnnotationBasedBinding<C, B, P>, //
P extends AnnotationBasedBindingProvider<C, B, P>> extends AbstractBinding<P> {
    protected volatile boolean properlyConfigured = false;

    protected Class<B> bindingClass;
    protected Class<P> providerClass;

    public AnnotationBasedBinding(Class<B> bindingClass, Class<P> providerClass) {
        this.bindingClass = bindingClass;
        this.providerClass = providerClass;
    }

    public void setProperlyConfigured(boolean properlyConfigured) {
        boolean updateBindings = !this.properlyConfigured && properlyConfigured;
        this.properlyConfigured = properlyConfigured;
        if (updateBindings) {
            allBindingsChanged();
        }
    }

    /**
     * @{inheritDoc
     */
    @Override
    public void bindingChanged(BindingProvider provider, String itemName) {
        super.bindingChanged(provider, itemName);
        if (!properlyConfigured) {
            return;
        }
        if (provider != null && providerClass.isAssignableFrom(provider.getClass())) {
            providerClass.cast(provider).bind(bindingClass.cast(this), itemName);
        }
    }

    protected void allBindingsChanged() {
        for (BindingProvider provider : providers) {
            allBindingsChanged(provider);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void allBindingsChanged(BindingProvider provider) {
        super.allBindingsChanged(provider);

        for (String itemName : provider.getItemNames()) {
            bindingChanged(provider, itemName);
        }
    }

    public void postUpdate(String itemName, State state) {
        eventPublisher.postUpdate(itemName, state);
    }

    /**
     * @{inheritDoc
     */
    @Override
    protected void internalReceiveCommand(String itemName, Command command) {
        if (!properlyConfigured) {
            return;
        }
        for (P provider : providers) {
            if (provider.providesBindingFor(itemName)) {
                provider.receiveCommand(bindingClass.cast(this), itemName, command);
            }
        }
    }

    /**
     * @{inheritDoc
     */
    @Override
    protected void internalReceiveUpdate(String itemName, State newState) {
        if (!properlyConfigured) {
            return;
        }
        for (P provider : providers) {
            if (provider.providesBindingFor(itemName)) {
                provider.receiveUpdate(bindingClass.cast(this), itemName, newState);
            }
        }
    }
}
