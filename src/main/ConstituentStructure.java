package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import utilities.Pair;

public class ConstituentStructure 
{
	private static int HASHCODE = 0;
	
	private String constituentType;
	
	private List<String> contentWords;
	
	private ConstituentStructure parent;
	
	private List<ConstituentStructure> leftToRightChildren;
	
	private int thisLevelStringRepWidth = 0;
	
	private String totalStringRep;
	
	private StringBuilder thisLevelStringRepBuilder = new StringBuilder();
	
	private String thisLevelStringRep;
	
	private int hashCode;
	
	private int numChildren = 0;
	
	// 0 if there is no parent; otherwise, the leftmost child has childNumber = 1
	private int childNumber = 0;
	
	/**
	 * To create a String representation, the following information is required:
	 * 
	 *   0.) A list of all of the children at a given level
	 *   
	 *   1.) The number and length of all of the children of a node, bottom up
	 */
	private void changeStringRepMessageToParent()
	{
		/*
		 * Inform all of the parent nodes that this node has changed
		 */
		if (this.parent != null)
		{
			this.parent.changeStringRepMessageToParent();
		}
		
		/*
		 * Contains all of the current string rep lines
		 */
		ArrayList<StringBuilder> stringRepLines = new ArrayList<StringBuilder>();
		
		Stack<Pair<ConstituentStructure, Integer>> frontier = 
			new Stack<Pair<ConstituentStructure, Integer>>();
		
		frontier.add(new Pair<ConstituentStructure, Integer>(this, 0));
		
		/*
		 *  Add all of the children at each level in the tree to a distinct level in the stack:
		 *  
		 *  (s )
		 *  ||-------------------
		 *  |                   |
		 *  (NP )               (VP)                  
		 *  ||--------|         ||-------| 
		 *  |         |         |        |
		 *  (DET The) (NNS Man) (VBZ is) (JJ tall.) 
		 *  
		 *  The correct algorithm is to explore left-first infix and build up the string that way:
		 *  
		 *    For each child of the current node
		 *      Obtain its children
		 *      Based on the number of children, create the link anchors
		 *        |||
		 *        ||
		 *        |
		 *      Explore the children, starting with the left-most
		 *      If there are no more children, then report your length to your parent
		 *  
		 */
		
		// For keeping track of the number of backtracks to each node
		Map<ConstituentStructure, Integer> nodeToNumBacktracks =
			new HashMap<ConstituentStructure, Integer>();

		// This is the current number of backtracks made to the current node
		int curNumBacktracks;
		
		// This is the table that tallies how long the string rep is for all of the children 
		// of a node
		Map<ConstituentStructure, Integer> nodeToInheritedLeftOffset =
			new HashMap<ConstituentStructure, Integer>();
		
		// This is the offset of the parent of the current node
		Integer curParentNodeOffset;
		
		// Also need to keep track of the current line length as it compares to the
		// parent offset
		Map<ConstituentStructure, Integer> nodeToChildOffset =
			new HashMap<ConstituentStructure, Integer>();
		
		// This is the total child offset underneath the parent of the current node
		Integer curParentChildOffset;
		
		// This is the total offset of the current node that is inherited from the parent
		int curTotalInheritedOffset; 
		
		// This is the current node being explored along with the string rep depth of that node
		Pair<ConstituentStructure, Integer> curNodeAndDepth;
		
		// This is the current node being explored
		ConstituentStructure curNode;
		
		// This is the parent of the current node
		ConstituentStructure curParent;
		
		// This is the child number of the current node
		int curChildNumber;
		
		// These are the child nodes of the current node in L to R order
		List<ConstituentStructure> curLeftToRightChildren;
		
		// This is the number of child nodes of the current node
		int curNumChildren;
		
		// This is the (exact) string representation for the current node
		String curNodeStringRep;
		
		// This is the length of the string representation for the current node
		int curNodeStringRepLength;
		
		// This is the string line depth before the child node is added
		int curStringLineDepth;
		
		// This is the new string line depth of the string rep after the child node is added 
		int newStringLineDepth;
		
		// This is the (exact) length of the current node that is initially passed to its parent
		int curChildNodeLengthToParent;
		
		// This is the current total string rep line being modified
		StringBuilder curStringRepLine;
		
		// This is a counter for accessing lines of the total string rep
		int stringRepLinesIndex;
		
		// This is the total offset of the current node's children, which is to be compared to the (exact) length of the current node
		int curNodeChildOffset;
		
		// This is the total offset of the current node to be passed to its parent upon backtracking
		int curOffsetLengthToBePassedToParent;
		
		// While the frontier is not empty, there are still nodes to explore
		while (!frontier.isEmpty())
		{
			// The current node to explore is at the front of the frontier
			 curNodeAndDepth = frontier.pop();
			 curNode = curNodeAndDepth.getKey();
			 curStringLineDepth = curNodeAndDepth.getValue();
			
			 curChildNumber = curNode.childNumber;
			 
			// If this node has not yet been explored, 
			if (nodeToInheritedLeftOffset.get(curNode) == null)
			{
				// Obtain the string rep for just this level
				curNodeStringRep = curNode.thisLevelStringRepBuilder.toString();
				
				curNodeStringRepLength = curNodeStringRep.length();
				
				// Set the number of backtracks to 0
				curNumBacktracks = 0;
				
				nodeToNumBacktracks.put(curNode, curNumBacktracks);
				
				// Determine if this node has a parent
				curParent = curNode.getParent(); 
				
				// If the current node is equal to the node being updated, then act as though there is no
				if (curParent != null)
				{
					// Obtain the offset from the parent
					// The formula for the offset is:  parent.inherited_left_offset + parent.nodeToChildOffset
					curParentChildOffset = nodeToChildOffset.get(curParent);
					
					if (curParentChildOffset == null)
					{
						curParentChildOffset = 0;
					}
					
					curParentNodeOffset = nodeToInheritedLeftOffset.get(curParent);
					
					if (curParentNodeOffset == null)
					{
						curParentNodeOffset = 0;
					}
					
					curTotalInheritedOffset = 
						curParentNodeOffset +
						curParentChildOffset;
					
					// Report the contribution of this node to the total parent child offset
					// If this node has no children, then report the offset to the parent node
					if (curNumBacktracks == curNode.numChildren)
					{
						curChildNodeLengthToParent = curNodeStringRepLength;
						
						if (curChildNumber > 1)
						{
							curChildNodeLengthToParent++;
						}
						
						nodeToChildOffset.put(curParent, curChildNodeLengthToParent+curParentChildOffset);
					}
				}
				else
				{
					curTotalInheritedOffset = 0;
				}
				
				// Get the current child number
				curChildNumber = curNode.childNumber;
				
				nodeToInheritedLeftOffset.put(curNode, curTotalInheritedOffset);
				
				// Ensure that the current string rep level is at the the proper offset
				for (int i = stringRepLines.size() ; i <= curStringLineDepth ; i++)
				{
					// Then create it and add it to the string rep line stack
					curStringRepLine = new StringBuilder();
					
					stringRepLines.add(curStringRepLine);
				}
				
				curStringRepLine = stringRepLines.get(curStringLineDepth);
				
				// Pad the current string rep line to the appropriate amount
				for (int i = curStringRepLine.length() ; i < curTotalInheritedOffset ; i++)
				{
					curStringRepLine.append(" ");
				}
				
				// Append the current node string rep to the current string rep line
				curStringRepLine.append(curNode.thisLevelStringRepBuilder.toString());
				
				// This is where the connector to this node is created by appending the connector to 
				// the appropriate line
				if ((curParent != null) && (curChildNumber > 1))
				{
					int curFirstLineConnectorStringDepth = curStringLineDepth - curChildNumber;
					
					StringBuilder curStringRepLineForConnector = stringRepLines.get(curFirstLineConnectorStringDepth);
					
					int curLengthOfStringRepLineForConnector = curStringRepLineForConnector.length();
					
					// Create the horizontal connector components at the appropriate attachment line
					for (int i = curLengthOfStringRepLineForConnector ; i < curTotalInheritedOffset ; i++)
					{
						curStringRepLineForConnector.append("-");
					}
					
					// Create the padding and the rest of the vertical connectors
					for (int i = curFirstLineConnectorStringDepth; i < curStringLineDepth ; i++)
					{
						curStringRepLineForConnector = stringRepLines.get(i);
						
						curLengthOfStringRepLineForConnector = curStringRepLineForConnector.length();
						
						// Add the space pads
						for (int j = curLengthOfStringRepLineForConnector ; j < curTotalInheritedOffset ; j++)
						{
							curStringRepLineForConnector.append(" ");
						}
						
						// Add the vertical connector
						curStringRepLineForConnector.append("|");
					}
				}
				
				// Obtain the children from the current node
				curLeftToRightChildren = curNode.getConstituentChildren();
				
				// If there are child nodes
				if ((curLeftToRightChildren != null) && !curLeftToRightChildren.isEmpty())
				{
					curNumChildren = curLeftToRightChildren.size();
					
					newStringLineDepth = curStringLineDepth+curNumChildren+1;
					
					stringRepLinesIndex = curStringLineDepth+1;
					
					// Add the downward "connectors"
					for (int i = curNumChildren ; i >= 1 ; i--)
					{
						// Add a new level to the total String rep (if necessary)
						if (stringRepLines.size() <= stringRepLinesIndex)
						{
							curStringRepLine = new StringBuilder();
							
							stringRepLines.add(curStringRepLine);
						}
						
						for (int k = 0; k < i ; k++)
						{
							curStringRepLine.append("|");
						}
						
						stringRepLinesIndex++;
					}
					
					// Then add the children to the frontier
					for (int i = curNumChildren-1 ; i >= 0 ; i--)
					{
						frontier.push(
							new Pair<ConstituentStructure, Integer>(curNode, curStringLineDepth));
						frontier.push(
							new Pair<ConstituentStructure, Integer>(curLeftToRightChildren.get(i), newStringLineDepth));
					}
				}
			}
			// Otherwise, the node has previously been explored and thus this is a backtrack
			else
			{
				// Increment the recorded number of backtracks for the current node
				curNumBacktracks = nodeToNumBacktracks.get(curNode) + 1;
				
				nodeToNumBacktracks.put(curNode, curNumBacktracks);
				
				// Report the total child offset (or this node length if larger) to the parent node
				if (curNumBacktracks == curNode.numChildren)
				{
					// Add the current line length to the parent offset 
					curParent = curNode.getParent();
						
					if (curParent != null)
					{
						curNodeStringRepLength = curNode.thisLevelStringRepWidth;
						
						curNodeChildOffset = nodeToChildOffset.get(curNode);
						
						curOffsetLengthToBePassedToParent =
							Math.max(curNodeStringRepLength, curNodeChildOffset);
						
						curParentNodeOffset = nodeToChildOffset.get(curParent);
						
						if (curParentNodeOffset == null)
						{
							curParentNodeOffset = 0;
						}
						
						// Examine the offset of this node 
						nodeToChildOffset.put(
							curParent, curParentNodeOffset+curOffsetLengthToBePassedToParent);
					}
				}
			}
		}
		
		// Convert the string rep lines to a String
		this.totalStringRep = "";
		
		int curLineNumber = 1;
		
		for (StringBuilder curStringRepLineBuilder : stringRepLines)
		{
			this.totalStringRep += curStringRepLineBuilder.toString();
			
			// Add a line break if this is not the last toString line
			if (curLineNumber < stringRepLines.size())
			{
				this.totalStringRep += "\n";
			}
			
			curLineNumber++;
		}
	}
	
	private void setChildNumber(int numChild)
	{
		this.childNumber = numChild;
	}
	
	public ConstituentStructure(String constituentType)
	{
		this.constituentType = constituentType;
		
		this.contentWords = new LinkedList<String>();
		this.leftToRightChildren = new LinkedList<ConstituentStructure>();
		
		// Set the hashcode
		this.hashCode = ConstituentStructure.HASHCODE;
		ConstituentStructure.HASHCODE++;
		
		// Start the String representation
		this.thisLevelStringRepBuilder.append("(" + this.constituentType + ")");
		
		this.thisLevelStringRep = this.thisLevelStringRepBuilder.toString();
		
		// For now, the total string rep is just the current level string rep
		this.totalStringRep = this.thisLevelStringRep;
	}
	
	public void addContentWord(String word)
	{
		this.contentWords.add(word);
		
		this.thisLevelStringRepBuilder.insert(this.thisLevelStringRepBuilder.length()-1, word);
		
		this.thisLevelStringRep = this.thisLevelStringRepBuilder.toString();
		
		this.changeStringRepMessageToParent();
	}
	
	public void addConstituentStructureChild(
		ConstituentStructure newChild)
	{
		newChild.parent = this;
		
		this.leftToRightChildren.add(newChild);
		
		this.numChildren++;
		
		newChild.setChildNumber(this.numChildren);
		
		this.changeStringRepMessageToParent();
	}
	
	public List<String> getContentWords()
	{
		return this.contentWords;
	}
	
	public int numContentWords()
	{
		return this.contentWords.size();
	}
	
	public ConstituentStructure getParent()
	{
		return this.parent;
	}
	
	public List<ConstituentStructure> getConstituentChildren()
	{
		return this.leftToRightChildren;
	}
	
	public int numConstituentChildren()
	{
		return this.leftToRightChildren.size();
	}
	
	/*
	 * Object Overrides
	 */
	
	@Override
	public int hashCode() 
	{
		return this.hashCode;
	}
	
	@Override
	public boolean equals(Object obj) 
	{
		if (obj instanceof ConstituentStructure)
		{
			ConstituentStructure cast = (ConstituentStructure)obj;
	
			if (!this.constituentType.equals(((ConstituentStructure) obj).constituentType))
			{
				return false;
			}
			if (!this.parent.equals(cast.parent))
			{
				return false;
			}
			if (this.numContentWords() == cast.numContentWords())
			{
				for (int i = 0 ; i < this.numContentWords() ; i++)
				{
					if (!this.contentWords.get(i).equals(cast.contentWords.get(i)))
					{
						return false;
					}
				}
			}
			else
			{
				return false;
			}
			if (this.numConstituentChildren() == cast.numConstituentChildren())
			{
				for (int i = 0 ; i < this.numContentWords() ; i++)
				{
					if (!this.leftToRightChildren.get(i).equals(cast.leftToRightChildren.get(i)))
					{
						return false;
					}
				}
			}
			else
			{
				return false;
			}
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public String toString() 
	{
		return this.totalStringRep;
	}
	
	/*
	 * Testing
	 */
	public static void main(String[] args)
	{
		ConstituentStructure root = new ConstituentStructure("S");
		ConstituentStructure rC1 = new ConstituentStructure("NP");
		ConstituentStructure rC2 = new ConstituentStructure("VP");
		
		root.addConstituentStructureChild(rC1);
		root.addConstituentStructureChild(rC2);
		
		root.changeStringRepMessageToParent();
		
		System.out.println(root);
		
		ConstituentStructure rC1C1 = new ConstituentStructure("DET");
		ConstituentStructure rC1C2 = new ConstituentStructure("NP");
		
		rC1.addConstituentStructureChild(rC1C1);
		rC1.addConstituentStructureChild(rC1C2);
		
		System.out.println(root);
	}
}