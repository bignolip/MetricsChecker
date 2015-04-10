package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Count constituents as constructions in the form:
 * 
 *    ([A-Z]+ text )
 * 
 * Non-terminal constituents are those that contain more than one word or that contain another constituent
 * Terminal constituents are everything else
 * 
 * Note:  This version DOES NOT currently support the parsing of punctuation outside of constituents
 *        It is possible to distinguish between punctuation that is 'part' of a word if it occurs after
 *        text (as opposed to a right parenthesis)
 * 
 * @author hlil_administrator
 *
 */
public class WSJConstituentCounter 
{	
	/*
	 * Statistical frequency tables data type to track basic counts in WSJ tagged data 
	 */
	public static class WSJConstituentCounterStatistics
	{
		public Map<String, Integer> constituentTypeToCount =
			new HashMap<String, Integer>();
		
		// This is the total number of constituents 
		public Integer numConstituents = 0;
		
		// This is the total number of words in the corpus
		public Integer numWords = 0;
		
		public Map<String, Integer> wordToCount = new HashMap<String, Integer>();
		
		public Map<String, Map<String, Integer>> wordToImmediateParentConstituentTypeToCount =
			new HashMap<String, Map<String, Integer>>();
	}
	
	public static class WSJConstituentCounterStructures
	{
		public List<ConstituentStructure> parsedConstituentStructures = new LinkedList<ConstituentStructure>();
		
		public List<ConstituentSkeleton> parsedConstituentSkeletons = new LinkedList<ConstituentSkeleton>();
	}
	
	public static WSJConstituentCounterStructures parseContituentsFromString(
			String constituentsString,
			WSJConstituentCounterStatistics statistics)
	{
		WSJConstituentCounterStructures returnStructure = new WSJConstituentCounterStructures();
		
		char curChar;
		StringBuilder curConstituentTypeBuilder = new StringBuilder();
		String curConstituentTypeString;
		int curConstituentTypeStringLength;
		boolean leftBracketReached = false;
		boolean endOfContituentTypeReached = false;
		
		boolean startOfWordReached = false;
		boolean endOfWordReached = false;
		
		boolean rightBracketReached = false;
				
		StringBuilder curWordBuilder = new StringBuilder();
		String curWordString;
		int curWordStringLength = 0;
		
		Integer curWordFrequencyCounter;
		Map<String, Integer> curWordToParentConstituentMap;
		Integer curWordToParentConstituentFrequencyCounter;
		
		// Create the new constituent roots
		ConstituentStructure prevConstituentStructureNode = new ConstituentStructure("ROOT");
		ConstituentSkeleton prevConstituentSkeletonNode = new ConstituentSkeleton("ROOT");
		
		ConstituentStructure curConstituentStructureNode = null;
		ConstituentSkeleton curConstituentSkeletonNode = null;
		
		for (int i = 0; i < constituentsString.length() ; i++)
		{
			curChar = constituentsString.charAt(i);
			
			// Determine if this is the start of a new constituent
			if (curChar == '(')
			{
				leftBracketReached = true;
				endOfContituentTypeReached = false;
			}
			// If a new constituent is reached and the constituent type is being parsed
			else if (leftBracketReached && !endOfContituentTypeReached)
			{
				// Possible end of constituent type
				if (curChar == ' ')
				{
					endOfContituentTypeReached = true;
					
					curConstituentTypeString = curConstituentTypeBuilder.toString().trim();
					curConstituentTypeStringLength = curConstituentTypeString.length();
					
					// If this was just a left-bracket, then do nothing
					if (curConstituentTypeString.equals(""))
					{
						leftBracketReached = false;
						endOfContituentTypeReached = false;
						
						continue;
					}
					// Otherwise, log the constituent type
					else
					{
						endOfContituentTypeReached = true;
						
						// Create the new constituent structure
						curConstituentStructureNode = new ConstituentStructure(curConstituentTypeString);
						curConstituentSkeletonNode = new ConstituentSkeleton(curConstituentTypeString);
						
						// Attach to the previous constituent
						prevConstituentStructureNode.addConstituentStructureChild(curConstituentStructureNode);
						prevConstituentSkeletonNode.addConstituentStructureChild(curConstituentSkeletonNode);
						
						// Increment the number of constituents
						statistics.numConstituents++;
						
						// Increment the number of this type of constituent
						Integer curConstituentTypeToCount = 
							statistics.constituentTypeToCount.get(curConstituentTypeString);
						
						if (curConstituentTypeToCount == null)
						{
							curConstituentTypeToCount = 0;
						}
						
						curConstituentTypeToCount++;
						
						statistics.constituentTypeToCount.put(curConstituentTypeString, curConstituentTypeToCount);
						
						// Clear the string builder for the next constituent type
						curConstituentTypeBuilder.delete(0, curConstituentTypeStringLength);
					}
				}
				else
				{
					curConstituentTypeBuilder.append(curChar);
				}
			}
			// If the end of a constituent type is reached, then determine how to continue to parse the structure
			else if (endOfContituentTypeReached && !rightBracketReached)
			{
				// Determine if the start of a word has been reached
				if (startOfWordReached)
				{
					// If this is a space, then the end of the word has been reached
					if (curChar == ' ')
					{
						// Read the string off of the string builder
						curWordString = curWordBuilder.toString();
						curWordStringLength = curWordString.length();
						
						// Add the content word to the current structure node
						curConstituentStructureNode.addContentWord(curWordString);
						
						// Update the state of the parser
						endOfWordReached = true;
						startOfWordReached = false;
						
						// Increment the word statistics counter
						
						// Clear the string builder for the next word
						curWordBuilder.delete(0, curWordStringLength);
					}
				}
				// Detect if the end of a constituent has been reached (this assumes that a ')' will never
				// appear as part of a word (although this assumption is dubious, it is probably justified
				// for small amounts of standard texts)
				else if (curChar == ')')
				{
					endOfWordReached = true;
					
					// Note:  after a right bracket is reached, no more words can be added to a constituent structure
					rightBracketReached = true;
				}
				// Detect if
				else if (curChar != ' ')
				{
					// Append this character to the current word string builder
					curWordBuilder.append(curChar);
					
					// Update the state to indicate that a new word has been started
					startOfWordReached = true;
				}
			}
			
		}
		
		return returnStructure;
	}
	
	public static WSJConstituentCounterStructures parseConstituentStructureFiles(
		File baseDirectory, String filePatternRegex, WSJConstituentCounterStatistics statistics)
	{
		// Create return structure
		WSJConstituentCounterStructures allConstituentStructures = new WSJConstituentCounterStructures();
		
		// Obtain a list of all of the files in the base repository directory 
		File[] filesInBaseDirectory = baseDirectory.listFiles();
		
		String curFileName;
		
		BufferedReader reader;
		
		String curFileLine;
		
		StringBuilder curTotalFileStringBuilder;
		
		// Examine all of the files
		try
		{
			for (File curFile : filesInBaseDirectory)
			{
				curFileName = curFile.getName();
				
				// If the current file contains constituent data
				if (curFileName.matches(filePatternRegex))
				{
					// Compile the lines of the file into a single string and pass it to the parsing method
					reader = new BufferedReader(new FileReader(curFile));
					
					curTotalFileStringBuilder = new StringBuilder();
					
					curFileLine = reader.readLine();
					
					while (curFileLine != null)
					{
						curTotalFileStringBuilder.append(curFileLine);
						
						curFileLine = reader.readLine();
					}
					
					WSJConstituentCounter.parseContituentsFromString(
						curTotalFileStringBuilder.toString(), 
						statistics);
				}
			}
		}
		catch (Exception e)
		{
			
		}
		
		return allConstituentStructures;
	}
	
	public static final String WSJ_CORPUS_FILE_REGEX_STRING = "wsj_[0-9][0-9][0-9][0-9].prd";
	
	public static void main(String[] args)
	{
		
	}
}