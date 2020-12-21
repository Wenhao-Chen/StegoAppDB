package app_analysis.trees.exas;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

import apex.symbolic.Expression;
import app_analysis.common.Dirs;
import util.Graphviz;
import util.P;

public class Reference2 {

	public static void main(String[] args) {
		//allGroups();
		//coverTrees();
		//payloadTrees();
		//and_mask();
	}
	
	public static void everyTree() {
		int sum = 0;
		Map<String, List<Expression>> trees = getReferenceTrees2();
		for (String group : trees.keySet()) {
			P.p(group+" "+trees.get(group).size());
			sum += trees.get(group).size();
			int index = 1;
			for (int i=0; i<trees.get(group).size()/6; i++) {
				List<Expression> list = new ArrayList<Expression>();
				for (int j=i*6; j<i*6+6 && j<trees.get(group).size(); j++) {
					trees.get(group).get(j).note = group+"_"+(index++);
					list.add(trees.get(group).get(j));
				}
				toDotGraph(list, group+"_"+(i+1), 3, group+(i+1), Dirs.Desktop);
			}
		}
		P.p("total " +sum);
	}
	
	public static Map<String, List<Expression>> getReferenceTrees2() {
		Map<String, List<Expression>> trees = new TreeMap<String, List<Expression>>();
		String[] payloadOp = new String[] {"+", "|"};
		// 1. and_mask: cover & mask [+|] payload
		List<Expression> and_mask = new ArrayList<>();
		for (Expression cover : coverTrees()) {
			// use literal as payload
			for (Expression payload : payloadTrees()) {
				Expression root = new Expression("I", "+ |");
				Expression and = new Expression("I", "&");
				and.add(cover.clone()).add(literal("0xFE, 0xFC, 0xF8, 0xF0"));
				root.add(and).add(payload.clone());
				and_mask.add(root);
			}
		}
		trees.put("and_mask", and_mask);
			
		// 2. div_mul: cover / base * base [+|] payload
		List<Expression> div_mul = new ArrayList<>();
		for (Expression cover : coverTrees()) {
			for (Expression payload : payloadTrees()) {
				Expression root = new Expression("I", "+ |");
				Expression div = new Expression("I", "/").add(cover.clone()).add(literal("2, 4, 8, 10"));
				Expression mul = new Expression("I", "*").add(div).add(literal("2, 4, 8, 10"));
				root.add(mul).add(payload.clone());
				div_mul.add(root);
			}
		}
		trees.put("div_mul", div_mul);
			
		// 3. div_shl: cover / base << bits [+|] payload
		List<Expression> div_shl = new ArrayList<>();
		for (Expression cover : coverTrees())
		for (Expression payload : payloadTrees()) {
			Expression root = new Expression("I", "+ |");
			Expression div = new Expression("I", "/").add(cover.clone()).add(literal("2, 4, 8, 16"));
			Expression shl = new Expression("I", "<<").add(div).add(literal("1, 2, 3, 4"));
			root.add(shl).add(payload.clone());
			div_shl.add(root);
		}
		trees.put("div_shl", div_shl);
			
		// 4. shr_shl: cover [>> >>>] bits << bits [+|] payload
		List<Expression> shr_shl = new ArrayList<>();
		for (Expression cover : coverTrees())
		for (Expression payload : payloadTrees()) {
			Expression root = new Expression("I", "+ |");
			Expression shr = new Expression("I", ">> >>>").add(cover.clone()).add(literal("1, 2, 3, 4"));
			Expression shl = new Expression("I", "<<").add(shr).add(literal("1, 2, 3, 4"));
			root.add(shl).add(payload.clone());
			shr_shl.add(root);
		}
		trees.put("shr_shl", shr_shl);
			
		// 5. sub_mod: cover - cover % base [+|] payload
		List<Expression> sub_mod = new ArrayList<>();
		for (Expression cover : coverTrees())
		for (Expression payload : payloadTrees()) {
			Expression root = new Expression("I", "| +");
			Expression mod = new Expression("I", "%").add(cover.clone()).add(literal("2, 4, 8, 10"));
			Expression sub = new Expression("I", "-").add(cover.clone()).add(mod);
			root.add(sub).add(payload.clone());
			sub_mod.add(root);
		}
		trees.put("sub_mod", sub_mod);
			
		// 6. one_step:
		//    cover [+|] literal
		//    cover & (literal << bits)
		List<Expression> one_step = new ArrayList<>();
		for (Expression cover : coverTrees()) {
			for (Expression payload : payloadTrees()) {
				Expression root = new Expression("I", "+ |");
				root.add(cover.clone()).add(payload.clone());
				one_step.add(root);
			}
		}
		trees.put("one_step", one_step);
			
		/*
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
		*/
		return trees;
	}
	
	static void allGroups() {
		List<Expression> list = new ArrayList<Expression>();
		/*
		and_mask
		Y = (X & m) + S
		clear k bits from the cover image, then embed payload*/
		Expression masked = new Expression("&");
		masked.add("Cover subtree").add("constant mask value");
		Expression and_mask = new Expression("ADD or AND");
		and_mask.add(masked).add("Payload subtree");
		and_mask.note = "and_mask";
		list.add(and_mask);
		/*
		div_mul
		Y = (X / m * m)+S
		clear k digits from the cover image, then embed payload*/
		Expression divved = new Expression("/");
		divved.add("Cover subtree").add("base");
		Expression mulled = new Expression("*").add(divved).add("base");
		Expression div_mul = new Expression("ADD or AND");
		div_mul.add(mulled).add("Payload subtree");
		div_mul.note = "div_mul";
		list.add(div_mul);
		/*
		div_shl
		Y = (X / m <<n)+S
		clear k LSBs from the cover image, then embed payload*/
		divved = new Expression("/");
		divved.add("Cover subtree").add("2^k");
		Expression shled = new Expression("<<").add(divved).add("k");
		Expression div_shl = new Expression("ADD or AND").add(shled).add("Payload subtree");
		div_shl.note = "div_shl";
		list.add(div_shl);
		/*
		shr_shl
		Y = (X >>n <<n)+S
		clear k LSBs from the cover image, then embed payload*/
		Expression shr = new Expression(">>");
		shr.add("Cover subtree").add("k LSB");
		Expression shl = new Expression("<<");
		shl.add(shr).add("k LSB");
		Expression shr_shl = new Expression("ADD or AND").add(shl).add("Payload subtree");
		shr_shl.note = "shr_shl";
		list.add(shr_shl);
		/*
		sub_mod
		Y = (X - X%m)+S
		clear k digits from the cover image, then embed payload*/
		Expression mod = new Expression("%");
		mod.add("Cover subtree").add("base");
		Expression sub = new Expression("-").add("Cover subtree").add(mod);
		Expression sub_mod = new Expression("ADD or AND").add(sub).add("Payload subtree");
		sub_mod.note = "sub_mod";
		list.add(sub_mod);
		/*
		one_step
		Y = X+S
		directly embed payload into cover image values*/
		Expression one_step = new Expression("ADD or AND");
		one_step.add("Cover subtree").add("Payload subtree");
		one_step.note = "one_step";
		list.add(one_step);
		/*
		parse_int
		Y = X ->toBinaryString() ->concat() ->parseInt()
		embed payload into k LSBs of cover image using string concatenation*/
		
		
		toDotGraph(list, "Overview of Reference Tree Group", 6, "reference_overview", Dirs.Desktop);
	}
	

	
	static void and_mask() {
		String[] types = new String[] {
			"LSB \\l0xfe -> 11111110", 
			"LS2B\\l0xfc -> 11111100", 
			"LS3B\\l0xf8 -> 11111000", 
			"LS4B\\l0xf0 -> 11110000"
		};
		String[] masks = new String[] {
			"0xfe, 0xfffffffe (-0x2)",
			"0xfc, 0xfffffffc (-0x4)",
			"0xf8, 0xfffffff8 (-0x8)",
			"0xf0, 0xfffffff0 (-0x10)"
		};
		
		List<Expression> list = new ArrayList<Expression>();
		
		
		for (int i=0; i<types.length; i++) {
			Expression cover = new Expression("Cover subtree");
			Expression mask_values = literal(masks[i]);
			Expression masked_cover = new Expression("&").add(cover).add(mask_values);
			Expression payload = new Expression("Payload subtree");
			Expression embed = new Expression("+ |").add(masked_cover).add(payload);
			embed.note = types[i];
			list.add(embed);
		}
		
		toDotGraph(list,  "Reference Tree Group: And Mask",  0, "and_mask", Dirs.Desktop);
	}
	
	static List<Expression> payloadTrees() {
		// Two types: a constant value, or a compound value involving variables
		List<Expression> list = new ArrayList<Expression>();
		
		Expression constantPL = literal("LSB: 0x0, 0x1\\lLS2B: 0x0, 0x1, 0x10, 0x11\\l...");
		constantPL.note = "Constant value";
		
		Expression masked = new Expression("I", "&");
		masked.add(literal("X")).add(literal("LSB: 0x1\\lLS2B: 0x3\\l..."));
		masked.note = "Variable - take the LSkB of some integer/byte variable";
		
		Expression parseInt = new Expression("I", "return");
		parseInt.add("Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I\\lLjava/lang/Integer;->parseInt(Ljava/lang/String;I)I").add("reference");
		parseInt.add(literal("0x2"));
		parseInt.note = "Variable - convert a bit string (e.g. \\\"0\\\" or \\\"10\\\") into an integer";
		
		list.add(constantPL);
		list.add(masked);
		list.add(parseInt);
		//toDotGraph(list, "Payload subtrees", "payload", Dirs.Desktop);
		return list;
	}
	
	// cover subtrees for whole pixel or single channels
	static List<Expression> coverTrees() {
		List<Expression> list = new ArrayList<>();
		// API form separate channels
		String[] channelNotes = new String[] {
				"Red","Green","Blue","Alpha",
		};
		for (int i=0; i<ARGB_SIGS.length; i++) {
			Expression channel = new Expression("I", "return");
			channel.add(ARGB_SIGS[i]).add(getPixel());
			channel.note = channelNotes[i]+" channel only";
			list.add(channel);
		}
		// API form whole pixel
		list.add(getPixel());
		// arithmetic form: direct mask on whole pixel
		Expression root = new Expression("I", "&");
		root.add(getPixel()).add(literal("0xff"));
		root.note = "Blue channel only";
		list.add(root);
		// arithmetic form: shift then mask
		root = new Expression("I", "&");
		Expression left = new Expression("I", ">>");
		left.add(getPixel()).add(literal("0x18, 0x10, 0x8, 0x0"));
		root.add(left).add(literal("0xff"));
		root.note = "One of the ARGB channels, depending on the right shifted bits (24 - alpha, 16 - red, 8 - green, 0 - blue)";
		list.add(root);
		//toDotGraph(list, "Different forms of the cover value subtree", "covers", Dirs.Desktop);
		return list;
	}
	
	static final String[] ARGB_SIGS = {
			"Landroid/graphics/Color;->red(I)I",
			"Landroid/graphics/Color;->green(I)I",
			"Landroid/graphics/Color;->blue(I)I",
			"Landroid/graphics/Color;->alpha(I)I"
	};
	
	static Expression getPixel() {
		Expression wholePixel = new Expression("I", "return");
		wholePixel.add("Landroid/graphics/Bitmap;->getPixel(II)I")
				  .add("reference").add(literal()).add(literal());
		wholePixel.note = "Whole pixel";
		return wholePixel;
	}
	
	static Expression literal() {
		return literal(null);
	}
	
	static Expression literal(String val) {
		Expression exp = new Expression("I", "literal");
		if (val != null)
			exp.add(val);
		return exp;
	}
	
	
	
	static void toDotGraph(List<Expression> expressions, String label, int graphsPerRow, String name, File dir) {
		Queue<Expression> queue = new LinkedList<>();
		queue.addAll(expressions);
		
		String text = "digraph G{\n";
		text += "\tcompound=true;\n";
		text += "\tordering=out;\n";
		text += "\tnewrank=true;\n";
		text += "\trankdir=TB;\n";
		text += "\tnode [shape=box];\n";
		text += "\tlabelloc=t;\n";
		if (label != null)
			text += String.format("\tlabel=\"%s\"\n", label);
		
		Map<Expression, Integer> allIndices = new HashMap<>();
		List<Integer> clusterDepths = new ArrayList<Integer>();
		int clusterIndex = 1;
		for (Expression exp : expressions) {
			Map<Expression, Integer> indices = new HashMap<>();
			Map<Integer, List<Integer>> edges = new HashMap<>();
			int depth = collectVertices(allIndices, indices, exp);
			clusterDepths.add(depth);
			collectEdges(indices, edges, exp);
			text += "\tsubgraph cluster"+clusterIndex+"{\n";
			if (exp.note!=null)
				text += String.format("\tlabel=\"%s\"\n", exp.note);
			//text += "\t\tstyle=invis;\n";
			for (Expression e : indices.keySet()) {
				text += "\t\t"+Graphviz.toDotGraphString(indices.get(e), e.root)+"\n";
			}
			//text += "\t\tCN"+clusterIndex+" [style=invis]\n";
			// make CN the top level node of cluster
			//text += "\t\tCN"+clusterIndex+" -> "+indices.get(exp)+"[style=invis];\n";
			for (int src : edges.keySet()) {
				List<Integer> dsts = edges.get(src);
				for (int i = 0; i < dsts.size(); i++) {
					if (dsts.size()>1)
						text += "\t\t"+src+" -> "+dsts.get(i)+"[label=\""+(i+1)+"\"];\n";
					else
						text += "\t\t"+src+" -> "+dsts.get(i)+";\n";
				}
			}
			text += "\t}\n";
			clusterIndex++;
		}
		if (graphsPerRow<1)
			graphsPerRow = 4;
		for (int i=1; i < clusterIndex; i++) {
			if (i+graphsPerRow<clusterIndex) {
				int from = allIndices.get(expressions.get(i-1));
				int to = allIndices.get(expressions.get(i+graphsPerRow-1));
				text += String.format("\t%d -> %d [style=invis; ltail=cluster%d, lhead=cluster%d];\n", 
						from, to, i, i+graphsPerRow);
			}
				
		}
		int globalRank = 1;
		for (int row = 0; row < clusterIndex/graphsPerRow; row++) {
			int from = row*graphsPerRow+1, to = Math.min(from+graphsPerRow-1, clusterIndex-1);
			// 1. all the CN nodes in this row should have same rank as rank(totalRanks)
			String rankString = "\t{rank=same; rank"+globalRank+"; ";
			for (int j=from; j<=to; j++) {
				int root = allIndices.get(expressions.get(j-1));
				rankString += root+"; ";
			}
			rankString +="}\n";
			text += rankString;
			// 2. find the depth of the clusters in this row, increment global rank for next row
			int rowDepth = 0;
			for (int j=from; j<=to ; j++)
				rowDepth = Math.max(rowDepth, clusterDepths.get(j-1));
			globalRank += rowDepth;
		}
		String[] rulers = new String[globalRank];
		for (int j=0; j<globalRank; j++) {
			rulers[j] = "rank"+(1+j);
			text += "\trank"+(j+1)+" [style=invis;]\n";
		}
		for (int j=1; j<globalRank-1; j++) {
			text += String.format("\trank%d -> rank%d [style=invis];\n", j, j+1);
		}
		text += "}";
		Graphviz.makeDotGraph(text, name, dir, true);
	}
	
	static void collectEdges(Map<Expression, Integer> indices, Map<Integer, List<Integer>> edges, Expression exp) {
		if (exp == null)
			return;
		int index = indices.get(exp);
		List<Integer> E = edges.computeIfAbsent(index, k->new ArrayList<>());
		for (Expression child : exp.children) {
			int childIndex = indices.get(child);
			E.add(childIndex);
			collectEdges(indices, edges, child);
		}
	}
	
	static int collectVertices(Map<Expression, Integer> allIndices, Map<Expression, Integer> indices, Expression exp) {
		if (exp == null)
			return 0;
		int maxChildDepth = 0;
		if (!allIndices.containsKey(exp)) {
			int index = allIndices.size();
			allIndices.put(exp, index);
			indices.put(exp, index);
			
			for (Expression child : exp.children)
				maxChildDepth = Math.max(maxChildDepth, collectVertices(allIndices, indices, child));
		}
		return maxChildDepth+1;
	}

}
