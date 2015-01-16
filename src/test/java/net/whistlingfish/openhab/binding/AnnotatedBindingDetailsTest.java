package net.whistlingfish.openhab.binding;

import org.junit.Test;

public class AnnotatedBindingDetailsTest {

    @Test(expected = IllegalStateException.class)
    public void misorderedOptionalParametersFailToLoad() {
        new TestProvider(MisorderedOptionalParametersConfig.class);
    }

    public static abstract class TestBindingConfig extends
            AnnotationBasedBindingConfig<TestBindingConfig, TestBinding, TestProvider> {}

    @BindingConfigType(direction = BindingDirection.IN, name = "misordered")
    public static class MisorderedOptionalParametersConfig extends TestBindingConfig {
        @BindingConfigProperty(0)
        private String p1;

        @Optional
        @BindingConfigProperty(1)
        private int p2 = -1;

        @BindingConfigProperty(2)
        private String p3;
    }

    public static class TestProvider extends
            AnnotationBasedBindingProvider<TestBindingConfig, TestBinding, TestProvider> {
        @SafeVarargs
        public TestProvider(Class<? extends TestBindingConfig>... classes) {
            super(TestBindingConfig.class, classes);
        }

        @Override
        public String getBindingType() {
            return "test";
        }
    }

    public static class TestBinding extends AnnotationBasedBinding<TestBindingConfig, TestBinding, TestProvider> {
        public TestBinding(Class<TestProvider> providerClass) {
            super(TestBinding.class, providerClass);
        }
    }
}
