package com.lmt.util.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClassVisitorEntity {
	private String proxyName; //tets/AppleProxy
	private String targetName;//tets/Apple
	private String sourceName;// Ltets/Wash;
	private String proxyMethodName;//eat
	private List<Map<String, String>> privateFieldName=new ArrayList();	// Ltets/Wash,proxy
	private String sourceMethodName;//wash

	private Class sourceClass;
	private Object source;
	private boolean isBefore;
	
	public Object getSource() {
		return source;
	}
	public void setSource(Object source) {
		this.source = source;
	}
	public boolean isBefore() {
		return isBefore;
	}
	public void setBefore(boolean isBefore) {
		this.isBefore = isBefore;
	}
	public String getSourceMethodName() {
		return sourceMethodName;
	}
	public void setSourceMethodName(String sourceMethodName) {
		this.sourceMethodName = sourceMethodName;
	}

	public String getProxyName() {
		return proxyName;
	}
	public void setProxyName(String proxyName) {
		this.proxyName = proxyName;
	}
	public String getTargetName() {
		return targetName;
	}
	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}
	public String getSourceName() {
		return sourceName;
	}
	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}
	public String getProxyMethodName() {
		return proxyMethodName;
	}
	public void setProxyMethodName(String proxyMethodName) {
		this.proxyMethodName = proxyMethodName;
	}
	public List<Map<String, String>> getPrivateFieldName() {
		return privateFieldName;
	}
	public void setPrivateFieldName(List<Map<String, String>> privateFieldName) {
		this.privateFieldName = privateFieldName;
	}
	public Class getSourceClass() {
		return sourceClass;
	}
	public void setSourceClass(Class sourceClass) {
		this.sourceClass = sourceClass;
	}
	
	
}
