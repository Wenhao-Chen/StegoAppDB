package app_analysis.trees.exas;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import apex.symbolic.Expression;
import app_analysis.common.Dirs;
import util.P;

public class Reference {

	public static void main(String[] args) {
		getReferenceTrees();
		for (String group : trees.keySet()) {
			P.p(group+"  "+trees.get(group).size());
		}
		findConflict();
	}
	
	static void findConflict() {
		Map<String, List<Expression>> trees = getReferenceTrees();
		Map<Integer, String> map = new HashMap<>();
		for (String group : trees.keySet()) {
			List<Expression> list = trees.get(group);
			for (int i=0; i<list.size(); i++) {
				Expression exp = list.get(i);
				String id = group+i;
				String raw = exp.toStringRaw();
				if (map.containsKey(raw.hashCode())) {
					P.p(id+" conflict with "+map.get(raw.hashCode()));
				} else
					map.put(raw.hashCode(), id);
			}
		}
		
		P.p(map.size()+"");
		
	}
	/**
	 * Generate reference trees in a trimmed form
	 * 
	 * */
	private static Map<String, List<Expression>> trees;
	static boolean useCompoundPayload = false;
	static boolean useParseIntPayload = true;
	
	public static List<String> getReferenceGroupNames() {
		return new ArrayList<>(getReferenceTrees().keySet());
	}
	public static Map<String, List<Expression>> getReferenceTrees() {
		if (trees == null) {
			trees = new LinkedHashMap<>();
			
			String[] payloadOp = new String[] {"+", "|"};
			// 1. and_mask: cover & mask [+|] payload
			List<Expression> and_mask = new ArrayList<>();
			for (Expression cover : coverTrees()) {
				// use literal as payload
				for (String op : payloadOp) {
					Expression root = new Expression("I", op);
					Expression and = new Expression("I", "&");
					and.add(cover.clone()).add(literal());
					root.add(and).add(literal());
					and_mask.add(root);
				}
				if (useCompoundPayload) {
					for (Expression payload : payloadTrees()) {
						Expression root = new Expression("I", "|");
						Expression and = new Expression("I", "&");
						and.add(cover.clone()).add(literal());
						root.add(and).add(payload.clone());
						and_mask.add(root);
					}
				}
				if (useParseIntPayload) {
					for (Expression payload : parseIntTrees()) {
						Expression root = new Expression("I", "|");
						Expression and = new Expression("I", "&");
						and.add(cover.clone()).add(literal());
						root.add(and).add(payload.clone());
						and_mask.add(root);
					}
				}
			}
			
			trees.put("and_mask", and_mask);
			
			// 2. div_mul: cover / base * base [+|] payload
			List<Expression> div_mul = new ArrayList<>();
			for (Expression cover : coverTrees()) {
				for (String op : payloadOp){
					Expression root = new Expression("I", op);
					Expression div = new Expression("I", "/").add(cover.clone()).add(literal());
					Expression mul = new Expression("I", "*").add(div).add(literal());
					root.add(mul).add(literal());
					div_mul.add(root);
				}
			}
			trees.put("div_mul", div_mul);
			
			
			// 3. div_shl: cover / base << bits [+|] payload
			List<Expression> div_shl = new ArrayList<>();
			for (Expression cover : coverTrees())
			for (String op : payloadOp) {
				Expression root = new Expression("I", op);
				Expression div = new Expression("I", "/").add(cover.clone()).add(literal());
				Expression shl = new Expression("I", "<<").add(div).add(literal());
				root.add(shl).add(literal());
				div_shl.add(root);
			}
			trees.put("div_shl", div_shl);
			
			// 4. shr_shl: cover [>> >>>] bits << bits [+|] payload
			List<Expression> shr_shl = new ArrayList<>();
			for (Expression cover : coverTrees())
			for (String shrOp : new String[] {">>", ">>>"})
			for (String op : payloadOp) {
				Expression root = new Expression("I", op);
				Expression shr = new Expression("I", shrOp).add(cover.clone()).add(literal());
				Expression shl = new Expression("I", "<<").add(shr).add(literal());
				root.add(shl).add(literal());
				shr_shl.add(root);
			}
			trees.put("shr_shl", shr_shl);
			
			// 5. sub_mod: cover - cover % base [+|] payload
			List<Expression> sub_mod = new ArrayList<>();
			for (Expression cover : coverTrees())
			for (String op : payloadOp) {
				Expression root = new Expression("I", op);
				Expression mod = new Expression("I", "%").add(cover.clone()).add(literal());
				Expression sub = new Expression("I", "-").add(cover.clone()).add(mod);
				root.add(sub).add(literal());
				sub_mod.add(root);
			}
			trees.put("sub_mod", sub_mod);
			
			// 6. one_step:
			//    cover [+|] literal
			//    cover & (literal << bits)
			List<Expression> one_step = new ArrayList<>();
			for (Expression cover : coverTrees()) {
				for (String op : payloadOp){
					Expression root = new Expression("I", op);
					root.add(cover.clone()).add(literal());
					one_step.add(root);
				}
				if (useCompoundPayload)
					for (Expression payload : payloadTrees()) {
						Expression root = new Expression("I", "|");
						root.add(cover.clone()).add(payload.clone());
						and_mask.add(root);
					}
				if (useParseIntPayload)
					for (Expression payload : parseIntTrees()) {
						Expression root = new Expression("I", "|");
						root.add(cover.clone()).add(payload.clone());
						and_mask.add(root);
					}
			}
			trees.put("one_step", one_step);
			
			
			// 7. sum of parse int:
			//   literal + (parseInt * literal) + (parseInt * literal)
			Expression parseInt = new Expression("I", "return");
			parseInt.add("Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I").add("reference");
			Expression parseIntMul = new Expression("I", "*");
			parseIntMul.add(parseInt).add(literal());
			Expression firstAdd = new Expression("I", "+");
			firstAdd.add(literal()).add(parseIntMul.clone());
			Expression secondAdd = new Expression("I", "+");
			secondAdd.add(firstAdd).add(parseIntMul.clone());
			trees.put("parse_int", Arrays.asList(secondAdd));
			
			savePDFs();
		}
		return trees;
	}
	
	public static Map<String, List<Expression>> getReferenceTrees2() {
		if (trees == null) {
			trees = new LinkedHashMap<>();
			
			String[] payloadOp = new String[] {"+", "|"};
			// 1. and_mask: cover & mask [+|] payload
			List<Expression> and_mask = new ArrayList<>();
			for (Expression cover : coverTrees()) {
				// use literal as payload
				for (String op : payloadOp) {
					Expression root = new Expression("I", op);
					Expression and = new Expression("I", "&");
					and.add(cover.clone()).add(literal());
					root.add(and).add(literal());
					and_mask.add(root);
				}
				if (useCompoundPayload) {
					for (Expression payload : payloadTrees()) {
						Expression root = new Expression("I", "|");
						Expression and = new Expression("I", "&");
						and.add(cover.clone()).add(literal());
						root.add(and).add(payload.clone());
						and_mask.add(root);
					}
				}
				if (useParseIntPayload) {
					for (Expression payload : parseIntTrees()) {
						Expression root = new Expression("I", "|");
						Expression and = new Expression("I", "&");
						and.add(cover.clone()).add(literal());
						root.add(and).add(payload.clone());
						and_mask.add(root);
					}
				}
			}
			
			trees.put("and_mask", and_mask);
			
			// 2. div_mul: cover / base * base [+|] payload
			List<Expression> div_mul = new ArrayList<>();
			for (Expression cover : coverTrees()) {
				for (String op : payloadOp){
					Expression root = new Expression("I", op);
					Expression div = new Expression("I", "/").add(cover.clone()).add(literal());
					Expression mul = new Expression("I", "*").add(div).add(literal());
					root.add(mul).add(literal());
					div_mul.add(root);
				}
			}
			trees.put("div_mul", div_mul);
			
			
			// 3. div_shl: cover / base << bits [+|] payload
			List<Expression> div_shl = new ArrayList<>();
			for (Expression cover : coverTrees())
			for (String op : payloadOp) {
				Expression root = new Expression("I", op);
				Expression div = new Expression("I", "/").add(cover.clone()).add(literal());
				Expression shl = new Expression("I", "<<").add(div).add(literal());
				root.add(shl).add(literal());
				div_shl.add(root);
			}
			trees.put("div_shl", div_shl);
			
			// 4. shr_shl: cover [>> >>>] bits << bits [+|] payload
			List<Expression> shr_shl = new ArrayList<>();
			for (Expression cover : coverTrees())
			for (String shrOp : new String[] {">>", ">>>"})
			for (String op : payloadOp) {
				Expression root = new Expression("I", op);
				Expression shr = new Expression("I", shrOp).add(cover.clone()).add(literal());
				Expression shl = new Expression("I", "<<").add(shr).add(literal());
				root.add(shl).add(literal());
				shr_shl.add(root);
			}
			trees.put("shr_shl", shr_shl);
			
			// 5. sub_mod: cover - cover % base [+|] payload
			List<Expression> sub_mod = new ArrayList<>();
			for (Expression cover : coverTrees())
			for (String op : payloadOp) {
				Expression root = new Expression("I", op);
				Expression mod = new Expression("I", "%").add(cover.clone()).add(literal());
				Expression sub = new Expression("I", "-").add(cover.clone()).add(mod);
				root.add(sub).add(literal());
				sub_mod.add(root);
			}
			trees.put("sub_mod", sub_mod);
			
			// 6. one_step:
			//    cover [+|] literal
			//    cover & (literal << bits)
			List<Expression> one_step = new ArrayList<>();
			for (Expression cover : coverTrees()) {
				for (String op : payloadOp){
					Expression root = new Expression("I", op);
					root.add(cover.clone()).add(literal());
					one_step.add(root);
				}
				if (useCompoundPayload)
					for (Expression payload : payloadTrees()) {
						Expression root = new Expression("I", "|");
						root.add(cover.clone()).add(payload.clone());
						and_mask.add(root);
					}
				if (useParseIntPayload)
					for (Expression payload : parseIntTrees()) {
						Expression root = new Expression("I", "|");
						root.add(cover.clone()).add(payload.clone());
						and_mask.add(root);
					}
			}
			trees.put("one_step", one_step);
			
			
			// 7. sum of parse int:
			//   literal + (parseInt * literal) + (parseInt * literal)
			Expression parseInt = new Expression("I", "return");
			parseInt.add("Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I").add("reference");
			Expression parseIntMul = new Expression("I", "*");
			parseIntMul.add(parseInt).add(literal());
			Expression firstAdd = new Expression("I", "+");
			firstAdd.add(literal()).add(parseIntMul.clone());
			Expression secondAdd = new Expression("I", "+");
			secondAdd.add(firstAdd).add(parseIntMul.clone());
			trees.put("parse_int", Arrays.asList(secondAdd));
			
			savePDFs();
		}
		return trees;
	}
	
	
	static final String[] ARGB_SIGS = {
			"Landroid/graphics/Color;->red(I)I",
			"Landroid/graphics/Color;->green(I)I",
			"Landroid/graphics/Color;->blue(I)I",
			"Landroid/graphics/Color;->alpha(I)I"
	};
	
	// cover subtrees for whole pixel or single channels
	static List<Expression> coverTrees() {
		List<Expression> list = new ArrayList<>();
		// API form whole pixel
		list.add(getPixel());
		// API form separate channels
		for (String sig : ARGB_SIGS) {
			Expression channel = new Expression("I", "return");
			channel.add(sig).add(getPixel());
			list.add(channel);
		}
		// arithmetic form: direct mask on whole pixel
		Expression root = new Expression("I", "&");
		root.add(getPixel()).add(literal());
		list.add(root);
		// arithmetic form: shift then mask
		root = new Expression("I", "&");
		Expression left = new Expression("I", ">>");
		left.add(getPixel()).add(literal());
		root.add(left).add(literal());
		list.add(root);
		return list;
	}
	
	// cover subtrees for whole pixel or single channels
	static List<Expression> coverTrees2() {
		List<Expression> list = new ArrayList<>();
		// API form separate channels
		for (String sig : ARGB_SIGS) {
			Expression channel = new Expression("I", "return");
			channel.add(sig).add(getPixel());
			list.add(channel);
		}
		// API form whole pixel
		list.add(getPixel());
		// arithmetic form: direct mask on whole pixel
		Expression root = new Expression("I", "&");
		root.add(getPixel()).add(literal());
		list.add(root);
		// arithmetic form: shift then mask
		root = new Expression("I", "&");
		Expression left = new Expression("I", ">>");
		left.add(getPixel()).add(literal());
		root.add(left).add(literal());
		list.add(root);
		return list;
	}
	
	static List<Expression> payloadTrees() {
		Expression masked = new Expression("I", "&");
		masked.add(literal()).add(literal());
		Expression shifted = new Expression("I", "<<");
		shifted.add(literal()).add(literal());
		return Arrays.asList(masked, shifted);
	}
	
	static List<Expression> parseIntTrees() {
		Expression parseInt1 = new Expression("I", "return");
		parseInt1.add("Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I").add("reference");
		Expression parseInt2 = new Expression("I", "return");
		parseInt2.add("Ljava/lang/Integer;->parseInt(Ljava/lang/String;I)I").add("reference").add("literal");
		return Arrays.asList(parseInt1, parseInt2);
	}
	
	static Expression getPixel() {
		Expression wholePixel = new Expression("I", "return");
		wholePixel.add("Landroid/graphics/Bitmap;->getPixel(II)I")
				  .add("reference").add(literal()).add(literal());
		return wholePixel;
	}
	
	static Expression literal() {
		return new Expression("I", "literal");
	}
	
	static void savePDFs() {
		File dir = new File(Dirs.ExasRoot, "_reference");
		dir.mkdirs();
		for (Map.Entry<String, List<Expression>> entry : trees.entrySet()) {
			String group = entry.getKey();
			List<Expression> exps = entry.getValue();
			//P.p(group+" "+exps.size());
			for (int i=0; i<exps.size(); ++i) {
				String name = group+"_"+(i+1);
				//P.p("saving "+name+" in "+dir.getAbsolutePath());
				exps.get(i).toDotGraph(name, dir, false);
			}
		}
	}
}
