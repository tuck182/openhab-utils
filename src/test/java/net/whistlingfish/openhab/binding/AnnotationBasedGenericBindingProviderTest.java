package net.whistlingfish.openhab.binding;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.model.item.binding.BindingConfigParseException;

import static net.whistlingfish.openhab.binding.BindingDirection.IN;
import static net.whistlingfish.openhab.binding.BindingDirection.OUT;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

import static org.junit.Assert.assertThat;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AnnotationBasedGenericBindingProviderTest {

    private TestProvider provider = new TestProvider();

    @Mock
    private Item item;

    @Before
    public void setUp() {
        when(item.getName()).thenReturn("test item");
    }

    @Test
    public void simpleInputConfigIsParsed() throws BindingConfigParseException {
        BindingConfig bindingConfig = provider.createBindingConfiguration(item, "<[simpleIn]");
        assertThat(bindingConfig, is(instanceOf(SimpleInputConfig.class)));
    }

    @Test
    public void simpleOutputConfigIsParsed() throws BindingConfigParseException {
        BindingConfig bindingConfig = provider.createBindingConfiguration(item, ">[simpleOut]");
        assertThat(bindingConfig, is(instanceOf(SimpleOutputConfig.class)));
    }

    @Test(expected = BindingConfigParseException.class)
    public void wrongDirectionFails() throws BindingConfigParseException {
        provider.createBindingConfiguration(item, "<[simpleOut]");
    }

    @Test
    public void paramIsCaptured() throws BindingConfigParseException {
        BindingConfig bindingConfig = provider.createBindingConfiguration(item, ">[parameterized:foo:bar]");
        assertThat(bindingConfig, is(instanceOf(ParameterizedOutputConfig.class)));

        ParameterizedOutputConfig parameterized = (ParameterizedOutputConfig) bindingConfig;
        assertThat(parameterized.getP1(), is("foo"));
        assertThat(parameterized.getP2(), is("bar"));
    }

    @Test(expected = BindingConfigParseException.class)
    public void missingParamFails() throws BindingConfigParseException {
        provider.createBindingConfiguration(item, ">[parameterized:foo]");
    }

    @Test
    public void optionalParamIsIgnored() throws BindingConfigParseException {
        BindingConfig bindingConfig = provider.createBindingConfiguration(item, ">[complex:foo:23]");
        assertThat(bindingConfig, is(instanceOf(ComplexOutputConfig.class)));

        ComplexOutputConfig complex = (ComplexOutputConfig) bindingConfig;
        assertThat(complex.getProfile(), nullValue());
        assertThat(complex.getP1(), is("foo"));
        assertThat(complex.getP2(), is(23));
    }

    @Test(expected = BindingConfigParseException.class)
    public void missingParamWithOptionalFails() throws BindingConfigParseException {
        provider.createBindingConfiguration(item, ">[complex]");
    }

    @Test
    public void qualifierWithAllParamsIsParsed() throws BindingConfigParseException {
        BindingConfig bindingConfig = provider.createBindingConfiguration(item, ">[default:complex:foo:23:bar]");
        assertThat(bindingConfig, is(instanceOf(ComplexOutputConfig.class)));

        ComplexOutputConfig complex = (ComplexOutputConfig) bindingConfig;
        assertThat(complex.getProfile(), is("default"));
        assertThat(complex.getP1(), is("foo"));
        assertThat(complex.getP2(), is(23));
        assertThat(complex.getP3(), is("bar"));
    }

    @Test
    public void qualifierWithSomeParamsIsParsed() throws BindingConfigParseException {
        BindingConfig bindingConfig = provider.createBindingConfiguration(item, ">[default:complex:foo]");
        assertThat(bindingConfig, is(instanceOf(ComplexOutputConfig.class)));

        ComplexOutputConfig complex = (ComplexOutputConfig) bindingConfig;
        assertThat(complex.getProfile(), is("default"));
        assertThat(complex.getP1(), is("foo"));
        assertThat(complex.getP2(), is(-1));
        assertThat(complex.getP3(), nullValue());
    }

    @Test(expected = BindingConfigParseException.class)
    public void missingParamWithQualifierWithOptionalFails() throws BindingConfigParseException {
        provider.createBindingConfiguration(item, ">[default:complex]");
    }

    /*
     * No need to test both input and output in all permutations, as they both parse the same
     */
    public abstract static class TestBindingConfig extends
            AnnotationBasedBindingConfig<TestBindingConfig, TestBinding, TestProvider> {
    }

    @BindingConfigType(name = "simpleIn", direction = IN)
    public static class SimpleInputConfig extends TestBindingConfig {
        // no properties
    }

    @BindingConfigType(name = "simpleOut", direction = OUT)
    public static class SimpleOutputConfig extends TestBindingConfig {
        // no properties
    }

    @BindingConfigType(name = "parameterized", direction = OUT)
    public static class ParameterizedOutputConfig extends TestBindingConfig {
        @BindingConfigProperty(0)
        private String p1;

        @BindingConfigProperty(1)
        private String p2;

        public String getP1() {
            return p1;
        }

        public String getP2() {
            return p2;
        }
    }

    /*
     * An output config that has an optional profile as well as one required and two optional parameters
     */
    @BindingConfigType(name = "complex", direction = OUT)
    public static class ComplexOutputConfig extends TestBindingConfig {
        @BindingConfigQualifier
        private String profile;

        @BindingConfigProperty(0)
        private String p1;

        @Optional
        @BindingConfigProperty(1)
        private int p2 = -1;

        @Optional
        @BindingConfigProperty(2)
        private String p3;

        public String getProfile() {
            return profile;
        }

        public String getP1() {
            return p1;
        }

        public int getP2() {
            return p2;
        }

        public String getP3() {
            return p3;
        }
    }

    public static class TestProvider extends
            AnnotationBasedBindingProvider<TestBindingConfig, TestBinding, TestProvider> {
        private BindingConfig lastBindingConfig = null;

        public TestProvider() {
            super(TestBindingConfig.class, SimpleInputConfig.class, SimpleOutputConfig.class,
                    ParameterizedOutputConfig.class, ComplexOutputConfig.class);
        }

        @Override
        public String getBindingType() {
            return "test";
        }

        public BindingConfig createBindingConfiguration(Item item, String bindingConfig)
                throws BindingConfigParseException {
            processBindingConfiguration("test context", item, bindingConfig);
            return lastBindingConfig;
        }

        @Override
        protected void addBindingConfig(Item item, BindingConfig config) {
            lastBindingConfig = config;
            super.addBindingConfig(item, config);
        }
    }

    public static class TestBinding extends AnnotationBasedBinding<TestBindingConfig, TestBinding, TestProvider> {
        public TestBinding(Class<TestProvider> providerClass) {
            super(TestBinding.class, providerClass);
        }
    }
}
