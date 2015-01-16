package net.whistlingfish.openhab.binding;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.support.StandardTypeConverter;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static net.whistlingfish.openhab.binding.BindingDirection.BOTH;
import static net.whistlingfish.openhab.binding.BindingDirection.IN;
import static net.whistlingfish.openhab.binding.BindingDirection.OUT;

public class AnnotatedBindingDetails<B extends BindingConfig> {
    private static final TypeConverter TYPE_CONVERTER = new StandardTypeConverter(new DefaultConversionService());

    private Map<BindingDirection, Map<String, BindingDescription>> bindingDescriptions;

    @SafeVarargs
    public AnnotatedBindingDetails(Class<? extends B>... bindingClasses) {
        this.bindingDescriptions = processBindingClasses(bindingClasses);
    }

    private Map<BindingDirection, Map<String, BindingDescription>> processBindingClasses(
            Class<? extends B>[] bindingClasses) {
        Map<BindingDirection, Map<String, BindingDescription>> result = new HashMap<>();
        for (BindingDirection direction : BindingDirection.values()) {
            result.put(direction, new HashMap<String, BindingDescription>());
        }

        for (Class<? extends B> clazz : bindingClasses) {
            BindingDescription r = processBindingClass(clazz);
            if (r.direction == BOTH) {
                result.get(IN).put(r.bindingName, r);
                result.get(OUT).put(r.bindingName, r);
            } else {
                result.get(r.direction).put(r.bindingName, r);
            }
        }
        return result;
    }

    private BindingDescription processBindingClass(Class<? extends B> clazz) {
        BindingConfigType type = AnnotationUtils.getAnnotation(clazz, BindingConfigType.class);
        if (type == null) {
            throw new IllegalStateException(format("BindingConfig class '%s' is missing @BindingConfigType annotation",
                    clazz.getSimpleName()));
        }
        if (type.name() == null) {
            throw new IllegalStateException(format("BindingConfig class '%s' is missing name", clazz.getSimpleName()));
        }

        BindingPropertiesHolder holder = new BindingPropertiesHolder();

        processFieldsAndMethods(clazz, holder);

        boolean seenOptional = false;
        BindingPropertySetter<B> previousProperty = null;
        for (BindingPropertySetter<B> property : holder.properties) {
            if (seenOptional && property.isRequired()) {
                throw new IllegalStateException(format(
                        "Property %s was marked as required, but previous property %s is optional",
                        property.getDescription(), previousProperty.getDescription()));
            }
            seenOptional = !property.isRequired();
            previousProperty = property;
        }
        return new BindingDescription(clazz, type.direction(), type.name(), holder.qualifier, holder.properties,
                holder.extras);
    }

    private class BindingPropertiesHolder {
        public BindingPropertySetter<B> qualifier;
        public BindingPropertySetter<B> extras;
        public List<BindingPropertySetter<B>> properties = new ArrayList<>();
        public int propertiesCount = 0;
    }

    private void processFieldsAndMethods(Class<? extends B> clazz, BindingPropertiesHolder holder) {

        for (Field field : FieldUtils.getAllFieldsList(clazz)) {
            if (field.isAnnotationPresent(BindingConfigQualifier.class)) {
                if (holder.qualifier != null) {
                    throw new IllegalStateException(format(
                            "Can't declare more than one qualifier for a binding config: %s", clazz.getName()));
                }
                holder.qualifier = new BindingPropertyField(field, false);
                continue;
            }

            if (field.isAnnotationPresent(BindingConfigProperty.class)) {
                boolean required = (field.getAnnotation(Optional.class) == null);
                BindingPropertyField setter = new BindingPropertyField(field, required);

                processBindingProperty(clazz, holder, field, setter);
            }
        }
        for (Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(BindingConfigQualifier.class)) {
                if (holder.qualifier != null) {
                    throw new IllegalStateException(format(
                            "Can't declare more than one qualifier for a binding config: %s", clazz.getName()));
                }
                holder.qualifier = new BindingPropertyMethod(method, false);
                continue;
            }

            if (method.isAnnotationPresent(BindingConfigProperty.class)) {
                boolean required = (method.getAnnotation(Optional.class) == null);
                BindingPropertySetter<B> setter = new BindingPropertyMethod(method, required);

                processBindingProperty(clazz, holder, method, setter);
            }
        }
        if (holder.propertiesCount < holder.properties.size()) {
            throw new IllegalStateException(format(
                    "Missing property index (max %d specified but only %d defined) in binding config: %s",
                    holder.properties.size(), holder.propertiesCount, clazz.getName()));
        }
    }

    private void processBindingProperty(Class<? extends B> clazz, BindingPropertiesHolder holder,
            AccessibleObject member, BindingPropertySetter<B> setter) {
        BindingConfigProperty propertyAnn = member.getAnnotation(BindingConfigProperty.class);
        int index = propertyAnn.value();
        // Is it the single "extra properties" holder?
        if (index < 0) {
            if (holder.extras != null)
                throw new IllegalStateException("Can only define one BindingConfigProperty with remaining=true");
            if (!setter.isRequired())
                throw new IllegalStateException("BindingConfigProperty(remaining=true) cannot be optional");
            holder.extras = setter;
            return;
        }
        for (int i = holder.properties.size(); i <= index; ++i) {
            holder.properties.add(null);
        }
        if (holder.properties.get(index) != null) {
            throw new IllegalStateException(format("Duplicate index %d in binding config: %s", index, clazz.getName()));
        }
        holder.properties.set(index, setter);
        holder.propertiesCount++;
    }

    /*
     * Helper classes
     */
    private static final TypeDescriptor STRING_TYPE = TypeDescriptor.valueOf(String.class);

    private class BindingDescription {

        private Class<? extends B> bindingClass;
        private final BindingDirection direction;
        private final String bindingName;
        private final BindingPropertySetter<B> qualifier;
        private final List<BindingPropertySetter<B>> properties;
        private BindingPropertySetter<B> extras;

        public BindingDescription(Class<? extends B> clazz, BindingDirection direction, String bindingName,
                BindingPropertySetter<B> qualifier, List<BindingPropertySetter<B>> properties,
                BindingPropertySetter<B> extras) {
            this.bindingClass = clazz;
            this.direction = direction;
            this.bindingName = bindingName;
            this.qualifier = qualifier;
            this.extras = extras;
            this.properties = unmodifiableList(properties);
        }

        public B createBindingConfig(String bindingText, List<String> parameterValues)
                throws BindingConfigParseException {
            // First create the object
            B binding = createBindingConfig();

            return bindProperties(bindingText, binding, parameterValues);
        }

        public B createBindingConfig(String bindingText, String qualifierValue, List<String> parameterValues)
                throws BindingConfigParseException {
            if (this.qualifier == null) {
                return null;
            }

            // First create the object
            B binding = createBindingConfig();

            // Then bind the qualifier
            qualifier.setValue(binding, qualifierValue);

            return bindProperties(bindingText, binding, parameterValues);
        }

        private B bindProperties(String bindingText, B binding, List<String> parameterValues)
                throws BindingConfigParseException {
            // Then bind each property
            for (int i = 0; i < parameterValues.size(); i++) {
                String value = parameterValues.get(i);
                if (i >= properties.size()) {
                    if (extras == null)
                        throw new IllegalArgumentException(format(
                                "Binding '%s' contains too many parameters (max: %d) "
                                        + "and no holder is defined for extras", bindingText, i - 1));
                    extras.setValue(binding, value);
                    continue;
                }
                BindingPropertySetter<B> property = properties.get(i);
                property.setValue(binding, value);
            }

            // TODO: Fail if there are remaining non-optional fields
            if (properties.size() > parameterValues.size()) {
                BindingPropertySetter<B> nextProperty = properties.get(parameterValues.size());
                if (nextProperty.isRequired()) {
                    throw new BindingConfigParseException(format(
                            "Failed to parse binding '%s': field '%s' is required but was not provided", //
                            bindingText, nextProperty.getDescription()));
                }
            }
            return binding;
        }

        private B createBindingConfig() {
            try {
                return ConstructorUtils.invokeConstructor(bindingClass, new Object[0]);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException
                    | InstantiationException e) {
                throw new RuntimeException(format("Failed to create instance of %s", bindingClass), e);
            }
        }
    }

    protected static String getMemberName(Member member) {
        return format("%s.%s", member.getDeclaringClass().getSimpleName(), member.getName());
    }

    private interface BindingPropertySetter<B> {
        boolean isRequired();

        void setValue(B binding, String qualifierValue);

        String getDescription();
    }

    private class BindingPropertyField implements BindingPropertySetter<B> {
        private final Field field;
        private final TypeDescriptor targetType;
        private final boolean required;

        public BindingPropertyField(Field field, boolean required) {
            this.field = field;
            this.required = required;
            this.targetType = new TypeDescriptor(field);
            if (!TYPE_CONVERTER.canConvert(STRING_TYPE, targetType)) {
                throw new IllegalArgumentException(format("No conversion available for target field %s", field));
            }
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
        }

        @Override
        public void setValue(B binding, String value) {
            Object targetValue = TYPE_CONVERTER.convertValue(value, STRING_TYPE, targetType);
            try {
                FieldUtils.writeField(field, binding, targetValue);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean isRequired() {
            return required;
        }

        @Override
        public String getDescription() {
            return "Field:" + getMemberName(field);
        }
    }

    private class BindingPropertyMethod implements BindingPropertySetter<B> {
        private final Method method;
        private final TypeDescriptor targetType;
        private final boolean required;

        public BindingPropertyMethod(Method method, boolean required) {
            this.method = method;
            this.required = required;
            Class<?>[] types = method.getParameterTypes();
            if (types.length != 1)
                throw new IllegalStateException("Binding properties settings must take a single argument");

            this.targetType = new TypeDescriptor(new MethodParameter(method, 0));
            if (!TYPE_CONVERTER.canConvert(STRING_TYPE, targetType)) {
                throw new IllegalArgumentException(format("No conversion available for target field %s", method));
            }
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
        }

        @Override
        public void setValue(B binding, String value) {
            Object targetValue = TYPE_CONVERTER.convertValue(value, STRING_TYPE, targetType);
            try {
                method.invoke(binding, new Object[] { targetValue });
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean isRequired() {
            return required;
        }

        @Override
        public String getDescription() {
            return "Method:" + getMemberName(method);
        }
    }

    public B createBindingConfig(Item item, BindingDirection bindingDirection, List<String> parameters, String text)
            throws BindingConfigParseException {
        Map<String, BindingDescription> map = bindingDescriptions.get(bindingDirection);

        // The name is either the first or second (in the case of a qualifier) param
        BindingDescription candidate = map.get(parameters.get(0));
        B result = null;
        if (candidate != null) {
            result = candidate.createBindingConfig(text, extractArgs(parameters, 1));
        }
        if (result == null && parameters.size() > 1) {
            candidate = map.get(parameters.get(1));
            if (candidate != null) {
                result = candidate.createBindingConfig(text, parameters.get(0), extractArgs(parameters, 2));
            }
        }
        if (result == null) {
            throw new BindingConfigParseException(format("No candidate binding found for %s", text));
        }

        try {
            FieldUtils.writeField(result, "item", item, true);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(format("Failed to write itemName to binding config %s", result.getClass()), e);
        }
        return result;
    }

    private List<String> extractArgs(List<String> parameters, int toRemove) {
        if (parameters.size() <= toRemove) {
            return emptyList();
        }
        return parameters.subList(toRemove, parameters.size());
    }
}
