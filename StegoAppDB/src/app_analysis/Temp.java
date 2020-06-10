package app_analysis;

import java.util.Random;

public class Temp {

	public static void main(String[] args)
	{
		BlockForDCT block = new BlockForDCT();
		
		int[][] cover = new int[8][8];
		Random rng = new Random(0);
		for (int i=0; i<8; i++)
		for (int j=0; j<8; j++) {
			cover[i][j] = 10 + rng.nextInt(246);
		}
		block.create(cover, 0, 0);
		print("cover", block.mask);
		block.dct();
		print("dct", block.mask);
		block.inverseDct();
		print("idct", block.mask);
	}
	
	static void print(String label, int[][] arr) {
		System.out.println("------  "+label+"  -------");
		for (int[] row : arr) {
			for (int i : row)
				System.out.printf("%3d ", i);
			System.out.println();
		}
		System.out.println("-------------\n");
	}

	public static class BlockForDCT
	{
	    private int[][] mask;

	    public void create(int[][] tab, int startX, int startY)
	    {
	        int[][] temp = new int[8][8];

	        for (int i = 0; i<8; i++)
	        {
	            for (int j = 0; j<8; j++)
	            {
	                temp[i][j]=tab[startX + i][startY + j];
	            }
	        }
	        this.mask=temp;
	    }

	    public int[][] restore(int[][] imageMatrix, int startX, int startY)
	    {
	        int[][] temp = imageMatrix;

	        for (int i=0;i<8;i++)
	        {
	            for (int j=0;j<8;j++)
	            {
	                temp[startX + i][startY + j]=mask[i][j];
	            }
	        }
	        return temp;
	    }

	    public int[][] getBlockMask()
	    {
	        return this.mask;
	    }

	    private double C(int u)
	    {
	        double c;
	        if (u == 0)
	            c = (1.0/Math.sqrt(2.0));
	        else
	            c = 1.0;
	        return c;
	    }

	    public void dct()
	    {
	        int[][] temp = new int[8][8];

	        double Cu, Cv;

	        for (int u = 0; u< 8; u++)
	        {
	            for (int v = 0; v< 8; v++)
	            {
	                double sum=0.0;

	                for (int x = 0; x< 8; x++)
	                {
	                    for (int y = 0; y< 8; y++)
	                    {
	                        sum += ((double)this.mask[x][y])*Math.cos((Math.PI*u*(2.0*(double)x+1.0))/(16.0))*Math.cos((Math.PI*v*(2.0*(double)y+1.0))/(16.0));
	                    }
	                }
	                sum *= (2.0*(C(u)*C(v))/8.0);
	                Long result = Math.round(sum);
	                temp[u][v] = result.intValue();
	            }
	        }
	        this.mask=temp;
	    }

	    public void inverseDct()
	    {
	        int[][] temp = new int[8][8];

	        double Cu, Cv;

	        for (int x=0;x<8;x++)
	        {
	            for (int y=0;y<8;y++)
	            {
	                double sum=0.0;

	                for (int u=0;u<8;u++)
	                {
	                    for (int v=0;v<8;v++)
	                    {
	                        sum+=C(u)*C(v)*((double)this.mask[u][v])*Math.cos((Math.PI*u*(2.0*x+1.0))/(16.0))*Math.cos((Math.PI*v*(2.0*y+1.0))/(16.0));
	                    }
	                }
	                sum *= (2.0/8.0);
	                Long result = Math.round((sum));
	                temp[x][y]=result.intValue();
	            }
	        }
	        this.mask = temp;
	    }

	    public void hideSecretBit(int bit, int which, int u, int v)
	    {
	        if (bit==1)
	            this.mask[u][v] |= (1<<which);
	        else
	            this.mask[u][v] &= ~(1<<which);

	    }

	    public int revealSecretBit(int which, int u, int v)
	    {
	        return (this.mask[u][v] >> which) & 1;

	    }




	}


}
