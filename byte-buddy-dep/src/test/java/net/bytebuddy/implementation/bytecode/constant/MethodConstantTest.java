package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.asm.Opcodes;
import org.mockito.asm.Type;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class MethodConstantTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription.inDefinedShape methodDescription;

    @Mock
    private TypeDescription declaringType, parameterType, fieldType;

    @Mock
    private ParameterList<?> parameterList;

    @Mock
    private GenericTypeList typeList;

    @Mock
    private TypeList rawTypeList;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Mock
    private FieldDescription.InDefinedShape fieldDescription;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(declaringType.asRawType()).thenReturn(declaringType);
        when(methodDescription.getDeclaringType()).thenReturn(declaringType);
        when(methodDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getParameters()).thenReturn((ParameterList) parameterList);
        when(parameterList.asTypeList()).thenReturn(typeList);
        when(declaringType.getDescriptor()).thenReturn(BAR);
        when(typeList.asRawTypes()).thenReturn(rawTypeList);
        when(rawTypeList.iterator()).thenReturn(Collections.singletonList(parameterType).iterator());
        when(parameterType.getDescriptor()).thenReturn(QUX);
        when(fieldDescription.getType()).thenReturn(fieldType);
        when(fieldDescription.isStatic()).thenReturn(true);
        when(fieldType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(fieldType.asRawType()).thenReturn(fieldType);
        when(fieldDescription.getDeclaringType()).thenReturn(declaringType);
        when(declaringType.getInternalName()).thenReturn(BAZ);
        when(fieldDescription.getInternalName()).thenReturn(FOO);
        when(fieldDescription.getDescriptor()).thenReturn(QUX);
        when(fieldDescription.asDefined()).thenReturn(fieldDescription);
    }

    @Test
    public void testMethod() throws Exception {
        StackManipulation.Size size = MethodConstant.forMethod(methodDescription).apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(6));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                Type.getInternalName(Class.class),
                "getDeclaredMethod",
                "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
                false);
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testMethodCached() throws Exception {
        when(implementationContext.cache(any(StackManipulation.class), any(TypeDescription.class))).thenReturn(fieldDescription);
        StackManipulation.Size size = MethodConstant.forMethod(methodDescription).cached().apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitFieldInsn(Opcodes.GETSTATIC, BAZ, FOO, QUX);
        verifyNoMoreInteractions(methodVisitor);
        verify(implementationContext).cache(MethodConstant.forMethod(methodDescription), new TypeDescription.ForLoadedType(Method.class));
        verifyNoMoreInteractions(implementationContext);
    }

    @Test
    public void testConstructor() throws Exception {
        when(methodDescription.isConstructor()).thenReturn(true);
        StackManipulation.Size size = MethodConstant.forMethod(methodDescription).apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(5));
        verify(methodVisitor).visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                Type.getInternalName(Class.class),
                "getDeclaredConstructor",
                "([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;",
                false);
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testConstructorCached() throws Exception {
        when(methodDescription.isConstructor()).thenReturn(true);
        when(implementationContext.cache(any(StackManipulation.class), any(TypeDescription.class))).thenReturn(fieldDescription);
        StackManipulation.Size size = MethodConstant.forMethod(methodDescription).cached().apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitFieldInsn(Opcodes.GETSTATIC, BAZ, FOO, QUX);
        verifyNoMoreInteractions(methodVisitor);
        verify(implementationContext).cache(MethodConstant.forMethod(methodDescription), new TypeDescription.ForLoadedType(Method.class));
        verifyNoMoreInteractions(implementationContext);
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeInitializer() throws Exception {
        when(methodDescription.isTypeInitializer()).thenReturn(true);
        MethodConstant.CanCache methodConstant = MethodConstant.forMethod(methodDescription);
        assertThat(methodConstant.isValid(), is(false));
        assertThat(methodConstant.cached().isValid(), is(false));
        methodConstant.apply(methodVisitor, implementationContext);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodConstant.ForMethod.class).apply();
        ObjectPropertyAssertion.of(MethodConstant.ForConstructor.class).apply();
        ObjectPropertyAssertion.of(MethodConstant.Cached.class).apply();
        ObjectPropertyAssertion.of(MethodConstant.CanCacheIllegal.class).apply();
    }
}
