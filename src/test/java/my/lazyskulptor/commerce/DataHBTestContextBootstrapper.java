package my.lazyskulptor.commerce;

import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.TestContextAnnotationUtils;

public class DataHBTestContextBootstrapper extends SpringBootTestContextBootstrapper {

    @Override
    protected String[] getProperties(Class<?> testClass) {
        DataHBTest dataHBTest = TestContextAnnotationUtils.findMergedAnnotation(testClass, DataHBTest.class);
        return (dataHBTest != null) ? dataHBTest.properties() : null;
    }
}
