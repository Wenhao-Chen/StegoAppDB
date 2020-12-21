package app_analysis.trees;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import apex.symbolic.Expression;
import app_analysis.common.Dirs;
import util.P;

public class TreeRefUtils {

	public static Expression refTrees[];
	public static Map<String, Expression> trees;
	static {
		init();
	}
	
	static final void init() {
		
		trees = new LinkedHashMap<>();
		// one step embedding
		trees.put("one_step_1", deserialize("+", "Y", "literal", "", "", "0x1"));
		trees.put("one_step_2", deserialize("+", "&", "literal", "Y", "literal", "0x1", "", "", "", "0xff"));
		trees.put("one_step_3", deserialize("+", "Y", "literal", "", "", "-0x1"));
		trees.put("one_step_4", deserialize("+", "&", "literal", "Y", "literal", "-0x1", "", "", "", "0xff"));
		trees.put("one_step_5", deserialize("|", "Y", "literal", "", "", "0x1"));
		trees.put("one_step_6", deserialize("|", "&", "literal", "Y", "literal", "0x1", "", "", "", "0xff"));
		trees.put("one_step_7", deserialize("|", "Y", "<<", "", "", "literal", "X", "0x1"));
		trees.put("one_step_8", deserialize("|", "&", "<<", "Y", "literal", "literal", "X", "", "", "0xff", "", "0x1", ""));
		trees.put("one_step_9", deserialize("&", "Y", "~", "", "", "<<", "", "literal", "X", "0x1"));
		
		trees.put("special_1", deserialize("+", "Y", "*", "","", "*","X","literal","X","","","0x1"));
		trees.put("special_2", deserialize("+", "Y", "*", "","", "*","X","literal","X","","","-0x1"));
		trees.put("special_3",  new Expression("I", "return")
								.add("Landroid/graphics/Color;->argb(IIII)I")
								.add(deserialize("literal", "0xfe"))
								.add(deserialize("return", "$Red", "$GetPixel"))
								.add(deserialize("return", "$Green", "$GetPixel"))
								.add(deserialize("return", "$Blue", "$GetPixel")));
		
		
		List<Expression> twoStep = new ArrayList<>();
		twoStep.add(deserialize("|", "X"));
		twoStep.add(deserialize("+", "X"));
		for (int i=0; i<2; i++) {
			Expression stepTwo = twoStep.get(i);
			Map<String, Expression> eraseTrees = eraseLSBTrees();
			for (String label : eraseTrees.keySet()) {
				Expression erase = eraseTrees.get(label);
				Expression exp = stepTwo.clone();
				exp.children.add(0, erase);
				trees.put(label+"_"+(i==0?"or":"add"), exp);
			}
		}
		
		refTrees = new Expression[trees.size()];
		int i = 0;
		File dir = new File(Dirs.Desktop, "ref_trees");
		dir.mkdirs();
		for (String label : trees.keySet()) {
			refTrees[i] = trees.get(label);
			refTrees[i++].toDotGraph(label, dir, false);
		}
	}
	
	static Map<String, Expression> eraseLSBTrees() {
		Map<String, Expression> res = new HashMap<>();
		String[] and = new String[] {
				"0xfe", "0xfc", "0xf8", "0xf0",
				"-0x2", "-0x4", "-0x8", "-0x10", "-0x30304","-0x10102"
		};
		String[] mod = new String[] {"0x2","0x4","0x8","0x10", "0xa"};
		String[] shr = new String[] {"0x1", "0x2", "0x3", "0x4"};
		String[] div = new String[] {"0x2", "0x4", "0x8", "0x10"};
		
		for (int i=0; i < and.length; i++) {
			res.put("and_mask_"+(i+1), deserialize("&", "X", "literal", "", "", and[i]));
			//res.get(res.size()-1).toDotGraph("and_"+s, Dirs.Desktop, false);
		}
		for (int i=0; i < mod.length; i++) {
			res.put("sub_mod_"+(i+1), deserialize("-", "X", "%", "", "", "X", "literal", "", "", mod[i]));
			//res.get(res.size()-1).toDotGraph("mod_"+s, Dirs.Desktop, false);
		}
		for (int i=0; i < shr.length; i++) {
			res.put("shr_shl_"+(i*2+1), deserialize("<<", ">>>", "literal", "X", 
					"literal", shr[i], "", "", "", shr[i]));
			res.put("shr_shl_"+(i*2+2), deserialize("<<", ">>", "literal", "X", 
					"literal", shr[i], "", "", "", shr[i]));
			//res.get(res.size()-1).toDotGraph("shr_"+s, Dirs.Desktop, false);
		}
		for (int k=1; k <= 4; k++) {
			res.put("div_shl_"+k, deserialize("<<", "/", "literal", "X", "literal", 
					"0x"+k, "", "", "", div[k-1]));
			res.put("div_mul_"+k, deserialize("*", "/", "literal", "X", "literal", 
					div[k-1], "", "", "", div[k-1]));
			//res.get(res.size()-1).toDotGraph("div_"+k, Dirs.Desktop, false);
		}
		res.put("div_mul_5", deserialize("*", "/", "literal", "X", "literal", 
				"0xa", "", "", "", "0xa")); // base-10 LSB
		res.put("div_mul_6", deserialize("*", "/", "literal", "X", "literal", 
				"0x64", "", "", "", "0x64")); // base-10 LS2B
		res.put("sub_1", deserialize("+", "X", "literal", "", "", "-0x1"));
		//res.get(res.size()-1).toDotGraph("div_"+k, Dirs.Desktop, false);
		return res;
	}
	

	// return value: whether this tree contains getPixel() call
	public static boolean normalize(Expression exp) {
		// turn "return - getPixel()" to "Pixel"
		// put "literal - xx" to the right child
		// put "$GetPixel" to the left???
		if (exp == null)
			return false;
		if (exp.root.contentEquals("return")) {
			String sig = exp.children.get(0).root;
			if (sig.contentEquals("Landroid/graphics/Bitmap;->getPixel(II)I")) {
				exp.children.clear();
				exp.root = "$GetPixel";
				return true;
			} else if (sig.contentEquals("Landroid/graphics/Color;->red(I)I")) {
				exp.children.get(0).root = "$Red";
			} else if (sig.contentEquals("Landroid/graphics/Color;->green(I)I")) {
				exp.children.get(0).root = "$Green";
			} else if (sig.contentEquals("Landroid/graphics/Color;->blue(I)I")) {
				exp.children.get(0).root = "$Blue";
			} else if (sig.contentEquals("Landroid/graphics/Color;->alpha(I)I")) {
				exp.children.get(0).root = "$Alpha";
			}
		}
		else if (exp.root.contentEquals("literal")) {
			String num = exp.children.get(0).root;
			if (P.isInteger(num)) {
				int val = Integer.parseInt(num);
				exp.children.get(0).root = val >=0 ? "0x"+Integer.toHexString(val):
													"-0x"+Integer.toHexString(-val);
			} else if (num.length()>20)
				exp.children.get(0).root = "$trimmed";
		}
		
		boolean hasGetPixel = false;
		boolean childRes[] = new boolean[exp.children.size()];
		int i=0;
		for (Expression child : exp.children) {
			childRes[i] = normalize(child);
			hasGetPixel |= childRes[i++];
		}
			
		
		if (exp.children.size()==2) {
			if (!exp.root.contentEquals("return") && childRes[1] && !childRes[0]) { // if GetPixel is on the right but not the left, swap them
				Expression temp = exp.children.get(0);
				exp.children.set(0, exp.children.get(1));
				exp.children.set(1, temp);
			}
		}
		return hasGetPixel;
	}
	
	// in addition to normalization, also reduce the non-getPixel()
	// nodes to X
	public static void normalizeAndTrim(Expression exp) {
		normalize(exp);
		trim(exp);
	}
	
	
	public static Expression trim2(Expression exp) {
		if (exp == null)
			return null;
		
		if (exp.root.equals("literal") || exp.root.equals("reference"))
			exp.children.clear();
		if (exp.root.equals("return") && 
				exp.children.get(0).root.equals("Landroid/graphics/Bitmap;->getPixel(II)I")) {
			exp.children.clear();
			exp.add("Landroid/graphics/Bitmap;->getPixel(II)I")
			   .add("reference").add("literal").add("literal");
		}
		for (Expression child : exp.children)
			trim2(child);
		
		if (!exp.root.equals("^") && exp.children.size()==2 && 
				exp.children.get(0).root.equals("literal") && 
				exp.children.get(1).root.equals("literal")) {
			exp.root = "literal";
			exp.children.clear();
		}
		return exp;
	}
	
	public static Expression trim(Expression exp) {
		if (exp == null)
			return null;
		if (exp.root.equals("literal") || exp.root.equals("reference"))
			exp.children.clear();
		if (exp.root.equals("return") && 
				exp.children.get(0).root.equals("Landroid/graphics/Bitmap;->getPixel(II)I")) {
			exp.children.clear();
			exp.add("reference").add("literal").add("literal");
		}
		for (Expression child : exp.children)
			trim(child);
		return exp;
	}

	static Expression deserialize(String... s) {
		if (s.length<1)
			return null;
		int index = 0;
		Expression res = new Expression(s[index++]);
		Queue<Expression> q = new LinkedList<>();
		q.add(res);
		while (!q.isEmpty() && index < s.length) {
			Expression curr = q.poll();
			for (int i=0; i<2; i++)
			if (index < s.length) {
				if (!s[index].isEmpty()) {
					Expression child = new Expression(s[index]);
					curr.add(child);
					q.add(child);
				}
				index++;
			}
		}
		return res;
	}
}
