package org.reflections8;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.reflections8.ReflectionUtils.getAllAnnotations;
import static org.reflections8.ReflectionUtils.getAllConstructors;
import static org.reflections8.ReflectionUtils.getAllFields;
import static org.reflections8.ReflectionUtils.getAllMethods;
import static org.reflections8.ReflectionUtils.getAllSuperTypes;
import static org.reflections8.ReflectionUtils.getAnnotations;
import static org.reflections8.ReflectionUtils.getMethods;
import static org.reflections8.ReflectionUtils.withAnnotation;
import static org.reflections8.ReflectionUtils.withAnyParameterAnnotation;
import static org.reflections8.ReflectionUtils.withModifier;
import static org.reflections8.ReflectionUtils.withName;
import static org.reflections8.ReflectionUtils.withParameters;
import static org.reflections8.ReflectionUtils.withParametersAssignableFrom;
import static org.reflections8.ReflectionUtils.withParametersAssignableTo;
import static org.reflections8.ReflectionUtils.withParametersCount;
import static org.reflections8.ReflectionUtils.withPattern;
import static org.reflections8.ReflectionUtils.withReturnType;
import static org.reflections8.ReflectionUtils.withReturnTypeAssignableTo;
import static org.reflections8.ReflectionUtils.withTypeAssignableTo;
import static org.reflections8.ReflectionsTest.are;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.reflections8.scanners.FieldAnnotationsScanner;
import org.reflections8.util.ReflectionsIterables;

/**
 * @author mamo
 */
@SuppressWarnings("unchecked")
public class ReflectionUtilsTest {

    @Test
    public void getAllTest() {
        assertThat(getAllSuperTypes(TestModel.C3.class, withAnnotation(TestModel.AI1.class)), are(TestModel.I1.class));

        Set<Method> allMethods = getAllMethods(TestModel.C4.class, withModifier(Modifier.PUBLIC), withReturnType(void.class));
        Set<Method> allMethods1 = getAllMethods(TestModel.C4.class, withPattern("public.*.void .*"));

        assertTrue(allMethods.containsAll(allMethods1) && allMethods1.containsAll(allMethods));
        assertThat(allMethods1, names("m1"));

        assertThat(getAllMethods(TestModel.C4.class, withAnyParameterAnnotation(TestModel.AM1.class)), names("m4"));

        assertThat(getAllFields(TestModel.C4.class, withAnnotation(TestModel.AF1.class)), names("f1", "f2"));

        assertThat(getAllFields(TestModel.C4.class, withAnnotation(new TestModel.AF1() {
            public String value() {return "2";}
            public Class<? extends Annotation> annotationType() {return TestModel.AF1.class;}})),
                names("f2"));

        assertThat(getAllFields(TestModel.C4.class, withTypeAssignableTo(String.class)), names("f1", "f2", "f3"));

        assertThat(getAllConstructors(TestModel.C4.class, withParametersCount(0)), names(TestModel.C4.class.getName()));

        assertEquals(getAllAnnotations(TestModel.C3.class).size(), 5);

        Method m4 = getMethods(TestModel.C4.class, withName("m4")).iterator().next();
        assertEquals(m4.getName(), "m4");
        assertTrue(getAnnotations(m4).isEmpty());
    }

    @Test public void withParameter() throws Exception {
        Class target = Collections.class;
        Object arg1 = Arrays.asList(1, 2, 3);

        Set<Method> allMethods = new HashSet();
        for (Class<?> type : getAllSuperTypes(arg1.getClass())) {
            allMethods.addAll(getAllMethods(target, withModifier(Modifier.STATIC), withParameters(type)));
        }

        Set<Method> allMethods1 = getAllMethods(target, withModifier(Modifier.STATIC), withParametersAssignableTo(arg1.getClass()));

        assertEquals(allMethods, allMethods1);

        for (Method method : allMethods) { //effectively invokable
            //noinspection UnusedDeclaration
            Object invoke = method.invoke(null, arg1);
        }
    }

    @Test
    public void withParametersAssignableFromTest() throws Exception {
        //Check for null safe
        getAllMethods(Collections.class, withModifier(Modifier.STATIC), withParametersAssignableFrom());

        Class target = Collections.class;
        Object arg1 = Arrays.asList(1, 2, 3);

        Set<Method> allMethods = new HashSet();
        for (Class<?> type : getAllSuperTypes(arg1.getClass())) {
            allMethods.addAll(getAllMethods(target, withModifier(Modifier.STATIC), withParameters(type)));
        }

        Set<Method> allMethods1 = getAllMethods(target, withModifier(Modifier.STATIC), withParametersAssignableFrom(Iterable.class), withParametersAssignableTo(arg1.getClass()));

        assertEquals(allMethods, allMethods1);

        for (Method method : allMethods) { //effectively invokable
            //noinspection UnusedDeclaration
            Object invoke = method.invoke(null, arg1);
        }
    }

    @Test public void withReturn() throws Exception {
        Set<Method> returnMember = getAllMethods(Class.class, withReturnTypeAssignableTo(Member.class));
        Set<Method> returnsAssignableToMember = getAllMethods(Class.class, withReturnType(Method.class));

        assertTrue(returnMember.containsAll(returnsAssignableToMember));
        assertFalse(returnsAssignableToMember.containsAll(returnMember));

        returnsAssignableToMember = getAllMethods(Class.class, withReturnType(Field.class));
        assertTrue(returnMember.containsAll(returnsAssignableToMember));
        assertFalse(returnsAssignableToMember.containsAll(returnMember));
    }

    @Test
    public void getAllAndReflections() {
        Reflections reflections8 = new Reflections(TestModel.class, new FieldAnnotationsScanner());

        Set<Field> af1 = reflections8.getFieldsAnnotatedWith(TestModel.AF1.class);
        Set<? extends Field> allFields = ReflectionUtils.getAll(af1, withModifier(Modifier.PROTECTED));
        assertTrue(allFields.size() == 1);
        assertThat(allFields, names("f2"));
    }

    private Set<String> names(Set<? extends Member> o) {
        return ReflectionsIterables.transformToSet(o, new Function<Member, String>() {
            public String apply(Member input) {
                return input.getName();
            }
        });
    }

    private BaseMatcher<Set<? extends Member>> names(final String... namesArray) {
        return new BaseMatcher<Set<? extends Member>>() {

                public boolean matches(Object o) {
                    Collection<String> transform = names((Set<Member>) o);
                    final Collection<?> names = Arrays.asList(namesArray);
                    return transform.containsAll(names) && names.containsAll(transform);
                }

                public void describeTo(Description description) {
                }
            };
    }
}
