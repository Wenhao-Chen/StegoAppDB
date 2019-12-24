package app_analysis.asiaccs;

import java.util.ArrayList;
import java.util.List;

import apex.symbolic.Expression;

public class MatchingExpressionTrees {

	// This function checks whether the given expression tree:
	// contains a subtree that represents the return value of Bitmap.getPixel
	static boolean HasGetPixel(Expression exp)
	{
		if (exp == null)
			return false;

		if (exp.root.contentEquals("return") &&
			exp.children.get(0).root.contentEquals("Landroid/graphics/Bitmap;->getPixel(II)I"))
			return true;

		for (Expression child : exp.children)
			if (HasGetPixel(child))
				return true;

		return false;
	}

	
	// This function checks whether the given expression:
	// contains subtree that represents the return value of Bitmap.getPixel()
	// and the given expression x and y are identical to the x,y subtrees in the getPixel() parameters
	static boolean HasGetPixel(Expression exp, Expression x, Expression y)
	{
		if (exp == null)
			return false;

		if (exp.root.contentEquals("return"))
			return exp.children.size() == 4
					&& exp.children.get(0).root.contentEquals("Landroid/graphics/Bitmap;->getPixel(II)I")
					&& exp.children.get(2).equals(x) && exp.children.get(3).equals(y);

		for (Expression child : exp.children)
			if (HasGetPixel(child, x, y))
				return true;

		return false;
	}

	// This function returns the value of the node which is the immediate parent
	// of the "return-getPixel()" subtree.
	// Returns empty string if no such subtree is found
	static List<String> getOperatorForGetPixelValue(Expression exp, Expression x, Expression y)
	{
		List<String> res = new ArrayList<>();
		if (exp == null)
			return res;

		// if there is a direct child that is the GetPixel tree, add self
		for (Expression child : exp.children)
			if (child.root.contentEquals("return") && child.children.size() == 4
					&& child.children.get(0).root.contentEquals("Landroid/graphics/Bitmap;->getPixel(II)I")
					&& child.children.get(2).equals(x) && child.children.get(3).equals(y))
				res.add(exp.root);

		for (Expression child : exp.children)
		{
			List<String> childRes = getOperatorForGetPixelValue(child, x, y);
			res.addAll(childRes);
		}
		return res;
	}
	
	// This function recursively checks the subtrees of given expression 'exp'
	// and replaces the subtrees that have the "return-getPixel()-obj-x-y" pattern
	// Only the subtrees that have the identical x,y parameters with the given x,y are replaced.
	static void trimGetPixel(Expression exp, Expression x, Expression y)
	{
		if (exp == null)
			return;
		
		for (int i=0; i<exp.children.size(); i++)
		{
			Expression child = exp.children.get(i);
			if (child.root.contentEquals("return") && child.children.size() == 4
					&& child.children.get(0).root.contentEquals("Landroid/graphics/Bitmap;->getPixel(II)I")
					&& child.children.get(2).equals(x) && child.children.get(3).equals(y))
			{
				exp.children.set(i, new Expression("CoverPixel"));
			}
		}
		
		for (Expression child : exp.children)
			trimGetPixel(child, x, y);
	}
}
