package com.lmt.app;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.lmt.annotation.After;
import com.lmt.annotation.Aspect;
import com.lmt.annotation.Autowired;
import com.lmt.annotation.Before;
import com.lmt.annotation.Component;
import com.lmt.util.FileUtil;
import com.lmt.util.ProxyUtil;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class App {
	private Class main;
	private Table<Class, String, Object> beans;

	public void init() {
		beans = HashBasedTable.create();
	}

	/**
	 * 实例化bean
	 */
	private void creatBean() throws Exception {
		URL resource = this.main.getResource("/");
		File baseDir = new File(resource.toURI().getPath());
		int prefixFileName = baseDir.toURI().getPath().lastIndexOf("/");
		List<File> fileList = new ArrayList();
		// 开始遍历根路径下所有的文件
		FileUtil.getAllFile(baseDir, fileList);
		// 将根路径下所有的class文件中有component注解的类通过反射实例化并加入bean成员变量
		fileList.stream().map(classFile -> classFile.getPath().substring(prefixFileName).replaceAll("\\\\", "."))
				.filter(name -> name.endsWith(".class") && !name.contains("com.lmt")).forEach(clsName -> {

					try {
						Class clazz = this.main.getClassLoader()
								.loadClass(clsName.substring(0, clsName.lastIndexOf('.')));
						if (clazz.isAnnotationPresent(Component.class)) {
							Object obj = clazz.getConstructor().newInstance();
							Component com = (Component) clazz.getAnnotation(Component.class);
							this.beans.put(clazz, com.name(), obj);

						}
					} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
							| IllegalArgumentException | InvocationTargetException | NoSuchMethodException
							| SecurityException e) {

						e.printStackTrace();
					}

				});

	}

	/**
	 * 对所有的bean进行依赖注入
	 */
	private void dependencyInject() {
		// 遍历所有的bean
		this.beans.cellSet().forEach(cell -> {
			String beanName = cell.getColumnKey();
			Class beanClazz = cell.getRowKey();
			Arrays.asList(cell.getValue().getClass().getDeclaredFields()).forEach(field -> {
//				如果有autowired注解
				if (field.isAnnotationPresent(Autowired.class)) {
					Autowired auto = field.getAnnotation(Autowired.class);
					String requiredName = auto.name();
					beans.cellSet().forEach(requiredCell -> {
						Object requiredBean = requiredCell.getValue();
						Class clazz = field.getType();

						if (field.getType().isInstance(requiredBean) && Objects.equals(beanName, requiredName)) {
							field.setAccessible(true);
							try {

								field.set(cell.getValue(), requiredBean);

							} catch (IllegalArgumentException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IllegalAccessException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					});

				}
			});
		});

	}

	public void run(Class main) {
		// 初始化
		init();

		this.main = main;
		// 获取所有的需要注入容器的bean
		try {
			// 获取所有的需要注入容器的bean
			creatBean();
			aop();
			dependencyInject();

		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	/**
	 * 实现所有bean的aop
	 */
	private void aop() {
		System.out.println("开始aop");
		Table<Class, String, Object> beanTable = HashBasedTable.create();
		this.beans.cellSet().stream().filter(cell -> {
			// 取出所有含有aspect注解的bean
			return cell.getRowKey().isAnnotationPresent(Aspect.class);
		}).forEach(cell2 -> {

			Stream.of(cell2.getRowKey().getDeclaredMethods()).filter(method -> {
				// 取出含有before或者after注解修饰的方法
				return (method.isAnnotationPresent(Before.class) || method.isAnnotationPresent(After.class));
			}).forEach(method -> {

				String fullMethodName = null;
				Boolean isBefore = method.isAnnotationPresent(Before.class);
				if (isBefore) {
					fullMethodName = method.getAnnotation(Before.class).methodName();
				} else {
					fullMethodName = method.getAnnotation(After.class).methodName();
				}
				String methodName = fullMethodName.substring(fullMethodName.lastIndexOf('.') + 1);

				String className = fullMethodName.substring(0, fullMethodName.lastIndexOf('.'));

				try {
					Class target = Class.forName(className);
					Component com = (Component) target.getAnnotation(Component.class);
					String beanName = com.name();
					Object proxy=ProxyUtil.getProxy(cell2.getRowKey(), cell2.getValue());
					beanTable.put(target, beanName, proxy);
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			});
			;
		});
		// 重新给beans赋值来代替那些要织入的类
		this.beans.putAll(beanTable);

		System.out.println("结束");
	}

	public <T> T getBean(Class clazz, String beanName) {
		return (T) this.beans.get(clazz, beanName);
	}

}
