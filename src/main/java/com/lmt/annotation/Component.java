package com.lmt.annotation;



import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;



@Target(ElementType.TYPE)

@Retention(RetentionPolicy.RUNTIME)
public @interface Component {
	public String name () default "";

}
