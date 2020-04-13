package com.lmt.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.lmt.annotation.After;
import com.lmt.annotation.Before;
import com.lmt.util.entity.ClassVisitorEntity;

/**
 * 根据aop注解生成代理类
 * 
 * @author LMT
 *
 */
public class ProxyUtil {
	/**
	 * 根据目标类和通知类来生成代理类的发方法
	 * 
	 * @param interceptor
	 * @return
	 */
	public static Object getProxy(Class interceptor, Object sourceBean) {
		// Object result=null;
		return Stream.of(interceptor.getDeclaredMethods()).map(method -> {

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

				ClassVisitorEntity entity = new ClassVisitorEntity();
				entity.setBefore(isBefore); // 是否为前置通知
				entity.setProxyName(className.replace(".", "/") + "&Proxy");// 设置代理名称
				entity.setProxyMethodName(methodName); // 设置要代理的方法
				entity.setSource(sourceBean);// 设置要织入的对象
				entity.setSourceName("L" + interceptor.getName().replace(".", "/") + ";");
				entity.setSourceClass(interceptor);
				entity.setSourceMethodName(method.getName());
				entity.setTargetName(className.replace(".", "/"));
				// 获取父类的所有属性
				List<Map<String, String>> list = Stream.of(target.getDeclaredFields()).map(field -> {
					String key = "L" + field.getGenericType().getTypeName().replace(".", "/") + ";";
					String name = field.getName();
					Map<String, String> map = new HashMap();
					map.put(name, key);
					return map;

				}).collect(Collectors.toList());
				entity.setPrivateFieldName(list);

				// 使用ASM对目标方法进行代理
				return generateProxy(entity);

			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}).collect(Collectors.toList()).get(0);

	}

	public static Object generateProxy(ClassVisitorEntity entity) {

		try {
			byte[] byteClass = new Opcodes() {

				public byte[] dump() throws Exception {

					ClassWriter cw = new ClassWriter(0);
					FieldVisitor fv;
					MethodVisitor mv;
					AnnotationVisitor av0;

					cw.visit(52, ACC_PUBLIC + ACC_SUPER, entity.getProxyName(), null, entity.getTargetName(), null);

					{
						fv = cw.visitField(ACC_PRIVATE, "proxy", entity.getSourceName(), null, null);
						fv.visitEnd();
					}
					{
						fv = cw.visitField(ACC_PRIVATE, "isBefore", "Z", null, null);
						fv.visitEnd();
					}
					// 增加父类的私有属性
					entity.getPrivateFieldName().forEach(map -> {
						map.forEach((name, sign) -> {
							FieldVisitor fv1 = cw.visitField(ACC_PRIVATE, name, sign, null, null);
							fv1.visitEnd();
						});

					});

					{
						mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
						mv.visitCode();
						mv.visitVarInsn(ALOAD, 0);
						mv.visitMethodInsn(INVOKESPECIAL, entity.getTargetName(), "<init>", "()V", false);
						mv.visitVarInsn(ALOAD, 0);
						mv.visitInsn(ICONST_1);
						mv.visitFieldInsn(PUTFIELD, entity.getProxyName(), "isBefore", "Z");
						mv.visitInsn(RETURN);
						mv.visitMaxs(2, 1);
						mv.visitEnd();
					}
					{
						mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(" + entity.getSourceName() + "Z)V", null, null);
						mv.visitCode();
						mv.visitVarInsn(ALOAD, 0);
						mv.visitMethodInsn(INVOKESPECIAL, entity.getTargetName(), "<init>", "()V", false);
						mv.visitVarInsn(ALOAD, 0);
						mv.visitInsn(ICONST_1);
						mv.visitFieldInsn(PUTFIELD, entity.getProxyName(), "isBefore", "Z");
						mv.visitVarInsn(ALOAD, 0);
						mv.visitVarInsn(ALOAD, 1);
						mv.visitFieldInsn(PUTFIELD, entity.getProxyName(), "proxy", entity.getSourceName());
						mv.visitVarInsn(ALOAD, 0);
						mv.visitVarInsn(ILOAD, 2);
						mv.visitFieldInsn(PUTFIELD, entity.getProxyName(), "isBefore", "Z");
						mv.visitInsn(RETURN);
						mv.visitMaxs(2, 3);
						mv.visitEnd();
					}
					{
						mv = cw.visitMethod(ACC_PUBLIC, entity.getProxyMethodName(), "()V", null, null);
						mv.visitCode();
						mv.visitVarInsn(ALOAD, 0);
						mv.visitFieldInsn(GETFIELD, entity.getProxyName(), "isBefore", "Z");
						Label l0 = new Label();
						mv.visitJumpInsn(IFEQ, l0);
						mv.visitVarInsn(ALOAD, 0);
						mv.visitFieldInsn(GETFIELD, entity.getProxyName(), "proxy", entity.getSourceName());
						mv.visitMethodInsn(
								INVOKEVIRTUAL, entity.getSourceName()
										.substring(0, entity.getSourceName().lastIndexOf(";")).substring(1),
								entity.getSourceMethodName(), "()V", false);
						mv.visitLabel(l0);
						mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
						mv.visitVarInsn(ALOAD, 0);
						mv.visitMethodInsn(INVOKESPECIAL, entity.getTargetName(), entity.getProxyMethodName(), "()V",
								false);
						mv.visitVarInsn(ALOAD, 0);
						mv.visitFieldInsn(GETFIELD, entity.getProxyName(), "isBefore", "Z");
						Label l1 = new Label();
						mv.visitJumpInsn(IFNE, l1);
						mv.visitVarInsn(ALOAD, 0);
						mv.visitFieldInsn(GETFIELD, entity.getProxyName(), "proxy", entity.getSourceName());
						mv.visitMethodInsn(
								INVOKEVIRTUAL, entity.getSourceName()
										.substring(0, entity.getSourceName().lastIndexOf(";")).substring(1),
								entity.getSourceMethodName(), "()V", false);
						mv.visitLabel(l1);
						mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
						mv.visitInsn(RETURN);
						mv.visitMaxs(1, 1);
						mv.visitEnd();
					}
					cw.visitEnd();

					return cw.toByteArray();
				}
			}.dump();

			Class<?> proxy = new ClassLoader() {

				public Class<?> getClass(String name, byte[] byteClass) {
					// TODO Auto-generated method stub
					return defineClass(name, byteClass, 0, byteClass.length);
				}

			}.getClass(entity.getProxyName().replace("/", "."), byteClass);

			return proxy.getConstructor(entity.getSourceClass(), boolean.class).newInstance(entity.getSource(),
					entity.isBefore());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

}
