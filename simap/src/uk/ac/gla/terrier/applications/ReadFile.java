/*
 * Created on 9 Apr 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.applications;

import java.io.BufferedReader;
import java.io.IOException;

import uk.ac.gla.terrier.utility.Files;

public class ReadFile {
	
	static void getStringByOffset(String filename, int offset, int length){
		try{
			BufferedReader br = Files.openFileReader(filename);
			char[] ch = new char[length];
			//System.out.println(br.read(ch, offset, length));
			br.skip(offset);
			br.read(ch);
			//for (int i=0; i<length; i++)
				//ch[i] = (char)br.read();
			br.close();
			System.out.println("-----------------");
			System.out.println(String.copyValueOf(ch));
			System.out.println("-----------------");
			System.out.println("filename: "+filename+", offset: "+offset+", length: "+length);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	static char[] getTextByOffset(String filename, int offset, int length){
		char[] ch = new char[length];
		try{
			BufferedReader br = Files.openFileReader(filename);
			
			//System.out.println(br.read(ch, offset, length));
			br.skip(offset);
			br.read(ch);
			//for (int i=0; i<length; i++)
				//ch[i] = (char)br.read();
			br.close();
			//System.out.println(String.copyValueOf(ch));
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		return ch;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args[0].equals("--getstringbyoffset")){
			ReadFile.getStringByOffset(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]));
		}
	}

}
