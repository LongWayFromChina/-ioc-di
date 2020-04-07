package com.lmt.util;

import java.io.File;
import java.util.List;

public class FileUtil {
	public static void getAllFile(File dir,List<File> list){
		if(dir.exists()) {
			File[] fileList=dir.listFiles();
			for(File file:fileList) {
				if(file.isDirectory()) {
					getAllFile(file,list);
				}else {
					list.add(file);
					
				}
			}
		}else {
			System.out.println("Â·¾¶²»´æÔÚ");
		}
	}
}
