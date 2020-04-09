package database.stego.apps.passlok;

import java.util.ArrayList;
import java.util.List;

public class Isaac {

	
	int[] m = new int[256];
	int[] r = new int[256];
	public int acc = 0, brs = 0, cnt = 0, gnt = 0, resetCount = 0;
	
	private class Seed {
		int a, b, c, d, e, f, g, h, i;
		private Seed(int num)
	    {
	    	this(new int[] {num});
	    }
	    private Seed(String string)
	    {
	    	this(stringToIntArray(string));
	    }
	    private Seed(int[] nums)
	    {
	    	/* seeding the seeds of love */
		    a = b = c = d =
		    e = f = g = h = 0x9e3779b9; /* the golden ratio */
		    reset();
		    for(i = 0; i < nums.length; i++)
		        r[i & 0xff] += nums[i];
		    
		    for(i = 0; i < 4; i++) /* scramble it */
		        seed_mix();

		    for(i = 0; i < 256; i += 8)
		    {
		    	a = add(a, r[i + 0]); b = add(b, r[i + 1]);
		    	c = add(c, r[i + 2]); d = add(d, r[i + 3]);
		    	e = add(e, r[i + 4]); f = add(f, r[i + 5]);
		    	g = add(g, r[i + 6]); h = add(h, r[i + 7]);
		        seed_mix();
		        /* fill in m[] with messy stuff */
		        m[i + 0] = a; m[i + 1] = b; m[i + 2] = c; m[i + 3] = d;
		        m[i + 4] = e; m[i + 5] = f; m[i + 6] = g; m[i + 7] = h;
		    }
		    /* do a second pass to make all of the seed affect all of m[] */
		    for(i = 0; i < 256; i += 8)
		    {
		        a = add(a, m[i + 0]); b = add(b, m[i + 1]);
		        c = add(c, m[i + 2]); d = add(d, m[i + 3]);
		        e = add(e, m[i + 4]); f = add(f, m[i + 5]);
		        g = add(g, m[i + 6]); h = add(h, m[i + 7]);
		        seed_mix();
		        /* fill in m[] with messy stuff (again) */
		        m[i + 0] = a; m[i + 1] = b; m[i + 2] = c; m[i + 3] = d;
		        m[i + 4] = e; m[i + 5] = f; m[i + 6] = g; m[i + 7] = h;
		    }
		    prng(); /* fill in the first set of results */
		    gnt = 256;  /* prepare to use the first set of results */;
	    }
	    private void seed_mix()
	    {
	        a ^= b <<  11; d = add(d, a); b = add(b, c);
	        b ^= c >>>  2; e = add(e, b); c = add(c, d);
	        c ^= d <<   8; f = add(f, c); d = add(d, e);
	        d ^= e >>> 16; g = add(g, d); e = add(e, f);
	        e ^= f <<  10; h = add(h, e); f = add(f, g);
	        f ^= g >>>  4; a = add(a, f); g = add(g, h);
	        g ^= h <<   8; b = add(b, g); h = add(h, a);
	        h ^= a >>>  9; c = add(c, h); a = add(a, b);
	    }
	}
	
	Isaac()
	{
		new Seed((int)(Math.random()*0xffffffff));
	}
	
	Isaac(String seed)
	{
		setSeed(seed);
	}
	
	public void setSeed(String seed)
	{
		new Seed(seed);
	}
	
	public double random()
	{
		return 0.5 + rand()*2.3283064365386963e-10;
	}
	
	
	public int rand()
	{
		if (gnt-- == 0)
		{
			prng();
			gnt = 255;
			resetCount++;
		}
		return r[gnt];
	}
	
	public void reset()
	{
		acc = brs = cnt = 0;
	    for(int i = 0; i < 256; ++i)
	      m[i] = r[i] = 0;
	    gnt = 0;
	}
	
	
	public void prng()
	{
		prng(1);
	}
	
	public void prng(int n)
	{
	    int i, x, y;

	    n = (int) Math.abs(Math.floor(n));

	    while(n--!=0)
	    {
			cnt = add(cnt,   1);
			brs = add(brs, cnt);
			
			for(i = 0; i < 256; i++)
			{
				switch(i & 3)
				{
					case 0: acc ^= acc <<  13; break;
				  	case 1: acc ^= acc >>>  6; break;
				  	case 2: acc ^= acc <<   2; break;
				  	case 3: acc ^= acc >>> 16; break;
				}
				acc        = add(m[(i +  128) & 0xff], acc); x = m[i];
				m[i] =   y = add(m[(x >>>  2) & 0xff], add(acc, brs));
				r[i] = brs = add(m[(y >>> 10) & 0xff], x);
			}
	    }
	}
	
	private int add(int x, int y)
	{
		int lsb = (x & 0xffff) + (y & 0xffff);
		int msb = (x >>>   16) + (y >>>   16) + (lsb >>> 16);
	    return (msb << 16) | (lsb & 0xffff);
	}
	
 	private static int[] stringToIntArray(String s)
	{
		int w1, w2, u, i = 0;
		s = s + "\u0000\u0000\u0000"; // pad string to avoid discarding last chars
		int l = s.length() - 1;
		List<Integer> r4 = new ArrayList<>();
		List<Integer> r = new ArrayList<>();

		while(i < l) 
		{
			w1 = s.charAt(i++);
			w2 = i+1<s.length()?s.charAt(i+1):0;
			if (w1 < 0x0080)
			{
				// 0x0000 - 0x007f code point: basic ascii
				r4.add(w1);
			}
			else if(w1 < 0x0800)
			{
				// 0x0080 - 0x07ff code point
				r4.add(((w1 >>>  6) & 0x1f) | 0xc0);
				r4.add(((w1 >>>  0) & 0x3f) | 0x80);
			}
			else if((w1 & 0xf800) != 0xd800)
			{
				// 0x0800 - 0xd7ff / 0xe000 - 0xffff code point
				r4.add(((w1 >>> 12) & 0x0f) | 0xe0);
				r4.add(((w1 >>>  6) & 0x3f) | 0x80);
				r4.add(((w1 >>>  0) & 0x3f) | 0x80);
			}
			else if(((w1 & 0xfc00) == 0xd800) && ((w2 & 0xfc00) == 0xdc00))
			{
				// 0xd800 - 0xdfff surrogate / 0x10ffff - 0x10000 code point
				u = ((w2 & 0x3f) | ((w1 & 0x3f) << 10)) + 0x10000;
				r4.add(((u >>> 18) & 0x07) | 0xf0);
				r4.add(((u >>> 12) & 0x3f) | 0x80);
				r4.add(((u >>>  6) & 0x3f) | 0x80);
				r4.add(((u >>>  0) & 0x3f) | 0x80);
				i++;
			}
			else
			{
			  // invalid char
			}
			/* add integer (four utf-8 value) to array */
			if(r4.size() > 3)
			{
				// little endian
				r.add((r4.remove(0) <<  0) | (r4.remove(0) <<  8) | (r4.remove(0) << 16) | (r4.remove(0) << 24));
			}
		}
		int[] result = new int[r.size()];
		for (i = 0; i < result.length; i++)
			result[i] = r.get(i);
		return result;
	}
}
