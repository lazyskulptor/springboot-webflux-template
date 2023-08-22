package my.lazyskulptor.adapter.annotation;

import my.lazyskulptor.adapter.autoconfigure.HrsaAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ImportAutoConfiguration(value = HrsaAutoConfiguration.class)
public @interface EnableHibernateReactiveSpringAdapter {
}
