package net.whistlingfish.openhab.binding;

import net.whistlingfish.openhab.binding.parser.BindingConfigParser;
import net.whistlingfish.openhab.binding.parser.BindingParseResult;
import net.whistlingfish.openhab.binding.parser.ParsedBindingConfig;

import org.openhab.core.binding.BindingProvider;
import org.openhab.core.items.Item;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;

public abstract class AnnotationBasedBindingProvider<//
C extends AnnotationBasedBindingConfig<C, B, P>, //
B extends AnnotationBasedBinding<C, B, P>, //
P extends AnnotationBasedBindingProvider<C, B, P>> extends AbstractGenericBindingProvider implements BindingProvider {

    private BindingConfigParser parser = new BindingConfigParser();
    private Class<C> bindingConfigClass;
    private AnnotatedBindingDetails<C> bindingDetails;

    @SafeVarargs
    public AnnotationBasedBindingProvider(Class<C> bindingConfigClass, Class<? extends C>... bindingClasses) {
        this.bindingConfigClass = bindingConfigClass;
        this.bindingDetails = new AnnotatedBindingDetails<C>(bindingClasses);
    }

    @Override
    public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processBindingConfiguration(String context, Item item, String bindingConfig)
            throws BindingConfigParseException {
        super.processBindingConfiguration(context, item, bindingConfig);

        BindingParseResult parseResult = parser.parseBindingConfig(bindingConfig);
        for (ParsedBindingConfig config : parseResult.getConfigs()) {
            addBindingConfig(item, bindingDetails.createBindingConfig(item, config.getDirection().get(),
                    config.getParameters(), config.getText()));
        }
    }

    public void bind(B binding, String itemName) {
        C bindingConfig = bindingConfigClass.cast(bindingConfigs.get(itemName));
        if (bindingConfig != null) {
            bindingConfig.bind(binding);
        }
    }

    public void receiveCommand(B binding, String itemName, Command command) {
        C bindingConfig = bindingConfigClass.cast(bindingConfigs.get(itemName));
        if (bindingConfig != null) {
            bindingConfig.receiveCommand(binding, command);
        }
    }

    public void receiveUpdate(B binding, String itemName, State newState) {
        C bindingConfig = bindingConfigClass.cast(bindingConfigs.get(itemName));
        if (bindingConfig != null) {
            bindingConfig.receiveUpdate(binding, newState);
        }
    }
}
