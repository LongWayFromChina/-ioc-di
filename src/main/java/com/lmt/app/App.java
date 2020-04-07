package com.lmt.app;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.lmt.annotation.Autowired;
import com.lmt.annotation.Component;
import com.lmt.util.FileUtil;

public class App {
	private Class main;
	private Table<Class, String, Object> beans;

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
//				�����autowiredע��
				if (field.isAnnotationPresent(Autowired.class)) {
					Autowired auto = field.getAnnotation(Autowired.class);
					String requiredName = auto.name();
					beans.cellSet().forEach(requiredCell -> {
						Object requiredBean=requiredCell.getValue();
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
			dependencyInject();

		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	public Object getBean(Class clazz, String beanName) {
		return this.beans.get(clazz, beanName);
	}

}
