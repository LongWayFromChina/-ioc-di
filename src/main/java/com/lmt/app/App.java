package com.lmt.app;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.startup.Tomcat;
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
import com.lmt.annotation.Controller;
import com.lmt.util.FileUtil;
import com.lmt.util.ProxyUtil;
import com.servlet.DispatcherServlet;
import com.servlet.TestServlet;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class App {
	private Class main;
	private static Table<Class, String, Object> beans;

	public void init() {
		beans = HashBasedTable.create();
	}

	/**
	 * ʵ����bean
	 */
	private void creatBean() throws Exception {
		URL resource = this.main.getResource("/");
		File baseDir = new File(resource.toURI().getPath());
		int prefixFileName = baseDir.toURI().getPath().lastIndexOf("/");
		List<File> fileList = new ArrayList();
		// ��ʼ������·�������е��ļ�
		FileUtil.getAllFile(baseDir, fileList);
		// ����·�������е�class�ļ�����componentע�����ͨ������ʵ����������bean��Ա����
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
						// �ж��Ƿ���controllerע��,ʵ����controller����
						if (clazz.isAnnotationPresent(Controller.class)) {
							Object obj = clazz.getConstructor().newInstance();
							Controller con = (Controller) clazz.getAnnotation(Controller.class);
							this.beans.put(clazz, con.value(), obj);

						}
					} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
							| IllegalArgumentException | InvocationTargetException | NoSuchMethodException
							| SecurityException e) {

						e.printStackTrace();
					}

				});

	}

	/**
	 * �����е�bean��������ע��
	 */
	private void dependencyInject() {
		// �������е�bean
		this.beans.cellSet().forEach(cell -> {
			String beanName = cell.getColumnKey();
			Class beanClazz = cell.getRowKey();
			Arrays.asList(cell.getValue().getClass().getDeclaredFields()).forEach(field -> {
				// �����autowiredע��
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
		// ��ʼ��
		init();

		this.main = main;
		// ��ȡ���е���Ҫע��������bean
		try {
			// ��ȡ���е���Ҫע��������bean
			creatBean();
			// ��֪ͨ֯���е�
			aop();
			// ����ע��
			dependencyInject();
			startServer();
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	/**
	 * ʵ������bean��aop
	 */
	private void aop() {
		System.out.println("��ʼaop");
		Table<Class, String, Object> beanTable = HashBasedTable.create();
		this.beans.cellSet().stream().filter(cell -> {
			// ȡ�����к���aspectע���bean
			return cell.getRowKey().isAnnotationPresent(Aspect.class);
		}).forEach(cell2 -> {

			Stream.of(cell2.getRowKey().getDeclaredMethods()).filter(method -> {
				// ȡ������before����afterע�����εķ���
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
					Object proxy = ProxyUtil.getProxy(cell2.getRowKey(), cell2.getValue());
					beanTable.put(target, beanName, proxy);
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			});
			;
		});
		// ���¸�beans��ֵ��������ЩҪ֯�����
		this.beans.putAll(beanTable);

		System.out.println("����");
	}

	public static <T> T getBean(Class clazz, String beanName) {
		return (T) beans.get(clazz, beanName);
	}

	/**
	 * ����url��ȡcontroller�ķ���
	 * 
	 * @param <T>
	 * @param fullUrl
	 * @return
	 */
	public static Map<Class,Object> getController(String fullUrl) {
		Map<String, Map<Class, Object>> map = beans.columnMap();
		List<Map<Class, Object>> controllerList = map.keySet().stream().map(key -> {
			// ��ȡfullUrl��ͷ��keyֵ
			if (fullUrl.startsWith(key)) {
				return map.get(key);
			}
			return null;
		}).filter(Objects::isNull).collect(Collectors.toList());
		//	���ֻ��һ��ƥ�䣬������
		if(controllerList.size()==1) {
			return controllerList.get(0);
		}
		// @TODO ���������Ϊ�쳣
		
		// @TODO �˴���Ϊ404 controller
		return null;
	}
	
	private void startServer() throws LifecycleException {
		long start = System.currentTimeMillis();
	    // ���ö˿�
	    int port = 8888;
	    Tomcat tomcat = new Tomcat();
	    tomcat.setPort(port);
	 
	    // ���listener
	    StandardServer server = (StandardServer) tomcat.getServer();
	    AprLifecycleListener listener = new AprLifecycleListener();
	    server.addLifecycleListener(listener);
	 
	    // ����contextPath��·��
	    String contextPath = "/ee-tomat-embed";
	    String docBase = new File("webapp").getAbsolutePath();
	    Context context = tomcat.addWebapp(contextPath, docBase);
	    System.out.println("���contextPath��docBase��:" + contextPath + "==>" + docBase);
	 
	    // add servlet
	    // Context context = tomcat.addContext(contextPath, baseDir);
	    String servletName = "dispatcherServlet";
	    String servletMapping = "/hello";
	    tomcat.addServlet(contextPath, servletName, new DispatcherServlet());
	    context.addServletMappingDecoded(servletMapping, servletName);
	    // ����tomcat
	    tomcat.start();
	    long end = System.currentTimeMillis();
	    System.out.println("�������,��ʹ����:" + (end - start) + "ms");
	    // �������״̬,������������״̬,����tomat��ͻ�ر�tomcat
	    tomcat.getServer().await();

	
	}
}
