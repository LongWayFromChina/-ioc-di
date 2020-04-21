package com.servlet;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.stream.Stream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lmt.annotation.PostMapping;
import com.lmt.app.App;
import com.lmt.util.entity.ModelAndView;

public class DispatcherServlet extends HttpServlet {
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		super.doGet(req, resp);
		
		doPost(req, resp);
	}
	
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub
		super.doPost(req, resp);
		String requestUrl=req.getRequestURL().toString();
		Map<Class,Object> controllerMap=App.getController(requestUrl);
		controllerMap.forEach((clazz,obj)->{
			Stream.of(clazz.getDeclaredMethods()).forEach(method->{
				if(method.isAnnotationPresent(PostMapping.class)) {
					// 获取方法上的post注解，判断是否requstUrl以该注解的url为结尾
					PostMapping post=method.getAnnotation(PostMapping.class);
					if(requestUrl.endsWith(post.value())) {
						// 执行该方法
						Map<String,String[]> requestParam=req.getParameterMap();
						Class[] paramType=method.getParameterTypes();
						Object [] paramArray=Stream.of(method.getParameters())
							.map(param->{
								return requestParam.get(param.getName()).toString();
								
							}).toArray();
						;
						
						try {
							ModelAndView mov=(ModelAndView)method.invoke(obj,paramArray);
							req.setAttribute("data", mov.getModel());
							req.getRequestDispatcher(mov.getView()).forward(req, resp);
							
							
						} catch (IllegalAccessException |IllegalArgumentException |InvocationTargetException | ServletException | IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			});
		});
		
	}
	
}
