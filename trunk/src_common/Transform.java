import java.io.*;
import java.math.*;

import uk.ac.gla.terrier.structures.Matrix;
import uk.ac.gla.terrier.structures.indexing.MatrixBuilder;
public class Transform {

        /**
         * @param args
         */
        public static void main(String[] args) throws Exception{
        	if (args[0].equals("--transform")){
                // --transform <oldmatrix> <newmatrix>
                String pathtdmat = args[1];
                String pathmatrix = args[2];
                BufferedReader br = new BufferedReader(new FileReader(pathtdmat));
                MatrixBuilder mb = new MatrixBuilder(pathmatrix);
                String line = null;
                int id = 0;
                while(null!=(line = br.readLine())){
                        String[] strline = line.split(":");
                        String[] vecstr = strline[1].replace("(", "").replace(")", ";").split(";");
                        int[] index = new int[vecstr.length];
                        double[] value = new double[vecstr.length];
                        int[][] vector = new int[3][vecstr.length];
                        //System.out.println("id "+(id++)+" length "+vecstr.length);
                        for(int i = 0; i<vecstr.length; i++){
                                String[] data = vecstr[i].split(",");
                                vector[0][i] = Integer.valueOf(data[0]);//termid
                                value[i] = Double.valueOf(data[1]);//value
                                BigDecimal b = new BigDecimal(Double.toString(value[i]));
                                vector[1][i]=b.unscaledValue().intValue();//unscale
                                vector[2][i]=b.scale();//scale

                        }
                        mb.addVector(vector);
                }
                br.close();
                mb.close();
        	}else if (args[0].equals("--print")){
        		Matrix matrix = new Matrix(args[1]);
        		matrix.print();
        		matrix.close();
        	}
            
        }
        

}
